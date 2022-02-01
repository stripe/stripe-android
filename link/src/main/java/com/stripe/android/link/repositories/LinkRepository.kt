package com.stripe.android.link.repositories

import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup

/**
 * Interface for a repository that interacts with Link services.
 */
internal interface LinkRepository {

    /**
     * Check if the email already has a link account.
     */
    suspend fun lookupConsumer(
        email: String
    ): ConsumerSessionLookup?

    /**
     * Sign up for a new Link account
     */
    suspend fun consumerSignUp(
        email: String,
        phone: String,
        country: String
    ): ConsumerSession?
}
