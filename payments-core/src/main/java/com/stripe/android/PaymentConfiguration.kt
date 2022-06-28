package com.stripe.android

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiKeyValidator
import kotlinx.parcelize.Parcelize

@Parcelize
data class PaymentConfiguration
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val publishableKey: String,
    val stripeAccountId: String? = null
) : Parcelable {

    init {
        ApiKeyValidator.get().requireValid(publishableKey)
    }

    /**
     * Manages saving and loading [PaymentConfiguration] data to SharedPreferences.
     */
    private class Store(context: Context) {
        private val prefs: SharedPreferences =
            context.applicationContext.getSharedPreferences(NAME, 0)

        @JvmSynthetic
        fun save(
            publishableKey: String,
            stripeAccountId: String?
        ) {
            prefs.edit()
                .putString(KEY_PUBLISHABLE_KEY, publishableKey)
                .putString(KEY_ACCOUNT_ID, stripeAccountId)
                .apply()
        }

        @JvmSynthetic
        internal fun load(): PaymentConfiguration? {
            return prefs.getString(KEY_PUBLISHABLE_KEY, null)?.let { publishableKey ->
                PaymentConfiguration(
                    publishableKey = publishableKey,
                    stripeAccountId = prefs.getString(KEY_ACCOUNT_ID, null)
                )
            }
        }

        private companion object {
            private val NAME = PaymentConfiguration::class.java.canonicalName

            private const val KEY_PUBLISHABLE_KEY = "key_publishable_key"
            private const val KEY_ACCOUNT_ID = "key_account_id"
        }
    }

    companion object {
        private var instance: PaymentConfiguration? = null

        /**
         * Attempts to load a [PaymentConfiguration] instance. First attempt to use the class's
         * singleton instance. If unavailable, attempt to load from [Store].
         *
         * @param context application context
         * @return a [PaymentConfiguration] instance, or throw an exception
         */
        @JvmStatic
        fun getInstance(context: Context): PaymentConfiguration {
            return instance ?: loadInstance(context)
        }

        private fun loadInstance(context: Context): PaymentConfiguration {
            return Store(context).load()?.let {
                instance = it
                it
            }
                ?: throw IllegalStateException(
                    "PaymentConfiguration was not initialized. Call PaymentConfiguration.init()."
                )
        }

        /**
         * A publishable key from the Dashboard's [API keys](https://dashboard.stripe.com/apikeys) page.
         */
        @JvmStatic
        @JvmOverloads
        fun init(
            context: Context,
            publishableKey: String,
            stripeAccountId: String? = null
        ) {
            instance = PaymentConfiguration(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId
            )
            Store(context)
                .save(
                    publishableKey = publishableKey,
                    stripeAccountId = stripeAccountId
                )

            DefaultFraudDetectionDataRepository(context).refresh()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        // for paymentsheet
        @JvmSynthetic
        fun clearInstance() {
            instance = null
        }
    }
}
