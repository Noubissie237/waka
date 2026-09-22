package com.propentatech.waka.model

enum class Currency {
    EUR,
    XAF,
}

/** Taux fixe volontairement non modifiable : 1 EUR = 655 XAF. */
const val EUR_TO_XAF_RATE = 655.0

fun Double.convert(from: Currency, to: Currency): Double = when {
    from == to -> this
    from == Currency.EUR && to == Currency.XAF -> this * EUR_TO_XAF_RATE
    from == Currency.XAF && to == Currency.EUR -> this / EUR_TO_XAF_RATE
    else -> this
}
