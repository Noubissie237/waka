package com.propentatech.waka.ui.format

import com.propentatech.waka.model.Currency
import java.text.NumberFormat
import java.util.Locale

/** Formatage d'affichage : XAF sans décimales, EUR avec au plus deux décimales, séparateur de milliers français. */
fun formatMoney(amount: Double, currency: Currency): String {
    val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
    return when (currency) {
        Currency.EUR -> {
            formatter.maximumFractionDigits = 2
            formatter.minimumFractionDigits = 0
            "${formatter.format(amount)} €"
        }
        Currency.XAF -> {
            formatter.maximumFractionDigits = 0
            "${formatter.format(amount)} XAF"
        }
    }
}
