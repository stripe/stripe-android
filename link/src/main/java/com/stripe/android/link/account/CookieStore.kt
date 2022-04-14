package com.stripe.android.link.account

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent cookies storage.
 */
@Singleton
internal class CookieStore @Inject constructor(
    private val store: EncryptedStore
) {
    /**
     * Update authentication session cookie according to the following rules:
     *
     * +-----------------------------+---------+
     * |  cookie                     | Action  |
     * +-----------------------------+---------+
     * |  null                       | No-op   |
     * |-----------------------------|---------|
     * |  Empty (zero-length) string | Delete  |
     * |-----------------------------|---------|
     * |  Any other value            | Store   |
     * +-----------------------------+---------+
     */
    fun updateAuthSessionCookie(cookie: String?) = cookie?.let {
        if (it.isEmpty()) {
            store.delete(AUTH_SESSION_COOKIE)
        } else {
            store.write(AUTH_SESSION_COOKIE, it)
        }
    }

    /**
     * Retrieve and return the current authentication session cookie.
     */
    fun getAuthSessionCookie() = store.read(AUTH_SESSION_COOKIE)

    /**
     * Delete the current authentication session cookie and store the hash of the email so that the
     * user won't be automatically redirected to the verification screen next time.
     */
    fun logout(email: String) {
        storeLoggedOutEmail(email)
        store.delete(AUTH_SESSION_COOKIE)
    }

    /**
     * Check whether this is the most recently logged out email.
     */
    fun isEmailLoggedOut(email: String) =
        store.read(LOGGED_OUT_EMAIL_HASH) == email.sha256()

    fun storeLoggedOutEmail(email: String) =
        store.write(LOGGED_OUT_EMAIL_HASH, email.sha256())

    companion object {
        const val AUTH_SESSION_COOKIE = "auth_session_cookie"
        const val LOGGED_OUT_EMAIL_HASH = "logged_out_email_hash"
    }

    private fun String.sha256(): String =
        MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}
