package com.stripe.android

import android.content.Context

internal class UidParamsFactory constructor(
    private val packageName: String,
    private val uidSupplier: Supplier<StripeUid>
) {
    constructor(context: Context) : this(
        packageName = context.packageName,
        uidSupplier = UidSupplier(context)
    )

    fun createParams(): Map<String, String> {
        val guid = uidSupplier.get().value

        return if (guid.isNotBlank()) {
            createGuid(guid).plus(createMuid(guid))
        } else {
            emptyMap()
        }
    }

    private fun createGuid(guid: String): Map<String, String> {
        val hashGuid = StripeTextUtils.shaHashInput(guid)
        return if (hashGuid?.isNotBlank() == true) {
            mapOf(FIELD_GUID to hashGuid)
        } else {
            emptyMap()
        }
    }

    private fun createMuid(guid: String): Map<String, String> {
        val muid = packageName + guid
        val hashMuid = StripeTextUtils.shaHashInput(muid)
        return if (hashMuid?.isNotBlank() == true) {
            mapOf(FIELD_MUID to hashMuid)
        } else {
            emptyMap()
        }
    }

    private companion object {
        private const val FIELD_MUID = "muid"
        private const val FIELD_GUID = "guid"
    }
}
