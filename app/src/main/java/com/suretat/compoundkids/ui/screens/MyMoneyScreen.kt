package com.suretat.compoundkids.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.suretat.compoundkids.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HORIZONS = listOf(5, 10, 20)

/** Ligne en cours d'édition (texte brut des champs). */
private data class Draft(val name: String, val amount: String, val rate: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMoneyScreen(navController: NavController) {
    val context = LocalContext.current
    var savings by remember { mutableStateOf(AccountStore.load(context)) }
    var gate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }

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
                initial = savings.accounts,
                modifier = Modifier.padding(padding),
                onSave = { AccountStore.save(context, it); savings = AccountStore.load(context); editing = false }
            )
        } else {
            MoneyOverview(savings, Modifier.padding(padding), onSettings = { gate = true })
        }
    }
}

@Composable
private fun MoneyOverview(savings: Savings, modifier: Modifier, onSettings: () -> Unit) {
    val total = savings.accounts.sumOf { it.amount }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (savings.accounts.isEmpty()) {
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
                        if (savings.updatedAt > 0) Text(
                            "Montants mis à jour le " + SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(savings.updatedAt)),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Si tu n'y touches pas, ça pourrait devenir :", style = MaterialTheme.typography.labelLarge)
                        HORIZONS.forEach { y ->
                            val future = savings.accounts.sumOf { projectAccount(it, y) }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("dans $y ans")
                                Text(formatCents(future), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
            items(savings.accounts) { acc ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(acc.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(formatCents(acc.amount), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Rendement estimé : " + String.format(Locale.FRANCE, "%.1f", acc.annualRate * 100) + " %/an",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            HORIZONS.joinToString("   ") { y -> "$y ans : ${formatCents(projectAccount(acc, y))}" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                Text(
                    "Ce sont des estimations : les rendements ne sont pas garantis (sauf Livret A), et ne comptent pas les futurs versements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        item {
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Text("⚙️ Paramètres (parents) : modifier les montants")
            }
        }
    }
}

@Composable
private fun SettingsEditor(initial: List<Account>, modifier: Modifier, onSave: (List<Account>) -> Unit) {
    val rows: SnapshotStateList<Draft> = remember {
        initial.map { Draft(it.name, "%.2f".format(Locale.FRANCE, it.amount), "%.1f".format(Locale.FRANCE, it.annualRate * 100)) }
            .toMutableStateList()
    }
    // Une ligne est valide si elle a un nom, un montant et un rendement lisibles.
    val valid = rows.all { it.name.isNotBlank() && parseAmount(it.amount) != null && parseAmount(it.rate) != null }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Saisissez les montants réels de chaque compte. Rendements indicatifs : espèces 0 %, " +
                    "Livret A 2,4 %, fonds euros ~2,5 %, LifeStrategy 60 ~7 %, ETF Monde ~9,5 %.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        itemsIndexed(rows) { i, row ->
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(row.name, { rows[i] = row.copy(name = it) }, label = { Text("Compte") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(row.amount, { rows[i] = row.copy(amount = it) }, label = { Text("Montant (€)") },
                            singleLine = true, isError = parseAmount(row.amount) == null, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(row.rate, { rows[i] = row.copy(rate = it) }, label = { Text("% / an") },
                            singleLine = true, isError = parseAmount(row.rate) == null, modifier = Modifier.weight(0.6f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        TextButton(onClick = { rows.removeAt(i) }) { Text("🗑️") }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { rows.add(Draft("", "0", "0")) }, modifier = Modifier.fillMaxWidth()) {
                Text("➕ Ajouter un compte")
            }
        }
        item {
            Button(
                enabled = valid,
                onClick = { onSave(rows.map { Account(it.name.trim(), parseAmount(it.amount)!!, parseAmount(it.rate)!! / 100) }) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer") }
        }
    }
}
