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
        store.clear()
    }

    /**
     * Delete the current authentication session cookie and store the hash of the email so that the
     * user won't be automatically redirected to the verification screen next time.
     */
    internal fun logout() {
        store.delete(SignedUpEmail)
    }

    /**
     * Store the email that has recently signed up on this device so that the user is remembered.
     */
    internal fun storeNewUserEmail(email: String) {
        store.write(SignedUpEmail, email)
    }

    /**
     * Retrieve the email that has recently signed up on this device.
     */
    internal fun getNewUserEmail(): String? {
        return store.read(SignedUpEmail).also {
            store.delete(SignedUpEmail)
        }
    }

    fun markLinkAsUsed() {
        store.write(HasUsedLink, true)
    }

    fun hasUsedLink(): Boolean {
        return store.read(HasUsedLink, defaultValue = false)
    }

    internal companion object {
        const val SignedUpEmail = "signed_up_email"
        const val HasUsedLink = "signed_up_email"
    }
}
