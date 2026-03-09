package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Address {
    private var city: String? = null
    private var country: String? = null
    private var line1: String? = null
    private var line2: String? = null
    private var postalCode: String? = null
    private var state: String? = null

    fun city(city: String?) = apply {
        this.city = city
    }

    fun country(country: String) = apply {
        this.country = country
    }

    fun line1(line1: String?) = apply {
        this.line1 = line1
    }

    fun line2(line2: String?) = apply {
        this.line2 = line2
    }

    fun postalCode(postalCode: String?) = apply {
        this.postalCode = postalCode
    }

    fun state(state: String?) = apply {
        this.state = state
    }

    internal class State(
        val city: String?,
        val country: String?,
        val line1: String?,
        val line2: String?,
        val postalCode: String?,
        val state: String?,
    )

    internal fun build(): State {
        return State(
            city = city?.trim(),
            country = requireNotNull(country?.trim()) {
                "Country is required."
            },
            line1 = line1?.trim(),
            line2 = line2?.trim(),
            postalCode = postalCode?.trim(),
            state = state?.trim(),
        )
    }
}
