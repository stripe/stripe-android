package com.stripe.android.testing

import com.stripe.android.model.Customer

object CustomerFactory {
    fun create(
        id: String? = "cus_test",
        email: String? = null,
    ): Customer {
        return Customer(
            id = id,
            defaultSource = null,
            shippingInformation = null,
            sources = emptyList(),
            hasMore = false,
            totalCount = null,
            url = null,
            description = null,
            email = email,
            liveMode = false,
        )
    }
}
