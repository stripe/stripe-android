package com.stripe.android.link.account

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.CookieStore.Companion.AUTH_SESSION_COOKIE
import com.stripe.android.link.account.CookieStore.Companion.LOGGED_OUT_EMAIL_HASH
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CookieStoreTest {
    private val store = mock<EncryptedStore>()

    @Test
    fun `updateAuthSessionCookie with null value does nothing`() {
        val cookieStore = createCookieStore()
        cookieStore.updateAuthSessionCookie(null)

        verify(store, never()).write(eq(AUTH_SESSION_COOKIE), anyOrNull())
        verify(store, never()).delete(eq(AUTH_SESSION_COOKIE))
    }

    @Test
    fun `updateAuthSessionCookie with empty value deletes value`() {
        val cookieStore = createCookieStore()
        val cookie = ""
        cookieStore.updateAuthSessionCookie(cookie)

        verify(store).delete(eq(AUTH_SESSION_COOKIE))
    }

    @Test
    fun `updateAuthSessionCookie with non-empty value stores value`() {
        val cookieStore = createCookieStore()
        val cookie = "cookie"
        cookieStore.updateAuthSessionCookie(cookie)

        verify(store).write(eq(AUTH_SESSION_COOKIE), eq(cookie))
    }

    @Test
    fun `logout stores hashed email and clears cookie`() {
        val cookieStore = createCookieStore()
        val email = "test@stripe.com"
        cookieStore.logout(email)

        verify(store).write(
            eq(LOGGED_OUT_EMAIL_HASH),
            eq("49644df5404ea8ee8f0ec46cdb1dd7756c5661d6387fd1705a072f2fbf020f48")
        )
        verify(store).delete(eq(AUTH_SESSION_COOKIE))
    }

    @Test
    fun `isEmailLoggedOut checks for hashed email`() {
        val cookieStore = createCookieStore()
        val email = "test@stripe.com"

        whenever(store.read(eq(LOGGED_OUT_EMAIL_HASH)))
            .thenReturn("49644df5404ea8ee8f0ec46cdb1dd7756c5661d6387fd1705a072f2fbf020f48")
        assertThat(cookieStore.isEmailLoggedOut(email)).isTrue()
    }

    private fun createCookieStore() = CookieStore(store)
}
