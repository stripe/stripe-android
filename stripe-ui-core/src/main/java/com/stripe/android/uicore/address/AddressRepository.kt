package com.stripe.android.uicore.address

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.injection.IOContext
import com.stripe.android.uicore.elements.SectionFieldElement
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Repository to save country and their corresponding List<SectionFieldElement>.
 *
 * Note: this repository is mutable and stateful. The address information saved within the Element
 * list will carry over to other screens.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressRepository @Inject constructor(
    resources: Resources,
    @IOContext private val workContext: CoroutineContext,
) : AddressSchemaRepository(resources) {
    private val countryFieldMap: Deferred<MutableMap<String, List<SectionFieldElement>>>

    init {
        val completableDeferred = CompletableDeferred<MutableMap<String, List<SectionFieldElement>>>()
        countryFieldMap = completableDeferred
        CoroutineScope(workContext).launch {
            val map = countryAddressSchemaMap.entries.associate { (countryCode, schemaList) ->
                countryCode to requireNotNull(
                    schemaList
                        .transformToElementList(countryCode)
                )
            }.toMutableMap()
            completableDeferred.complete(map)
        }
    }

    @VisibleForTesting
    suspend fun add(
        countryCode: String,
        listElements: List<SectionFieldElement>
    ): Unit = withContext(workContext) {
        countryFieldMap.await()[countryCode] = listElements
    }

    suspend fun get(
        countryCode: String?
    ): List<SectionFieldElement>? = withContext(workContext) {
        countryCode?.let {
            countryFieldMap.await()[it]
        } ?: countryFieldMap.await()[DEFAULT_COUNTRY_CODE]
    }
}
