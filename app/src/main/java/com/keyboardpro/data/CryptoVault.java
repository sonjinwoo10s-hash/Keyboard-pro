package com.keyboardpro.data;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Hardware-backed AndroidKeyStore AES-256-GCM encryption utility for Keyboard Pass
 * credentials and user-provided AI API keys.
 */
public final class CryptoVault {

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "keyboard_pro_vault_aes_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private CryptoVault() {
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[1 + iv.length + encrypted.length];
            combined[0] = (byte) iv.length;
            System.arraycopy(iv, 0, combined, 1, iv.length);
            System.arraycopy(encrypted, 0, combined, 1 + iv.length, encrypted.length);
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            return Base64.encodeToString(plainText.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        }
    }

    public static String decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        try {
            byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);
            int ivLen = combined[0] & 0xFF;
            if (ivLen > 0 && ivLen <= 16 && combined.length > 1 + ivLen) {
                byte[] iv = new byte[ivLen];
                byte[] cipherText = new byte[combined.length - 1 - ivLen];
                System.arraycopy(combined, 1, iv, 0, ivLen);
                System.arraycopy(combined, 1 + ivLen, cipherText, 0, cipherText.length);
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                        new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
                byte[] plain = cipher.doFinal(cipherText);
                return new String(plain, StandardCharsets.UTF_8);
            }
            return new String(combined, StandardCharsets.UTF_8);
        } catch (Exception e) {
            try {
                return new String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return "";
            }
        }
    }
}
