package com.stripe.android.paymentsheet.utils

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

@Stable
internal fun Modifier.testMetadata(metadata: String?) = semantics(
    properties = {
        testMetadata = metadata
    }
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val TestMetadata = SemanticsPropertyKey<String?>(
    name = "TestMetadata",
    mergePolicy = { parentValue, _ ->
        // Never merge TestMetadata, to avoid leaking internal test tags to parents.
        parentValue
    }
)

internal var SemanticsPropertyReceiver.testMetadata by TestMetadata
