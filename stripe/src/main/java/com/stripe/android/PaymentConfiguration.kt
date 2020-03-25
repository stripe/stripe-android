package com.stripe.android

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PaymentConfiguration internal constructor(val publishableKey: String) : Parcelable {
    init {
        ApiKeyValidator.get().requireValid(publishableKey)
    }

    /**
     * Manages saving and loading [PaymentConfiguration] data to SharedPreferences.
     */
    private class Store internal constructor(context: Context) {
        private val prefs: SharedPreferences =
            context.applicationContext.getSharedPreferences(NAME, 0)

        @JvmSynthetic
        internal fun save(publishableKey: String) {
            prefs.edit()
                .putString(KEY_PUBLISHABLE_KEY, publishableKey)
                .apply()
        }

        @JvmSynthetic
        internal fun load(): PaymentConfiguration? {
            return prefs.getString(KEY_PUBLISHABLE_KEY, null)?.let { publishableKey ->
                PaymentConfiguration(publishableKey)
            }
        }

        private companion object {
            private val NAME = PaymentConfiguration::class.java.canonicalName

            private const val KEY_PUBLISHABLE_KEY = "key_publishable_key"
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
        fun init(context: Context, publishableKey: String) {
            instance = PaymentConfiguration(publishableKey)
            Store(context).save(publishableKey)

            FingerprintDataRepository.Default(context).get()
        }

        @JvmSynthetic
        internal fun clearInstance() {
            instance = null
        }
    }
}
