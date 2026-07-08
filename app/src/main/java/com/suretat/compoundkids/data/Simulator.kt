package com.suretat.compoundkids.data

import kotlin.math.pow

fun simulate(
    initialAmount: Double,
    monthlyContribution: Double,
    years: Int,
    annualRate: Double,
    inflationRate: Double = INFLATION_RATE
): List<SimPoint> {
    val monthlyRate = annualRate / 12
    val points = mutableListOf(SimPoint(0, initialAmount, initialAmount))
    var nominal = initialAmount

    for (year in 1..years) {
        repeat(12) { nominal = nominal * (1.0 + monthlyRate) + monthlyContribution }
        val real = nominal / (1.0 + inflationRate).pow(year.toDouble())
        points.add(SimPoint(year, nominal, real))
    }
    return points
}

fun simulateAll(
    initialAmount: Double,
    monthlyContribution: Double,
    years: Int
): List<ScenarioResult> = SCENARIOS.map { scenario ->
    ScenarioResult(
        scenario = scenario,
        points = simulate(initialAmount, monthlyContribution, years, scenario.annualRate)
    )
}

fun totalInvested(initialAmount: Double, monthlyContribution: Double, years: Int): Double =
    initialAmount + monthlyContribution * years * 12

fun formatEuro(amount: Double): String = when {
    amount >= 1_000_000 -> "%.1fM€".format(amount / 1_000_000)
    amount >= 1_000     -> "%.0fk€".format(amount / 1_000)
    else                -> "%.0f€".format(amount)
}

fun formatEuroFull(amount: Double): String = when {
    amount >= 1_000_000 -> "%.2fM€".format(amount / 1_000_000)
    else                -> "%,.0f€".format(amount).replace(",", " ")
}

// Returns the year at which the scenario passes the double-invested threshold
fun doubleYear(points: List<SimPoint>, invested: Double): Int? =
    points.firstOrNull { it.nominal >= invested * 2 }?.year
