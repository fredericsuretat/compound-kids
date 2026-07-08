package com.suretat.compoundkids.data

import androidx.compose.ui.graphics.Color

const val INFLATION_RATE = 0.02  // 2 % /an — objectif BCE long terme

data class SimPoint(val year: Int, val nominal: Double, val real: Double)

data class Scenario(
    val name: String,
    val emoji: String,
    val color: Color,
    val annualRate: Double,
    val tagline: String,
    val historicalNote: String
)

data class ScenarioResult(val scenario: Scenario, val points: List<SimPoint>)

val SCENARIOS = listOf(
    Scenario(
        name = "Dans ta poche",
        emoji = "👛",
        color = Color(0xFF9E9E9E),
        annualRate = 0.00,
        tagline = "L'argent qui dort",
        historicalNote = "0 % de rendement. Garanti de ne rien gagner — et l'inflation grignote ton pouvoir d'achat chaque année."
    ),
    Scenario(
        name = "Livret A",
        emoji = "🏦",
        color = Color(0xFF1565C0),
        annualRate = 0.024,
        tagline = "Le classique sûr",
        historicalNote = "Taux actuel : 2,4 % (depuis fév. 2025). Taux moyen depuis 1981 : ~3,5 %/an. Garanti par l'État, sans risque de perte."
    ),
    Scenario(
        name = "LifeStrategy 60",
        emoji = "📈",
        color = Color(0xFFE65100),
        annualRate = 0.070,
        tagline = "60% actions + 40% oblig.",
        historicalNote = "Vanguard LifeStrategy 60% Acc (IE00BMVB5R75). Portefeuille 60/40 mondial depuis 1973 : ~7 %/an en moyenne. Risque modéré, adapté à un horizon 10+ ans."
    ),
    Scenario(
        name = "ETF Monde",
        emoji = "🌍",
        color = Color(0xFF2E7D32),
        annualRate = 0.095,
        tagline = "1500 entreprises mondiales",
        historicalNote = "MSCI World depuis 1970 : ~9,5 %/an en moyenne (en EUR). Risque plus élevé — des baisses de -30 % à -50 % sont possibles, mais sur 20+ ans l'historique est très favorable."
    )
)
