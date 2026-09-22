package com.propentatech.waka.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.propentatech.waka.model.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "waka_prefs")

/** Préférences non sensibles : la devise d'affichage par défaut, basculable partout dans l'app. */
class AppPreferences(private val context: Context) {

    val defaultDisplayCurrency: Flow<Currency> = context.dataStore.data.map { prefs ->
        prefs[KEY_DISPLAY_CURRENCY]?.let { runCatching { Currency.valueOf(it) }.getOrNull() }
            ?: Currency.EUR
    }

    suspend fun setDefaultDisplayCurrency(currency: Currency) {
        context.dataStore.edit { it[KEY_DISPLAY_CURRENCY] = currency.name }
    }

    private companion object {
        val KEY_DISPLAY_CURRENCY = stringPreferencesKey("default_display_currency")
    }
}
