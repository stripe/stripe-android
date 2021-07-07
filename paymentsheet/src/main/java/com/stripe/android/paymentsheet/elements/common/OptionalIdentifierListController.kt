package com.stripe.android.paymentsheet.elements.common

import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow

internal interface OptionalIdentifierListController {
    val optionalIdentifiers: Flow<Set<IdentifierSpec>>
}
