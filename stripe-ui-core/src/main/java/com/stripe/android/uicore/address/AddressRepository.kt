package com.stripe.android.uicore.address

import android.content.res.Resources
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.IOContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Repository to asynchronously load country address schemas.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AddressRepository @Inject constructor(
    resources: Resources,
    @IOContext private val workContext: CoroutineContext,
) : AddressSchemaRepository(resources) {
    suspend fun load(): AddressSchemas {
        return with(workContext) {
            AddressSchemas(
                schemaMap = countryAddressSchemaMap,
                defaultCountryCode = DEFAULT_COUNTRY_CODE
            )
        }
    }
}
