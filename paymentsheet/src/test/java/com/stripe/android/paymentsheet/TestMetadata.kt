package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsMatcher
import com.stripe.android.paymentsheet.utils.TestMetadata

fun hasTestMetadata(testMetadata: String?): SemanticsMatcher =
    SemanticsMatcher.expectValue(TestMetadata, testMetadata)
