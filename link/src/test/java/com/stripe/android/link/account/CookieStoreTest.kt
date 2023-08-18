package com.stripe.android.link.account

import com.stripe.android.link.account.CookieStore.Companion.SIGNED_UP_EMAIL
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class CookieStoreTest {
    private val store = mock<EncryptedStore>()

    @Test
    fun `clear deletes all data`() {
        createCookieStore().clear()

        verify(store).delete(SIGNED_UP_EMAIL)
    }

    @Test
    fun `logout clears signed up email`() {
        val cookieStore = createCookieStore()
        cookieStore.logout()

        verify(store).delete(eq(SIGNED_UP_EMAIL))
    }

    @Test
    fun `storeNewUserEmail stores email`() {
        val cookieStore = createCookieStore()
        val email = "test@stripe.com"
        cookieStore.storeNewUserEmail(email)

        verify(store).write(eq(SIGNED_UP_EMAIL), eq(email))
    }

    @Test
    fun `getNewUserEmail returns saved email and deletes it`() {
        val cookieStore = createCookieStore()
        cookieStore.getNewUserEmail()

        verify(store).read(eq(SIGNED_UP_EMAIL))
        verify(store).delete(eq(SIGNED_UP_EMAIL))
    }

    private fun createCookieStore() = CookieStore(store)
}
