package com.propentatech.waka.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage chiffré (Keystore Android via [EncryptedSharedPreferences]) du hash de PIN et de l'état
 * de verrouillage. Un seul code PIN protège l'ensemble des projets marqués privés.
 */
class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "waka_security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isPinSet(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val hashed = PinHasher.hash(pin)
        prefs.edit()
            .putString(KEY_HASH, hashed.hash)
            .putString(KEY_SALT, hashed.salt)
            .apply()
        resetFailedAttempts()
    }

    fun clearPin() {
        prefs.edit().remove(KEY_HASH).remove(KEY_SALT).apply()
        resetFailedAttempts()
        setBiometricEnabled(false)
    }

    fun verifyPin(pin: String): Boolean {
        val hash = prefs.getString(KEY_HASH, null) ?: return false
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        return PinHasher.verify(pin, hash, salt)
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun recordFailedAttempt(): LockoutState {
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
        if (attempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            editor.putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
            editor.apply()
            return LockoutState.LockedOut(lockoutUntil)
        }
        editor.apply()
        return LockoutState.AttemptsRemaining(MAX_ATTEMPTS_BEFORE_LOCKOUT - attempts)
    }

    fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    fun currentLockout(): LockoutState.LockedOut? {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (lockoutUntil <= System.currentTimeMillis()) return null
        return LockoutState.LockedOut(lockoutUntil)
    }

    sealed interface LockoutState {
        data class AttemptsRemaining(val remaining: Int) : LockoutState
        data class LockedOut(val untilMillis: Long) : LockoutState
    }

    private companion object {
        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_LOCKOUT_UNTIL = "lockout_until"
        const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
        const val LOCKOUT_DURATION_MS = 60_000L
    }
}
