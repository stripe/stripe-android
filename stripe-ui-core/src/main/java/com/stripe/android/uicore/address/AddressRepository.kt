package com.stripe.android.uicore.address

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.uicore.elements.SectionFieldElement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository to save country and their corresponding List<SectionFieldElement>.
 *
 * Note: this repository is mutable and stateful. The address information saved within the Element
 * list will carry over to other screens.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressRepository @Inject constructor(
    resources: Resources?
) : AddressSchemaRepository(resources) {
    private val countryFieldMap =
        countryAddressSchemaMap.entries.associate { (countryCode, schemaList) ->
            countryCode to requireNotNull(
                schemaList
                    .transformToElementList(countryCode)
            )
        }.toMutableMap()

    @VisibleForTesting
    fun add(countryCode: String, listElements: List<SectionFieldElement>) {
        countryFieldMap[countryCode] = listElements
    }

    fun get(countryCode: String?) = countryCode?.let {
        countryFieldMap[it]
    } ?: countryFieldMap[DEFAULT_COUNTRY_CODE]
}
