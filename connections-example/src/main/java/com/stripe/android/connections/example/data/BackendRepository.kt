package com.stripe.android.connections.example.data

import com.stripe.android.connections.example.data.model.CreateLinkAccountSessionBody
import com.stripe.android.connections.model.AccountHolder

// private const val BASE_URL = "https://desert-instinctive-eoraptor.glitch.me/"
private const val BASE_URL = "https://instinctive-bolder-creator.glitch.me/"

class BackendRepository(
    private val backendService: BackendApiService = BackendApiFactory(BASE_URL).create()
) {

    suspend fun createAccountHolder() = backendService.createAccountHolder()

    suspend fun createLinkAccountSession(
        accountHolder: AccountHolder? = null
    ) = backendService.createLinkAccountSession(
        CreateLinkAccountSessionBody(
            accountHolder
        )
    )
}
