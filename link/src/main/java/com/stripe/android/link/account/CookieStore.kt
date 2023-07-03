package com.stripe.android.link.account

import android.content.Context
import androidx.annotation.RestrictTo
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

    constructor(context: Context) : this(EncryptedStore(context))

    /**
     * Clear all local data.
     */
    fun clear() {
        allCookies.forEach { store.delete(it) }
    }

    /**
     * Delete the current authentication session cookie and store the hash of the email so that the
     * user won't be automatically redirected to the verification screen next time.
     */
    internal fun logout() {
        store.delete(SIGNED_UP_EMAIL)
    }

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
        const val SIGNED_UP_EMAIL = "signed_up_email"

        val allCookies = arrayOf(
            SIGNED_UP_EMAIL
        )
    }
}
