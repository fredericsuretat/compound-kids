package com.suretat.compoundkids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.suretat.compoundkids.data.SCENARIOS
import com.suretat.compoundkids.data.INFLATION_RATE

private data class LearnCard(
    val emoji: String,
    val title: String,
    val body: String,
    val color: Color
)

private val LEARN_CARDS = listOf(
    LearnCard(
        emoji = "🧲",
        title = "Les intérêts composés : l'argent qui fait des bébés",
        body  = """
Imagine que tu as 100 € et que chaque année ils gagnent 10 %.
La première année tu gagnes 10 € → tu as 110 €.
La deuxième année tu gagnes 10 % de 110 € = 11 € → tu as 121 €.
La troisième : 10 % de 121 € = 12,10 € → 133,10 €.

Tu n'as pas fait de travail supplémentaire — les intérêts de l'année dernière génèrent eux-mêmes des intérêts. C'est ça, les intérêts composés.

Plus tu attends longtemps, plus l'effet est spectaculaire. C'est comme une boule de neige qui dévale une montagne : elle grossit de plus en plus vite.
        """.trimIndent(),
        color = Color(0xFF1565C0)
    ),
    LearnCard(
        emoji = "⏳",
        title = "Le temps est ton meilleur ami",
        body  = """
Léa commence à investir 50 €/mois à 18 ans.
Tom commence à investir 50 €/mois à 28 ans.
À 58 ans, avec un ETF Monde à 9,5 %/an :

• Léa a 40 ans d'investissement → environ 340 000 €
• Tom a 30 ans d'investissement → environ 126 000 €

Tom a investi 10 ans de moins, mais son résultat est presque 3 × plus petit.
Ces 10 années supplémentaires de Léa valent plus que tout l'argent que Tom a versé.

Commencer tôt, même avec peu d'argent, bat toujours commencer tard avec beaucoup d'argent.
        """.trimIndent(),
        color = Color(0xFF2E7D32)
    ),
    LearnCard(
        emoji = "🧮",
        title = "La règle des 72",
        body  = """
Un truc magique pour savoir en combien d'années ton argent double :

             72 ÷ taux d'intérêt = années pour doubler

Exemples :
• Livret A à 2,4 % → 72 ÷ 2,4 = 30 ans pour doubler
• LifeStrategy à 7 % → 72 ÷ 7 = 10,3 ans pour doubler
• ETF Monde à 9,5 % → 72 ÷ 9,5 = 7,6 ans pour doubler

Avec l'ETF Monde, ton argent double environ tous les 7,6 ans !
En 30 ans, il pourrait doubler 4 fois : 1 000 € → 2 000 € → 4 000 € → 8 000 € → 16 000 €.
        """.trimIndent(),
        color = Color(0xFFE65100)
    ),
    LearnCard(
        emoji = "👻",
        title = "L'inflation : l'ennemie invisible",
        body  = """
L'inflation, c'est quand les prix augmentent chaque année.
Si l'inflation est de 2 %/an, un jouet qui coûte 100 € aujourd'hui coûtera environ 149 € dans 20 ans.

Ton argent dans ta poche ne fait rien, mais les prix montent → tu peux acheter de moins en moins.

Avec 2 % d'inflation/an :
• Livret A à 2,4 % → rendement réel : +0,4 %/an (à peine !)
• ETF Monde à 9,5 % → rendement réel : +7,5 %/an (tu gagnes vraiment)

Dans l'appli, active "Après inflation" pour voir ce que vaut vraiment ton argent en euros d'aujourd'hui.
        """.trimIndent(),
        color = Color(0xFF6A1B9A)
    ),
    LearnCard(
        emoji = "⚠️",
        title = "Les risques des ETF",
        body  = """
Les ETF actions ne sont pas garantis. Voici ce qui s'est passé dans l'histoire :

• Crise 2000-2002 (bulle internet) : -50 % en 3 ans
• Crise 2008-2009 (subprimes) : -55 % en 18 mois
• COVID mars 2020 : -34 % en 5 semaines (puis +60 % en 5 mois)

Si tu avais besoin de cet argent pendant une crise, tu le vendais à perte.

La règle d'or : n'investis en ETF que de l'argent dont tu n'auras pas besoin pendant au moins 8-10 ans.

Pour les projets à court terme (vacances, téléphone, voiture) → Livret A.
Pour la retraite ou les gros projets à 20+ ans → ETF.
        """.trimIndent(),
        color = Color(0xFFC62828)
    ),
    LearnCard(
        emoji = "📊",
        title = "D'où viennent ces chiffres ?",
        body  = """
Les taux utilisés dans l'appli sont basés sur des données historiques réelles :

👛 Poche : 0 % — par définition, l'argent liquide ne rapporte rien.

🏦 Livret A 2,4 % : taux officiel depuis février 2025.
   Historique depuis 1981 : de 8,5 % (1981) à 0,5 % (2020-2022).
   Moyenne long terme : ~3,5 %/an.

📈 LifeStrategy 60/40 à 7 % : basé sur la performance historique
   d'un portefeuille mixte 60 % actions / 40 % obligations mondiales
   depuis 1973 (source : Vanguard research).

🌍 ETF Monde (MSCI World) à 9,5 % : performance en EUR du MSCI
   World depuis sa création en 1970 jusqu'en 2024.
   Sources : MSCI, Vanguard, Morningstar.

L'inflation à 2 %/an correspond à l'objectif de la Banque Centrale
Européenne (BCE) et à la moyenne observée en zone euro depuis 2000.

Ces taux sont des moyennes historiques — le passé ne garantit pas l'avenir.
        """.trimIndent(),
        color = Color(0xFF37474F)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 Comment ça marche ?", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp)
        ) {
            // Intro banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "Comprendre avant d'investir",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Cette appli utilise des données historiques réelles pour te montrer comment ton argent peut grandir selon où tu le mets. Ce n'est pas un conseil financier — c'est de l'éducation !",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Scenario quick cards
            item {
                Text(
                    "Les 4 options comparées",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(SCENARIOS) { scenario ->
                ScenarioInfoCard(scenario)
            }

            // Learn cards
            item {
                Text(
                    "Les concepts clés",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(LEARN_CARDS) { card ->
                LearnCardView(card)
            }

            // Support (demande au développeur, avec captures, et suivi de la réponse)
            item { SupportEntry() }

            // Footer
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "⚖️ Ceci est un outil éducatif. Les performances passées ne garantissent pas les performances futures. Consultez un conseiller financier pour de vrais investissements.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ScenarioInfoCard(scenario: com.suretat.compoundkids.data.Scenario) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(scenario.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(scenario.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        scenario.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scenario.color
                    )
                    Text(
                        "${(scenario.annualRate * 100).let { if (it == 0.0) "0" else "%.1f".format(it) }} %/an",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = scenario.color
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    scenario.historicalNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun LearnCardView(card: LearnCard) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    card.title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = card.color
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                card.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SupportEntry() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var gate by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val version = androidx.compose.runtime.remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?" } catch (_: Exception) { "?" }
    }
    OutlinedButton(onClick = { gate = true }, modifier = Modifier.fillMaxWidth()) {
        Text("✉️ Espace parents : contacter le support")
    }
    // Contrôle parental : le support demande un email et peut envoyer des photos, il est donc
    // réservé à un adulte (app utilisée par des enfants).
    if (gate) ParentGate(onPass = { gate = false; open = true }, onDismiss = { gate = false })
    if (open) {
        com.suretat.compoundkids.support.SupportScreen(
            com.suretat.compoundkids.support.SupportConfig(
                appKey = "compound-kids", appLabel = "Les sous qui poussent", versionName = version,
            ),
        ) { open = false }
    }
}

@Composable
private fun ParentGate(onPass: () -> Unit, onDismiss: () -> Unit) {
    val a = androidx.compose.runtime.remember { (6..9).random() }
    val b = androidx.compose.runtime.remember { (6..9).random() }
    var answer by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var wrong by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Espace parents") },
        text = {
            Column {
                Text("Cette partie est réservée aux adultes. Combien font $a × $b ?")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it.filter(Char::isDigit).take(3); wrong = false },
                    singleLine = true,
                    isError = wrong,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                )
                if (wrong) Text("Ce n'est pas ça.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (answer.toIntOrNull() == a * b) onPass() else wrong = true }) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
