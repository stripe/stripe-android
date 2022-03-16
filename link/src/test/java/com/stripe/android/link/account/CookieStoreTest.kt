package com.stripe.android.link.account

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class CookieStoreTest {
    private val store = mock<EncryptedStore>()

    @Test
    fun `updateAuthSessionCookie with null value does nothing`() {
        val cookieStore = createCookieStore()
        cookieStore.updateAuthSessionCookie(null)

        verify(store, never()).write(any(), anyOrNull())
        verify(store, never()).delete(any())
    }

    @Test
    fun `updateAuthSessionCookie with empty value deletes value`() {
        val cookieStore = createCookieStore()
        val cookie = ""
        cookieStore.updateAuthSessionCookie(cookie)

        verify(store).delete(any())
    }

    @Test
    fun `updateAuthSessionCookie with non-empty value stores value`() {
        val cookieStore = createCookieStore()
        val cookie = "cookie"
        cookieStore.updateAuthSessionCookie(cookie)

        verify(store).write(any(), eq(cookie))
    }

    private fun createCookieStore() = CookieStore(store)
}
