package com.stripe.android.common.taptoadd

import com.stripe.android.core.strings.ResolvableString

internal data class TapToAddErrorMessage(
    val title: ResolvableString,
    val action: ResolvableString,
)
