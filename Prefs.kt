package com.ss.client

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** All settings live in local storage. Reads are Compose-observable. */
object Prefs {
    private var sp: SharedPreferences? = null
    private var secure: SharedPreferences? = null
    private val cache = mutableStateMapOf<String, String>()

    /** Session-only: true after a successful "Test Connection". Never persisted. */
    var aiConnected by mutableStateOf(false)

    @Synchronized
    fun init(c: Context) {
        if (sp != null) return
        val p = c.getSharedPreferences("ss_prefs", Context.MODE_PRIVATE)
        sp = p
        p.all.forEach { (k, v) -> if (v != null) cache[k] = v.toString() }
        secure = try {
            val mk = MasterKey.Builder(c).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                c, "ss_secure", mk,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            null
        }
    }

    fun has(k: String) = cache.containsKey(k)
    fun str(k: String, d: String = ""): String = cache[k] ?: d
    fun int(k: String, d: Int = 0): Int = cache[k]?.toIntOrNull() ?: d
    fun bool(k: String, d: Boolean = false): Boolean = cache[k]?.toBooleanStrictOrNull() ?: d

    fun put(k: String, v: Any) {
        cache[k] = v.toString()
        val e = sp?.edit() ?: return
        when (v) {
            is Int -> e.putInt(k, v)
            is Boolean -> e.putBoolean(k, v)
            else -> e.putString(k, v.toString())
        }
        e.apply()
    }

    fun remove(k: String) {
        cache.remove(k)
        sp?.edit()?.remove(k)?.apply()
    }

    fun list(k: String): List<String> = try {
        val a = JSONArray(str(k, "[]"))
        List(a.length()) { a.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }

    fun putList(k: String, l: List<String>) = put(k, JSONArray(l).toString())

    // ---- API key: only ever stored in EncryptedSharedPreferences, never in source ----
    fun apiKey(): String = secure?.getString("api_key", "") ?: ""
    fun setApiKey(k: String): Boolean {
        val s = secure ?: return false
        s.edit().putString("api_key", k).apply()
        return true
    }

    val language: String get() = str("lang", "en")
    val accent: Int get() = int("accent", ACCENTS[0])

    // ---- AI provider settings ----
    fun providerDefaultEndpoint(p: String) = if (p == "grok") "https://api.x.ai/v1" else ""
    fun providerDefaultModel(p: String) = if (p == "grok") "grok-4" else ""
    val aiProvider: String get() = str("ai_provider", "grok")
    fun aiEndpoint() = str("ai_endpoint_$aiProvider", providerDefaultEndpoint(aiProvider))
    fun aiModel() = str("ai_model_$aiProvider", providerDefaultModel(aiProvider))
}

fun t(en: String, bn: String): String = if (Prefs.language == "bn") bn else en

/** Local-only account. Password is salted + PBKDF2-hashed, never stored as plain text. */
object Account {
    val user: String get() = Prefs.str("acct_user")
    val signedIn: Boolean get() = Prefs.bool("acct_in") && user.isNotEmpty()

    private fun hash(p: String, salt: ByteArray): String {
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val h = f.generateSecret(PBEKeySpec(p.toCharArray(), salt, 120_000, 256)).encoded
        return Base64.encodeToString(h, Base64.NO_WRAP)
    }

    /** Returns an error message, or null on success. */
    fun create(u: String, p: String): String? {
        val name = u.trim()
        if (name.length < 3) return t("Username must be at least 3 characters.", "ইউজারনেম কমপক্ষে ৩ অক্ষরের হতে হবে।")
        if (p.length < 8) return t("Password must be at least 8 characters.", "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।")
        if (Prefs.str("acct_user").isNotEmpty()) return t("An account already exists on this device. Sign in instead.", "এই ডিভাইসে একটি অ্যাকাউন্ট আছে। সাইন ইন করুন।")
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        Prefs.put("acct_user", name)
        Prefs.put("acct_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
        Prefs.put("acct_hash", hash(p, salt))
        Prefs.put("acct_in", true)
        return null
    }

    fun signIn(u: String, p: String): String? {
        val saltB64 = Prefs.str("acct_salt")
        if (Prefs.str("acct_user").isEmpty() || saltB64.isEmpty()) return t("No account found. Create one first.", "কোনো অ্যাকাউন্ট নেই। আগে তৈরি করুন।")
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val ok = u.trim() == Prefs.str("acct_user") &&
            MessageDigest.isEqual(hash(p, salt).toByteArray(), Prefs.str("acct_hash").toByteArray())
        if (!ok) return t("Wrong username or password.", "ইউজারনেম বা পাসওয়ার্ড ভুল।")
        Prefs.put("acct_in", true)
        return null
    }

    fun signOut() = Prefs.put("acct_in", false)
}
