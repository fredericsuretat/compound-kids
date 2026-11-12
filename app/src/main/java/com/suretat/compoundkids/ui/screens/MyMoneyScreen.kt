package com.suretat.compoundkids.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.suretat.compoundkids.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HORIZONS = listOf(5, 10, 20)
private val GREEN = Color(0xFF2E7D32)

/** Ligne en cours d'édition (texte brut des champs). */
private data class Draft(val name: String, val amount: String, val invested: String, val rate: String)

private fun Account.toDraft() = Draft(
    name, "%.2f".format(Locale.FRANCE, amount), "%.2f".format(Locale.FRANCE, invested), "%.1f".format(Locale.FRANCE, annualRate * 100)
)

private fun Draft.isValid() = name.isNotBlank() && parseAmount(amount) != null && parseAmount(invested) != null && parseAmount(rate) != null
private fun Draft.toAccount() = Account(name.trim(), parseAmount(amount)!!, parseAmount(invested)!!, parseAmount(rate)!! / 100)

private fun signed(amount: Double) = (if (amount >= 0) "+" else "") + formatCents(amount)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMoneyScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savings by remember { mutableStateOf(AccountStore.load(context)) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var gate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }

    // Téléphone relié : on récupère les derniers montants (sinon on garde ceux enregistrés sur l'appareil).
    LaunchedEffect(savings.link?.code) {
        val link = savings.link ?: return@LaunchedEffect
        runCatching { FamilyClient.fetch(link.code) }
            .onSuccess { (_, accounts, updatedAt) ->
                AccountStore.save(context, accounts, updatedAt); savings = AccountStore.load(context); syncError = null
            }
            .onFailure { syncError = "Pas de connexion : montants enregistrés sur ce téléphone." }
    }

    if (gate) ParentGate(onPass = { gate = false; editing = true }, onDismiss = { gate = false })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "⚙️ Paramètres" else "💰 Mes vrais sous", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (editing) editing = false else navController.popBackStack() }) {
                        Text("←", fontSize = 22.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (editing) {
            SettingsEditor(
                savings = savings,
                modifier = Modifier.padding(padding),
                onChanged = { savings = AccountStore.load(context) },
                onDone = { editing = false },
                scope = scope
            )
        } else {
            MoneyOverview(savings, syncError, Modifier.padding(padding), onSettings = { gate = true })
        }
    }
}

@Composable
private fun MoneyOverview(savings: Savings, syncError: String?, modifier: Modifier, onSettings: () -> Unit) {
    val accounts = savings.accounts
    val total = accounts.sumOf { it.amount }
    val invested = accounts.sumOf { it.invested }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (accounts.isEmpty()) {
            item {
                Text(
                    "Ici tu verras ton vrai argent et ce qu'il pourrait devenir. " +
                        "Demande à un parent d'ajouter tes comptes dans ⚙️ Paramètres.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Aujourd'hui, tu as", style = MaterialTheme.typography.labelLarge)
                        Text(formatCents(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Argent versé : ${formatCents(invested)}  ·  gagné : ${signed(total - invested)}",
                            color = if (total >= invested) GREEN else MaterialTheme.colorScheme.error)
                        if (savings.updatedAt > 0) Text(
                            "Montants du " + SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(savings.updatedAt)) +
                                if (savings.link?.isParent == false) " (envoyés par tes parents)" else "",
                            style = MaterialTheme.typography.bodySmall
                        )
                        syncError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                        Spacer(Modifier.height(12.dp))
                        Text("Si on n'y touche pas, ça pourrait devenir :", style = MaterialTheme.typography.labelLarge)
                        HORIZONS.forEach { y ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("dans $y ans")
                                Text(formatCents(accounts.sumOf { projectAccount(it, y) }), fontWeight = FontWeight.Bold, color = GREEN)
                            }
                        }
                    }
                }
            }
            item { WhatIfCard(accounts) }
            items(accounts) { acc ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(acc.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(formatCents(acc.amount), fontWeight = FontWeight.Bold)
                        }
                        Text("Versé : ${formatCents(acc.invested)}  ·  gagné : ${signed(acc.amount - acc.invested)}",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Rendement estimé : " + String.format(Locale.FRANCE, "%.1f", acc.annualRate * 100) + " %/an",
                            style = MaterialTheme.typography.bodySmall)
                        Text(HORIZONS.joinToString("   ") { y -> "$y ans : ${formatCents(projectAccount(acc, y))}" },
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text(
                    "Ce sont des estimations : les rendements ne sont pas garantis (sauf Livret A) et peuvent varier d'une année à l'autre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        item {
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Text("⚙️ Paramètres (parents)")
            }
        }
    }
}

/** « Et si on ajoutait x € sur tel compte ? » : compare avec et sans l'ajout. */
@Composable
private fun WhatIfCard(accounts: List<Account>) {
    // Par défaut le compte au meilleur rendement (souvent LifeStrategy).
    var selected by remember(accounts) { mutableIntStateOf(accounts.indices.maxByOrNull { accounts[it].annualRate } ?: 0) }
    var extraText by remember { mutableStateOf("20") }
    var monthly by remember { mutableStateOf(true) }
    val extra = parseAmount(extraText) ?: 0.0
    val target = accounts.getOrNull(selected) ?: return

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🤔 Et si on ajoutait…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(extraText, { extraText = it.take(8) }, label = { Text("€") }, singleLine = true,
                    modifier = Modifier.width(110.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                FilterChip(selected = monthly, onClick = { monthly = true }, label = { Text("chaque mois") })
                FilterChip(selected = !monthly, onClick = { monthly = false }, label = { Text("une fois") })
            }
            Text("sur le compte :", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEachIndexed { i, a -> FilterChip(selected = i == selected, onClick = { selected = i }, label = { Text(a.name) }) }
            }
            HORIZONS.forEach { y ->
                val without = accounts.sumOf { projectAccount(it, y) }
                val with = without - projectAccount(target, y) +
                    projectAccount(target, y, monthly = if (monthly) extra else 0.0, lump = if (monthly) 0.0 else extra)
                val added = if (monthly) extra * 12 * y else extra
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("dans $y ans")
                        Text(formatCents(with), fontWeight = FontWeight.Bold, color = GREEN)
                    }
                    Text("au lieu de ${formatCents(without)} · tu aurais ajouté ${formatCents(added)}, " +
                        "les intérêts en plus : ${signed(with - without - added)}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SettingsEditor(
    savings: Savings,
    modifier: Modifier,
    onChanged: () -> Unit,
    onDone: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    val link = savings.link
    val readOnly = link != null && !link.isParent
    val rows: SnapshotStateList<Draft> = remember { savings.accounts.map { it.toDraft() }.toMutableStateList() }
    val valid = rows.all { it.isValid() }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var codeInput by remember { mutableStateOf("") }

    fun saveLocal(): List<Account> = rows.map { it.toAccount() }.also { AccountStore.save(context, it); onChanged() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (readOnly) {
            item {
                Text("Ce téléphone est relié en mode enfant : les montants sont mis à jour depuis le téléphone d'un parent.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            item {
                Text(
                    "Pour chaque compte : sa valeur aujourd'hui, l'argent réellement versé dessus, et un rendement estimé. " +
                        "Repères : espèces 0 %, Livret A 2,4 %, fonds euros ~2,5 %, LifeStrategy 60 ~7 %, ETF Monde ~9,5 %.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            itemsIndexed(rows) { i, row ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(row.name, { rows[i] = row.copy(name = it) }, label = { Text("Compte") },
                                singleLine = true, modifier = Modifier.weight(1f))
                            TextButton(onClick = { rows.removeAt(i) }) { Text("🗑️") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DecimalField(row.amount, "Valeur (€)", Modifier.weight(1f)) { rows[i] = row.copy(amount = it) }
                            DecimalField(row.invested, "Versé (€)", Modifier.weight(1f)) { rows[i] = row.copy(invested = it) }
                            DecimalField(row.rate, "% / an", Modifier.weight(0.7f)) { rows[i] = row.copy(rate = it) }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { rows.add(Draft("", "0", "0", "0")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("➕ Ajouter un compte")
                }
            }
            item {
                Button(
                    enabled = valid && !busy,
                    onClick = {
                        val accounts = saveLocal()
                        if (link == null) { onDone(); return@Button }
                        busy = true
                        scope.launch {
                            runCatching { FamilyClient.push(link.code, accounts) }
                                .onSuccess { busy = false; onDone() }
                                .onFailure { busy = false; message = "Enregistré sur ce téléphone, mais pas envoyé : ${it.message}" }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (link != null) "Enregistrer et envoyer" else "Enregistrer") }
            }
        }

        // ── Lien familial ─────────────────────────────────────────────
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📱 Lien familial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (link == null) {
                        Text("Reliez ce téléphone à celui de l'enfant : il verra les montants dès que vous les modifiez ici. " +
                            "Les montants sont alors stockés sur le serveur du développeur, lisibles seulement avec vos codes.",
                            style = MaterialTheme.typography.bodySmall)
                        Button(enabled = valid && !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                            val accounts = saveLocal()
                            busy = true
                            scope.launch {
                                runCatching { FamilyClient.create(accounts) }
                                    .onSuccess { (parent, child) ->
                                        AccountStore.saveLink(context, FamilyLink(parent, true, child)); onChanged(); message = null
                                    }
                                    .onFailure { message = "Échec : ${it.message}" }
                                busy = false
                            }
                        }) { Text("Créer le lien (ce téléphone est celui d'un parent)") }
                        HorizontalDivider()
                        Text("Vous avez reçu un code ?", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(codeInput, { codeInput = it.uppercase().take(16) }, label = { Text("Code parent ou enfant") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(enabled = codeInput.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                            val code = codeInput.filter { it.isLetterOrDigit() }
                            busy = true
                            scope.launch {
                                runCatching { FamilyClient.fetch(code) }
                                    .onSuccess { (isParent, accounts, updatedAt) ->
                                        AccountStore.save(context, accounts, updatedAt)
                                        AccountStore.saveLink(context, FamilyLink(code, isParent)); onChanged(); onDone()
                                    }
                                    .onFailure { message = "Code refusé : ${it.message}" }
                                busy = false
                            }
                        }) { Text("Relier avec ce code (remplace les montants de ce téléphone)") }
                    } else {
                        if (link.isParent) {
                            link.childCode?.let { CodeLine("Code à saisir sur le téléphone de l'enfant", it) }
                            CodeLine("Code pour l'autre parent (peut aussi modifier)", link.code)
                        } else {
                            Text("Relié en mode enfant (lecture seule).", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                            AccountStore.saveLink(context, null); onChanged()
                        }) { Text("Délier ce téléphone") }
                    }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun DecimalField(value: String, label: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, isError = parseAmount(value) == null,
        modifier = modifier, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun CodeLine(label: String, code: String) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    SelectionContainer { Text(formatCode(code), fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
}
