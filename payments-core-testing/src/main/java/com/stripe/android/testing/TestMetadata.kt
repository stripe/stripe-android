package com.stripe.android.testing

import androidx.compose.ui.test.SemanticsMatcher
import com.stripe.android.paymentsheet.utils.TestMetadata

fun hasTestMetadata(testMetadata: String?): SemanticsMatcher =
    SemanticsMatcher.expectValue(TestMetadata, testMetadata)
