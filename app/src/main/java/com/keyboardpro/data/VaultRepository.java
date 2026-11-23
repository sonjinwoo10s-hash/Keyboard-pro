package com.keyboardpro.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Encrypted local vault for Keyboard Pass (accounts, cards, IBANs, quick-type secrets).
 * Protected via AndroidKeyStore AES-GCM encryption and gated behind BiometricPrompt in UI.
 */
public class VaultRepository {

    public static class VaultItem {
        public final String id;
        public final String category; // account, card, iban, note
        public final String label;
        public final String usernameOrMeta;
        public final String secretValue;

        public VaultItem(String id, String category, String label, String usernameOrMeta, String secretValue) {
            this.id = id;
            this.category = category;
            this.label = label;
            this.usernameOrMeta = usernameOrMeta;
            this.secretValue = secretValue;
        }
    }

    private static final String PREFS_VAULT = "keyboard_pro_vault";
    private static final String KEY_ITEMS = "vault_items_enc";
    private static final String KEY_UNLOCK_UNTIL = "vault_unlock_until";

    private final SharedPreferences prefs;

    public VaultRepository(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_VAULT, Context.MODE_PRIVATE);
    }

    public List<VaultItem> getItems() {
        List<VaultItem> items = new ArrayList<>();
        String enc = prefs.getString(KEY_ITEMS, "");
        if (enc.isEmpty()) {
            return items;
        }
        String json = CryptoVault.decrypt(enc);
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                items.add(new VaultItem(
                        o.optString("id"),
                        o.optString("category", "account"),
                        o.optString("label"),
                        o.optString("meta"),
                        o.optString("secret")
                ));
            }
        } catch (Exception ignored) {
        }
        return items;
    }

    public void addItem(String category, String label, String meta, String secret) {
        List<VaultItem> items = getItems();
        items.add(0, new VaultItem(UUID.randomUUID().toString(), category, label, meta, secret));
        saveAll(items);
    }

    public void deleteItem(String id) {
        List<VaultItem> items = getItems();
        items.removeIf(item -> item.id.equals(id));
        saveAll(items);
    }

    private void saveAll(List<VaultItem> items) {
        JSONArray arr = new JSONArray();
        try {
            for (VaultItem item : items) {
                JSONObject o = new JSONObject();
                o.put("id", item.id);
                o.put("category", item.category);
                o.put("label", item.label);
                o.put("meta", item.usernameOrMeta);
                o.put("secret", item.secretValue);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        prefs.edit().putString(KEY_ITEMS, CryptoVault.encrypt(arr.toString())).apply();
    }

    /**
     * Marks Keyboard Pass unlocked for 2 minutes after successful BiometricPrompt authentication
     * so the user can auto-type saved credentials directly from the keyboard bar.
     */
    public void markSessionUnlocked(long durationMs) {
        prefs.edit().putLong(KEY_UNLOCK_UNTIL, System.currentTimeMillis() + durationMs).apply();
    }

    public boolean isSessionUnlocked() {
        return System.currentTimeMillis() < prefs.getLong(KEY_UNLOCK_UNTIL, 0L);
    }

    public void lockSession() {
        prefs.edit().putLong(KEY_UNLOCK_UNTIL, 0L).apply();
    }
}
