package com.stripe.android

import android.content.Context

internal class ApiFingerprintParamsFactory constructor(
    private val store: ClientFingerprintDataStore
) {
    constructor(context: Context) : this(
        store = ClientFingerprintDataStore.Default(context)
    )

    fun createParams(guid: String?): Map<String, String> {
        return mapOf(FIELD_MUID to store.getMuid())
            .plus(
                guid?.let {
                    mapOf(FIELD_GUID to it)
                }.orEmpty()
            )
    }

    private companion object {
        private const val FIELD_MUID = "muid"
        private const val FIELD_GUID = "guid"
    }
}
