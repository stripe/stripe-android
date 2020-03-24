package com.stripe.android

import android.content.Context

internal class UidParamsFactory constructor(
    private val store: ClientFingerprintDataStore,
    private val uidSupplier: Supplier<StripeUid>
) {
    constructor(context: Context) : this(
        store = ClientFingerprintDataStore.Default(context),
        uidSupplier = UidSupplier(context)
    )

    fun createParams(): Map<String, String> {
        return mapOf(FIELD_MUID to store.getMuid())
            .plus(
                guid?.let {
                    mapOf(FIELD_GUID to it)
                }.orEmpty()
            )
    }

    private val guid: String?
        get() {
            return StripeTextUtils.shaHashInput(uidSupplier.get().value).takeUnless {
                it.isNullOrBlank()
            }
        }

    private companion object {
        private const val FIELD_MUID = "muid"
        private const val FIELD_GUID = "guid"
    }
}
