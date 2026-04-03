package com.stripe.android.core.model

import androidx.annotation.RestrictTo
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Locale.getCountryCode(): CountryCode = CountryCode.create(this.country)
