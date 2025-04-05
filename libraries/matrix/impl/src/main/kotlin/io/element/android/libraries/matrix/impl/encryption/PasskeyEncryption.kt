package io.element.android.libraries.matrix.impl.encryption

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.util.Base64

/**
 * Utility class for encrypting and decrypting passkeys using passphrase as salt.
 */
object PasskeyEncryption {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATION_COUNT = 65536
    private const val IV_LENGTH = 16

    /**
     * Encrypts a passkey using the passphrase as salt.
     * @param passkey The passkey to encrypt
     * @param passphrase The passphrase to use as salt
     * @return The encrypted passkey as a Base64 encoded string
     */
    fun encrypt(passkey: String, passphrase: String): String {
        // Generate a random IV
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)

        // Generate key from passphrase
        val keySpec = PBEKeySpec(passphrase.toCharArray(), iv, ITERATION_COUNT, KEY_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val key = SecretKeySpec(keyFactory.generateSecret(keySpec).encoded, "AES")

        // Initialize cipher
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        // Encrypt the passkey
        val encrypted = cipher.doFinal(passkey.toByteArray())

        // Combine IV and encrypted data
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

        // Return Base64 encoded string
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts an encrypted passkey using the passphrase as salt.
     * @param encryptedPasskey The encrypted passkey as a Base64 encoded string
     * @param passphrase The passphrase to use as salt
     * @return The decrypted passkey
     */
    fun decrypt(encryptedPasskey: String, passphrase: String): String {
        // Decode Base64 string
        val combined = Base64.getDecoder().decode(encryptedPasskey)

        // Extract IV and encrypted data
        val iv = ByteArray(IV_LENGTH)
        val encrypted = ByteArray(combined.size - IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
        System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.size)

        // Generate key from passphrase
        val keySpec = PBEKeySpec(passphrase.toCharArray(), iv, ITERATION_COUNT, KEY_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val key = SecretKeySpec(keyFactory.generateSecret(keySpec).encoded, "AES")

        // Initialize cipher
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        // Decrypt the data
        val decrypted = cipher.doFinal(encrypted)

        // Return decrypted string
        return String(decrypted)
    }
} 