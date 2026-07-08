package com.suretat.compoundkids.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.suretat.compoundkids.data.*
import com.suretat.compoundkids.ui.components.CompoundChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(navController: NavController) {
    var initialAmount        by remember { mutableFloatStateOf(1000f) }
    var monthlyContribution  by remember { mutableFloatStateOf(50f) }
    var years                by remember { mutableIntStateOf(20) }
    var bannerDismissed      by rememberSaveable { mutableStateOf(false) }
    var showReal             by remember { mutableStateOf(false) }

    val results      = remember(initialAmount, monthlyContribution, years) {
        simulateAll(initialAmount.toDouble(), monthlyContribution.toDouble(), years)
    }
    val invested     = totalInvested(initialAmount.toDouble(), monthlyContribution.toDouble(), years)
    val bestFinal    = results.last().points.last().let { if (showReal) it.real else it.nominal }
    val worstFinal   = results.first().points.last().let { if (showReal) it.real else it.nominal }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "🌱 Les Sous Qui Poussent",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("learn") }) {
                        Icon(Icons.Default.Info, contentDescription = "Apprendre")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Sliders ──────────────────────────────────────────────
            item {
                InputCard(
                    initialAmount       = initialAmount,
                    onInitialChange     = { initialAmount = it },
                    monthlyContribution = monthlyContribution,
                    onMonthlyChange     = { monthlyContribution = it },
                    years               = years,
                    onYearsChange       = { years = it },
                    invested            = invested
                )
            }

            // ── Toggle inflation ─────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showReal,
                        onClick  = { showReal = false },
                        label    = { Text("Valeur nominale") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = showReal,
                        onClick  = { showReal = true },
                        label    = { Text("Après inflation (2 %/an)") }
                    )
                }
            }

            // ── Chart ─────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text  = if (showReal) "Valeur en euros d'aujourd'hui (pouvoir d'achat réel)"
                                    else "Valeur nominale (chiffres bruts)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        CompoundChart(
                            results  = results,
                            showReal = showReal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    }
                }
            }

            // ── Key insight banner ────────────────────────────────────
            item {
                InsightBanner(
                    bestFinal  = bestFinal,
                    worstFinal = worstFinal,
                    invested   = invested,
                    years      = years,
                    showReal   = showReal
                )
            }

            // ── Result cards (best→worst order) ──────────────────────
            items(results.reversed()) { result ->
                ResultCard(
                    result   = result,
                    invested = invested,
                    years    = years,
                    showReal = showReal
                )
            }

            // ── Bannière Play Store ───────────────────────────────────
            if (!bannerDismissed) {
                item {
                    val context = LocalContext.current
                    RateBanner(
                        onRate    = {
                            bannerDismissed = true
                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.suretat.compoundkids")))
                        },
                        onDismiss = { bannerDismissed = true }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InputCard(
    initialAmount: Float, onInitialChange: (Float) -> Unit,
    monthlyContribution: Float, onMonthlyChange: (Float) -> Unit,
    years: Int, onYearsChange: (Int) -> Unit,
    invested: Double
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        shape     = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text  = "Paramètres de simulation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            SliderRow(
                label    = "Montant de départ",
                value    = initialAmount,
                onValue  = onInitialChange,
                min      = 0f,
                max      = 150_000f,   // plafond PEA
                steps    = 299,        // pas de 500 €
                display  = when {
                    initialAmount >= 1000f -> "${"%.0f".format(initialAmount / 1000)} k€"
                    else                   -> "%.0f €".format(initialAmount)
                }
            )
            SliderRow(
                label    = "Épargne par mois",
                value    = monthlyContribution,
                onValue  = onMonthlyChange,
                min      = 0f,
                max      = 500f,
                steps    = 49,
                display  = "%.0f €/mois".format(monthlyContribution)
            )
            SliderRow(
                label    = "Durée",
                value    = years.toFloat(),
                onValue  = { onYearsChange(it.toInt()) },
                min      = 1f,
                max      = 40f,
                steps    = 38,
                display  = "$years ans"
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Total versé : ${formatEuroFull(invested)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String, value: Float, onValue: (Float) -> Unit,
    min: Float, max: Float, steps: Int, display: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text  = display,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value         = value,
            onValueChange = onValue,
            valueRange    = min..max,
            steps         = steps,
            modifier      = Modifier.fillMaxWidth()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InsightBanner(
    bestFinal: Double, worstFinal: Double,
    invested: Double, years: Int, showReal: Boolean
) {
    val multiplier = if (worstFinal > 0) bestFinal / worstFinal else 0.0
    val gains      = bestFinal - invested
    val gainPct    = if (invested > 0) gains / invested * 100 else 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text  = "💡 Résumé après $years ans",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightStat(
                    label = "ETF Monde",
                    value = formatEuro(bestFinal),
                    sub   = "+${gainPct.toInt()} % de gain",
                    modifier = Modifier.weight(1f)
                )
                InsightStat(
                    label = "vs poche",
                    value = "× %.1f".format(multiplier),
                    sub   = "fois plus d'argent",
                    modifier = Modifier.weight(1f)
                )
                InsightStat(
                    label = "Intérêts",
                    value = formatEuro(gains),
                    sub   = "créés sans travailler",
                    modifier = Modifier.weight(1f)
                )
            }
            if (showReal) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = "⚠️ Ces valeurs reflètent le pouvoir d'achat d'aujourd'hui (corrigées à 2 % d'inflation/an).",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun InsightStat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        Text(
            value,
            style      = MaterialTheme.typography.headlineMedium,
            color      = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center,
            fontSize   = 18.sp
        )
        Text(sub, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultCard(
    result: ScenarioResult,
    invested: Double,
    years: Int,
    showReal: Boolean
) {
    val lastPt    = result.points.last()
    val finalVal  = if (showReal) lastPt.real else lastPt.nominal
    val gains     = (finalVal - invested).coerceAtLeast(0.0)
    val gainFrac  = if (finalVal > 0) gains / finalVal else 0.0
    val scenario  = result.scenario

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(scenario.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        scenario.name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        scenario.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedContent(
                        targetState = finalVal,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                        label = "finalValue"
                    ) { value ->
                        Text(
                            formatEuroFull(value),
                            style      = MaterialTheme.typography.titleLarge,
                            color      = scenario.color,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (scenario.annualRate > 0) {
                        Text(
                            "${scenario.annualRate * 100}%/an",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            // Progress bar: invested vs interests
            Spacer(Modifier.height(10.dp))
            Text(
                text  = "Répartition : ${formatEuro(invested)} versés + ${formatEuro(gains)} d'intérêts",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(gainFrac.toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(scenario.color.copy(alpha = 0.8f))
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Argent versé", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(
                    "${(gainFrac * 100).toInt()} % générés par les intérêts",
                    style = MaterialTheme.typography.labelSmall,
                    color = scenario.color
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RateBanner(onRate: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌱", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "L'app vous est utile ?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Laissez un avis sur le Play Store, ça aide !",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onRate) {
                Icon(Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Noter", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Default.Close, contentDescription = "Fermer",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}
