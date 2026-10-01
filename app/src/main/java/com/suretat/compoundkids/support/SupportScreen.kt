package com.suretat.compoundkids.support

// Écran de support commun aux apps (copié à l'identique, seul le package change) :
// « Mes demandes » (suivi), nouvelle demande avec photos, conversation + réponse.

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch

private sealed interface SupportView {
    data object List : SupportView
    data object New : SupportView
    data class Detail(val ticket: LocalTicket) : SupportView
}

@Composable
fun SupportScreen(config: SupportConfig, onClose: () -> Unit) {
    val context = LocalContext.current
    val client = remember { SupportClient(context.applicationContext, config) }
    var view by remember { mutableStateOf<SupportView>(if (client.localTickets().isEmpty()) SupportView.New else SupportView.List) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT)
        }
        BackHandler { if (view is SupportView.List || client.localTickets().isEmpty()) onClose() else view = SupportView.List }
        Surface(Modifier.fillMaxSize()) {
            // Plein écran edge-to-edge : barres système + clavier, sinon les boutons du bas sont cachés.
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (val v = view) {
                            SupportView.List -> "💬 Mes demandes"
                            SupportView.New -> "✉️ Nouvelle demande"
                            is SupportView.Detail -> "Demande n° ${v.ticket.number}"
                        },
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    )
                    if (view !is SupportView.List && client.localTickets().isNotEmpty()) {
                        TextButton(onClick = { view = SupportView.List }) { Text("← Liste") }
                    }
                    TextButton(onClick = onClose) { Text("Fermer") }
                }
                HorizontalDivider()
                when (val v = view) {
                    SupportView.List -> TicketList(client, onNew = { view = SupportView.New }, onOpen = { view = SupportView.Detail(it) })
                    SupportView.New -> NewTicket(client, onCreated = { view = SupportView.List })
                    is SupportView.Detail -> TicketConversation(client, v.ticket)
                }
            }
        }
    }
}

@Composable
private fun TicketList(client: SupportClient, onNew: () -> Unit, onOpen: (LocalTicket) -> Unit) {
    val local = remember { client.localTickets() }
    var summaries by remember { mutableStateOf<Map<Int, TicketSummary>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        client.statuses().onSuccess { list -> summaries = list.associateBy { it.number } }
            .onFailure { error = "Impossible de vérifier l'état des demandes (${it.message})." }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("✉️ Nouvelle demande") }
        Spacer(Modifier.height(12.dp))
        error?.let { Text(it, color = Color(0xFFC62828), fontSize = 12.sp); Spacer(Modifier.height(8.dp)) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(local, key = { it.number }) { t ->
                val s = summaries[t.number]
                val unread = s != null && s.lastFrom == "support" && s.messageCount > client.seenCount(t.number)
                Card(Modifier.fillMaxWidth().clickable { onOpen(t) }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s?.title ?: t.title, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.weight(1f))
                            if (s != null) StateBadge(s.state)
                        }
                        Text(
                            when {
                                s == null -> "n° ${t.number}"
                                unread -> "🔔 Nouvelle réponse"
                                s.lastFrom == "support" -> "Réponse reçue"
                                else -> "En attente de réponse"
                            },
                            fontSize = 12.sp,
                            color = if (unread) Color(0xFF1565C0) else Color(0xFF757575),
                            fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StateBadge(state: String) {
    val (label, color) = if (state == "open") "En cours" to Color(0xFF2E7D32) else "Résolue" to Color(0xFF6A1B9A)
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(color).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun PhotoPicker(photos: List<Uri>, onChange: (List<Uri>) -> Unit) {
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) onChange((photos + uris).distinct().take(5))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = photos.size < 5, modifier = Modifier.fillMaxWidth(),
        ) { Text(if (photos.isEmpty()) "📷 Ajouter des photos / captures (5 max)" else "📷 Ajouter une photo (${photos.size}/5)") }
        if (photos.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.forEach { uri ->
                    Box {
                        UriThumb(uri)
                        Text("✕", color = Color.White, fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(10.dp))
                                .background(Color(0x99000000)).clickable { onChange(photos - uri) }.padding(horizontal = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UriThumb(uri: Uri) {
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(null, uri) {
        value = try {
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 })
            }
        } catch (_: Exception) { null }
    }
    Box(Modifier.size(84.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) } ?: Text("📷")
    }
}

@Composable
private fun NewTicket(client: SupportClient, onCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(client.email) }
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val emailOk = client.isValidEmail(email)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (done) {
            Text("✅ Demande envoyée, merci ! Vous serez prévenu ici dès qu'une réponse arrive.", color = Color(0xFF2E7D32))
            warning?.let { Text("⚠️ $it", fontSize = 12.sp, color = Color(0xFFE65100)) }
            Button(onClick = onCreated, modifier = Modifier.fillMaxWidth()) { Text("Voir mes demandes") }
            return@Column
        }
        Text("Une question, un bug, une idée ? Décrivez-le ici, avec des captures d'écran si besoin. " +
            "Vous pourrez suivre la réponse dans « Mes demandes ».", fontSize = 13.sp, color = Color(0xFF757575))
        OutlinedTextField(
            value = email, onValueChange = { email = it; error = null },
            label = { Text("Votre email (obligatoire, pour la réponse)") }, singleLine = true,
            isError = email.isNotBlank() && !emailOk,
            supportingText = if (email.isNotBlank() && !emailOk) { { Text("Adresse email invalide") } } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(value = message, onValueChange = { message = it; error = null },
            label = { Text("Votre message") }, minLines = 5, modifier = Modifier.fillMaxWidth())
        PhotoPicker(photos) { photos = it }
        error?.let { Text("Échec de l'envoi : $it", fontSize = 12.sp, color = Color(0xFFC62828)) }
        Button(
            onClick = {
                sending = true; error = null
                scope.launch {
                    client.create(message.trim(), photos, email)
                        .onSuccess { warning = it; done = true }
                        .onFailure { error = it.message ?: "erreur inconnue" }
                    sending = false
                }
            },
            enabled = !sending && emailOk && (message.isNotBlank() || photos.isNotEmpty()),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (sending) "Envoi en cours…" else "Envoyer") }
    }
}

@Composable
private fun TicketConversation(client: SupportClient, ticket: LocalTicket) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<TicketDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reply by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var sending by remember { mutableStateOf(false) }
    var warning by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            client.detail(ticket).onSuccess { detail = it; client.markSeen(ticket.number, it.messages.size) }
                .onFailure { error = "Chargement impossible (${it.message})." }
        }
    }
    LaunchedEffect(ticket.number) { load() }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            error?.let { Text(it, color = Color(0xFFC62828), fontSize = 12.sp) }
            val d = detail
            if (d == null && error == null) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            if (d != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    StateBadge(d.state)
                }
                d.messages.forEach { m -> MessageBubble(client, ticket, m) }
            }
        }
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            warning?.let { Text("⚠️ $it", fontSize = 12.sp, color = Color(0xFFE65100)) }
            OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text("Répondre") },
                maxLines = 4, modifier = Modifier.fillMaxWidth())
            PhotoPicker(photos) { photos = it }
            Button(
                onClick = {
                    sending = true
                    scope.launch {
                        client.reply(ticket, reply.trim(), photos)
                            .onSuccess { warning = it; reply = ""; photos = emptyList(); load() }
                            .onFailure { error = "Envoi impossible : ${it.message}" }
                        sending = false
                    }
                },
                enabled = !sending && (reply.isNotBlank() || photos.isNotEmpty()),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (sending) "Envoi…" else "Envoyer la réponse") }
        }
    }
}

@Composable
private fun MessageBubble(client: SupportClient, ticket: LocalTicket, m: TicketMessage) {
    val mine = m.from == "user"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Text(if (mine) "Vous" else "Support", fontSize = 11.sp, color = Color(0xFF757575))
        Column(
            Modifier.fillMaxWidth(0.88f).clip(RoundedCornerShape(12.dp))
                .background(if (mine) Color(0xFFE3F2FD) else Color(0xFFF1F8E9)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val text = m.body.removePrefix("**Réponse de l'utilisateur :**").trim()
            if (text.isNotBlank()) Text(text, fontSize = 14.sp)
            if (m.photos.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    m.photos.forEach { p ->
                        val bmp by produceState<Bitmap?>(null, p) { value = client.photo(ticket, p) }
                        Box(Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                            bmp?.let { Image(it.asImageBitmap(), "Photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                ?: Text("📷", fontSize = 20.sp)
                        }
                    }
                }
            }
            Text(m.createdAt.take(16).replace('T', ' '), fontSize = 10.sp, color = Color(0xFF9E9E9E))
        }
    }
}
