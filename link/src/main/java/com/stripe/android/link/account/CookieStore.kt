package com.stripe.android.link.account

import android.content.Context
import androidx.annotation.RestrictTo
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent cookies storage.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CookieStore @Inject internal constructor(
    private val store: EncryptedStore
) {

    constructor(context: Context): this(EncryptedStore(context))

    /**
     * Clear all local data.
     */
    fun clear() {
        allCookies.forEach { store.delete(it) }
    }

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
    internal fun updateAuthSessionCookie(cookie: String?) = cookie?.let {
        if (it.isEmpty()) {
            store.delete(AUTH_SESSION_COOKIE)
        } else {
            store.write(AUTH_SESSION_COOKIE, it)
        }
    }

    /**
     * Retrieve and return the current authentication session cookie.
     */
    internal fun getAuthSessionCookie() = store.read(AUTH_SESSION_COOKIE)

    /**
     * Delete the current authentication session cookie and store the hash of the email so that the
     * user won't be automatically redirected to the verification screen next time.
     */
    internal fun logout(email: String) {
        storeLoggedOutEmail(email)
        store.delete(AUTH_SESSION_COOKIE)
    }

    /**
     * Check whether this is the most recently logged out email.
     */
    internal fun isEmailLoggedOut(email: String) =
        store.read(LOGGED_OUT_EMAIL_HASH) == email.sha256()

    internal fun storeLoggedOutEmail(email: String) =
        store.write(LOGGED_OUT_EMAIL_HASH, email.sha256())

    /**
     * Store the email that has recently signed up on this device so that the user is remembered.
     */
    internal fun storeNewUserEmail(email: String) =
        store.write(SIGNED_UP_EMAIL, email)

    /**
     * Retrieve the email that has recently signed up on this device.
     */
    internal fun getNewUserEmail() =
        store.read(SIGNED_UP_EMAIL).also {
            store.delete(SIGNED_UP_EMAIL)
        }

    internal companion object {
        const val AUTH_SESSION_COOKIE = "auth_session_cookie"
        const val LOGGED_OUT_EMAIL_HASH = "logged_out_email_hash"
        const val SIGNED_UP_EMAIL = "signed_up_email"

        val allCookies = arrayOf(
            AUTH_SESSION_COOKIE,
            LOGGED_OUT_EMAIL_HASH,
            SIGNED_UP_EMAIL
        )
    }

    private fun String.sha256(): String =
        MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}
