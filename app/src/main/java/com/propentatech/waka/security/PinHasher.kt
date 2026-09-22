package com.propentatech.waka.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hache le code PIN avec PBKDF2 + sel aléatoire : seul le hash est stocké, jamais le PIN.
 * Le sel diffère à chaque appel de [hash], donc deux hachages du même PIN ne sont jamais égaux —
 * c'est [verify] qu'il faut utiliser pour comparer, jamais l'égalité directe.
 */
object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val SALT_BYTES = 16

    data class Hashed(val hash: String, val salt: String)

    fun hash(pin: String): Hashed {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        return Hashed(
            hash = Base64.encodeToString(hash, Base64.NO_WRAP),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
        )
    }

    fun verify(pin: String, storedHash: String, storedSalt: String): Boolean {
        val salt = Base64.decode(storedSalt, Base64.NO_WRAP)
        val candidate = derive(pin, salt)
        val expected = Base64.decode(storedHash, Base64.NO_WRAP)
        return candidate.contentEquals(expected)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }
}
