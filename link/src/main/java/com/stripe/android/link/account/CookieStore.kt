package com.stripe.android.link.account

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

    private companion object {
        const val AUTH_SESSION_COOKIE = "auth_session_cookie"
    }
}
