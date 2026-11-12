package com.suretat.compoundkids.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Un vrai compte de l'enfant, saisi à la main par un parent.
 * [amount] = valeur aujourd'hui, [invested] = argent réellement versé dessus.
 */
data class Account(val name: String, val amount: Double, val invested: Double, val annualRate: Double)

/** Lien familial : [code] parent (peut modifier) ou enfant (lecture seule). */
data class FamilyLink(val code: String, val isParent: Boolean, val childCode: String? = null)

data class Savings(val accounts: List<Account>, val updatedAt: Long, val link: FamilyLink?)

fun accountsToJson(accounts: List<Account>): JSONArray = JSONArray().apply {
    accounts.forEach {
        put(JSONObject().put("name", it.name).put("amount", it.amount).put("invested", it.invested).put("rate", it.annualRate))
    }
}

fun accountsFromJson(json: JSONArray): List<Account> = (0 until json.length()).map { i ->
    val o = json.getJSONObject(i)
    // Les comptes saisis en 1.5.0 n'avaient pas « invested » : on prend la valeur actuelle.
    Account(o.getString("name"), o.getDouble("amount"), o.optDouble("invested", o.getDouble("amount")), o.getDouble("rate"))
}

object AccountStore {
    private const val PREFS = "mes_vrais_sous"

    fun load(context: Context): Savings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val link = p.getString("linkCode", null)?.let { FamilyLink(it, p.getBoolean("linkParent", false), p.getString("childCode", null)) }
        return Savings(accountsFromJson(JSONArray(p.getString("accounts", "[]"))), p.getLong("updatedAt", 0L), link)
    }

    fun save(context: Context, accounts: List<Account>, updatedAt: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("accounts", accountsToJson(accounts).toString())
            .putLong("updatedAt", updatedAt)
            .apply()
    }

    fun saveLink(context: Context, link: FamilyLink?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (link == null) remove("linkCode").remove("linkParent").remove("childCode")
            else putString("linkCode", link.code).putBoolean("linkParent", link.isParent).putString("childCode", link.childCode)
        }.apply()
    }
}

/** Synchronisation avec le serveur (frederic.suretat.com, route /api/compound-kids). Toutes les fonctions lèvent une exception en cas d'échec. */
object FamilyClient {
    private const val API = "https://frederic.suretat.com/api/compound-kids"
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val JSON_TYPE = "application/json".toMediaType()

    private fun call(request: Request): JSONObject = client.newCall(request).execute().use { r ->
        val body = r.body?.string().orEmpty()
        if (!r.isSuccessful) error(runCatching { JSONObject(body).getString("message") }.getOrDefault("Erreur ${r.code}"))
        JSONObject(body)
    }

    private fun body(accounts: List<Account>) = JSONObject().put("accounts", accountsToJson(accounts)).toString().toRequestBody(JSON_TYPE)

    /** Crée le lien depuis le téléphone parent ; renvoie (code parent, code enfant). */
    suspend fun create(accounts: List<Account>): Pair<String, String> = withContext(Dispatchers.IO) {
        val o = call(Request.Builder().url("$API/families").post(body(accounts)).build())
        o.getString("parentCode") to o.getString("childCode")
    }

    /** Lit les comptes ; renvoie (est parent, comptes, date de mise à jour). */
    suspend fun fetch(code: String): Triple<Boolean, List<Account>, Long> = withContext(Dispatchers.IO) {
        val o = call(Request.Builder().url("$API/family").header("X-Family-Code", code).build())
        Triple(o.getString("role") == "parent", accountsFromJson(o.getJSONArray("accounts")), o.getLong("updatedAt"))
    }

    suspend fun push(code: String, accounts: List<Account>): Long = withContext(Dispatchers.IO) {
        call(Request.Builder().url("$API/family").header("X-Family-Code", code).put(body(accounts)).build()).getLong("updatedAt")
    }
}

/** Lit « 2 177,16 » ou « 2177.16 » ; null si ce n'est pas un nombre. */
fun parseAmount(text: String): Double? =
    text.replace(',', '.').filter { it.isDigit() || it == '.' }.toDoubleOrNull()

fun formatCents(amount: Double): String = String.format(Locale.FRANCE, "%,.2f €", amount)

/** Code affiché par groupes de 4 : « ABCD-EFGH-JKLM ». */
fun formatCode(code: String): String = code.chunked(4).joinToString("-")

/** Valeur d'un compte dans [years] ans, avec un éventuel ajout ponctuel aujourd'hui et un versement mensuel. */
fun projectAccount(account: Account, years: Int, monthly: Double = 0.0, lump: Double = 0.0): Double =
    simulate(account.amount + lump, monthly, years, account.annualRate).last().nominal
