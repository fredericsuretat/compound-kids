package com.suretat.compoundkids.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Un vrai compte de l'enfant, saisi à la main par un parent (rien ne quitte le téléphone). */
data class Account(val name: String, val amount: Double, val annualRate: Double)

data class Savings(val accounts: List<Account>, val updatedAt: Long)

object AccountStore {
    private const val PREFS = "mes_vrais_sous"

    fun load(context: Context): Savings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = JSONArray(prefs.getString("accounts", "[]"))
        val accounts = (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            Account(o.getString("name"), o.getDouble("amount"), o.getDouble("rate"))
        }
        return Savings(accounts, prefs.getLong("updatedAt", 0L))
    }

    fun save(context: Context, accounts: List<Account>) {
        val json = JSONArray()
        accounts.forEach { json.put(JSONObject().put("name", it.name).put("amount", it.amount).put("rate", it.annualRate)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("accounts", json.toString())
            .putLong("updatedAt", System.currentTimeMillis())
            .apply()
    }
}

/** Lit « 2 177,16 » ou « 2177.16 » ; null si ce n'est pas un nombre. */
fun parseAmount(text: String): Double? =
    text.replace(',', '.').filter { it.isDigit() || it == '.' }.toDoubleOrNull()

fun formatCents(amount: Double): String = String.format(Locale.FRANCE, "%,.2f €", amount)

/** Valeur d'un compte dans [years] ans, sans nouveau versement. */
fun projectAccount(account: Account, years: Int): Double =
    simulate(account.amount, 0.0, years, account.annualRate).last().nominal
