package com.suretat.compoundkids.support

// Module de support commun aux apps (copié à l'identique dans chaque app, seul le package change).
// Tickets = issues GitHub créées par le backend site-ateris (/api/support/<app>/…), qui détient le
// token GitHub : rien de sensible n'est embarqué dans l'APK. Chaque ticket a un secret connu de ce
// seul téléphone, stocké localement : il permet d'en suivre l'état, de lire les réponses et de répondre.

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class SupportConfig(
    /** Identifiant côté serveur (clé de SUPPORT_APPS dans site-ateris/backend/src/routes/support.js). */
    val appKey: String,
    val appLabel: String,
    val versionName: String,
    val baseUrl: String = "https://frederic.suretat.com",
)

data class LocalTicket(val number: Int, val secret: String, val title: String, val createdAt: Long)

data class TicketSummary(
    val number: Int, val title: String, val state: String, val updatedAt: String,
    val lastFrom: String, val messageCount: Int,
)

data class TicketMessage(val from: String, val body: String, val photos: List<String>, val createdAt: String)

data class TicketDetail(val number: Int, val title: String, val state: String, val messages: List<TicketMessage>)

class SupportClient(private val context: Context, val config: SupportConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("support_tickets", Context.MODE_PRIVATE)
    private val api get() = "${config.baseUrl.trimEnd('/')}/api/support/${config.appKey}"

    val deviceInfo: String
        get() = "${config.appLabel} v${config.versionName} — Android ${android.os.Build.VERSION.RELEASE} " +
            "(SDK ${android.os.Build.VERSION.SDK_INT}) — ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

    // ── Tickets connus de ce téléphone ──────────────────────────────────────────
    fun localTickets(): List<LocalTicket> {
        val arr = try { JSONArray(prefs.getString("tickets", "[]")) } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            LocalTicket(o.getInt("number"), o.getString("secret"), o.optString("title"), o.optLong("createdAt"))
        }.sortedByDescending { it.createdAt }
    }

    private fun addLocalTicket(t: LocalTicket) {
        val arr = JSONArray()
        (localTickets() + t).forEach {
            arr.put(JSONObject().put("number", it.number).put("secret", it.secret).put("title", it.title).put("createdAt", it.createdAt))
        }
        prefs.edit().putString("tickets", arr.toString()).apply()
    }

    /** Email de contact (obligatoire pour une nouvelle demande), mémorisé pour les suivantes. */
    var email: String
        get() = prefs.getString("email", "") ?: ""
        set(v) { prefs.edit().putString("email", v.trim()).apply() }

    fun isValidEmail(v: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(v.trim()).matches()

    /** Nombre de messages déjà vus par ticket, pour signaler les nouvelles réponses. */
    fun seenCount(number: Int): Int = prefs.getInt("seen_$number", 0)
    fun markSeen(number: Int, count: Int) = prefs.edit().putInt("seen_$number", count).apply()

    // ── API ─────────────────────────────────────────────────────────────────────
    /** Crée un ticket. Retourne l'éventuel avertissement du serveur (photos non jointes). */
    suspend fun create(message: String, photos: List<Uri>, contactEmail: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            email = contactEmail
            val body = multipart(message, photos)
                .addFormDataPart("deviceInfo", deviceInfo)
                .addFormDataPart("email", contactEmail.trim())
                .build()
            client.newCall(Request.Builder().url("$api/tickets").post(body).build()).execute().use { r ->
                val o = JSONObject(r.body?.string() ?: "{}")
                if (!r.isSuccessful) return@withContext Result.failure(Exception(o.optString("message", "HTTP ${r.code}")))
                val title = message.lineSequence().firstOrNull()?.take(70)?.ifBlank { null } ?: "Photo"
                addLocalTicket(LocalTicket(o.getInt("number"), o.getString("secret"), title, System.currentTimeMillis()))
                Result.success(o.optString("warning").ifBlank { null }.takeUnless { o.isNull("warning") })
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun statuses(): Result<List<TicketSummary>> = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray()
            localTickets().forEach { arr.put(JSONObject().put("number", it.number).put("secret", it.secret)) }
            val payload = JSONObject().put("tickets", arr).toString().toRequestBody("application/json".toMediaType())
            client.newCall(Request.Builder().url("$api/tickets/status").post(payload).build()).execute().use { r ->
                if (!r.isSuccessful) return@withContext Result.failure(Exception("HTTP ${r.code}"))
                val res = JSONArray(r.body?.string() ?: "[]")
                Result.success((0 until res.length()).map { i ->
                    val o = res.getJSONObject(i)
                    TicketSummary(o.getInt("number"), o.optString("title"), o.optString("state"),
                        o.optString("updatedAt"), o.optString("lastFrom"), o.optInt("messageCount"))
                })
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun detail(t: LocalTicket): Result<TicketDetail> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$api/tickets/${t.number}").header("X-Ticket-Secret", t.secret).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext Result.failure(Exception("HTTP ${r.code}"))
                val o = JSONObject(r.body?.string() ?: "{}")
                val msgs = o.optJSONArray("messages") ?: JSONArray()
                Result.success(TicketDetail(
                    o.getInt("number"), o.optString("title"), o.optString("state"),
                    (0 until msgs.length()).map { i ->
                        val m = msgs.getJSONObject(i)
                        val ph = m.optJSONArray("photos") ?: JSONArray()
                        TicketMessage(m.optString("from"), m.optString("body"),
                            (0 until ph.length()).map { ph.getString(it) }, m.optString("createdAt"))
                    },
                ))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reply(t: LocalTicket, message: String, photos: List<Uri>): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$api/tickets/${t.number}/replies")
                .header("X-Ticket-Secret", t.secret).post(multipart(message, photos).build()).build()
            client.newCall(req).execute().use { r ->
                val o = JSONObject(r.body?.string() ?: "{}")
                if (!r.isSuccessful) return@withContext Result.failure(Exception(o.optString("message", "HTTP ${r.code}")))
                Result.success(o.optString("warning").ifBlank { null }.takeUnless { o.isNull("warning") })
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun photo(t: LocalTicket, path: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$api/tickets/${t.number}/photo?path=${Uri.encode(path)}")
                .header("X-Ticket-Secret", t.secret).build()
            client.newCall(req).execute().use { r ->
                val bytes = if (r.isSuccessful) r.body?.bytes() else null
                bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }
        } catch (_: Exception) { null }
    }

    // ── Envoi ───────────────────────────────────────────────────────────────────
    private fun multipart(message: String, photos: List<Uri>): MultipartBody.Builder {
        val b = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("message", message)
        photos.take(5).forEachIndexed { i, uri ->
            compress(uri)?.let { bytes ->
                b.addFormDataPart("photos", "photo_${i + 1}.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
            }
        }
        return b
    }

    /** Redimensionne (1600 px max) et recompresse en JPEG : envoi rapide, sous la limite serveur. */
    private fun compress(uri: Uri): ByteArray? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        bmp?.let {
            val scale = 1600f / maxOf(it.width, it.height)
            val scaled = if (scale < 1f) Bitmap.createScaledBitmap(it, (it.width * scale).toInt(), (it.height * scale).toInt(), true) else it
            ByteArrayOutputStream().also { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 82, out) }.toByteArray()
        }
    } catch (_: Exception) { null }
}
