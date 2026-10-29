package com.suretat.compoundkids.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.suretat.compoundkids.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private const val SITE_PAGE = "https://frederic.suretat.com/les-sous-qui-poussent"
private const val LATEST_RELEASE = "https://api.github.com/repos/fredericsuretat/compound-kids/releases/latest"

/** Dernière version publiée sur GitHub (dépôt public) : nom de version et lien de l'APK. */
private data class Release(val version: String, val apkUrl: String?)

private fun fetchLatest(): Release {
    val json = JSONObject(URL(LATEST_RELEASE).openConnection().apply {
        connectTimeout = 10_000; readTimeout = 10_000
        setRequestProperty("Accept", "application/vnd.github+json")
    }.getInputStream().bufferedReader().use { it.readText() })
    val assets = json.optJSONArray("assets")
    val apk = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }
        .firstOrNull { it.optString("name").endsWith(".apk") }?.optString("browser_download_url")
    return Release(json.getString("tag_name").removePrefix("v"), apk)
}

/** « 1.10.0 » > « 1.9.2 » : comparaison nombre par nombre. */
internal fun isNewer(remote: String, local: String): Boolean {
    val r = remote.split('.').map { it.toIntOrNull() ?: 0 }
    val l = local.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(r.size, l.size)) {
        val d = r.getOrElse(i) { 0 } - l.getOrElse(i) { 0 }
        if (d != 0) return d > 0
    }
    return false
}

private fun installedFromPlay(context: Context): Boolean = try {
    val installer = if (Build.VERSION.SDK_INT >= 30) context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    else @Suppress("DEPRECATION") context.packageManager.getInstallerPackageName(context.packageName)
    installer == "com.android.vending"
} catch (_: Exception) { false }

private fun open(context: Context, url: String) =
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

/**
 * Espace parents (derrière la question de calcul) : version, recherche de mise à jour, QR code et lien de la page
 * de l'application, contact du support. Pas d'installation par l'appli elle-même (règles Google Play) : la mise à
 * jour s'ouvre dans le Play Store si l'appli en vient, sinon l'APK se télécharge dans le navigateur.
 */
@Composable
internal fun ParentsDialog(version: String, onSupport: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var update by remember { mutableStateOf<Release?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Espace parents") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version installée : $version")
                OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    busy = true; status = "Recherche…"; update = null
                    scope.launch {
                        try {
                            val r = withContext(Dispatchers.IO) { fetchLatest() }
                            if (isNewer(r.version, version)) { update = r; status = "Version ${r.version} disponible." }
                            else status = "L'application est à jour."
                        } catch (_: Exception) { status = "Recherche impossible : pas de connexion ?" }
                        busy = false
                    }
                }) { Text("🔄 Vérifier les mises à jour") }
                if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
                update?.let { r ->
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        if (installedFromPlay(context)) open(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
                        else open(context, r.apkUrl ?: SITE_PAGE)
                    }) { Text("⬇️ Télécharger la version ${r.version}") }
                }
                HorizontalDivider()
                Text("Installer l'application sur un autre téléphone :", style = MaterialTheme.typography.bodySmall)
                Image(painterResource(R.drawable.qr_page), contentDescription = "QR code de la page de l'application",
                    modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally))
                TextButton(onClick = { open(context, SITE_PAGE) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("frederic.suretat.com/les-sous-qui-poussent")
                }
                HorizontalDivider()
                OutlinedButton(onClick = onSupport, modifier = Modifier.fillMaxWidth()) { Text("✉️ Contacter le support") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}
