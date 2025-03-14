package com.stripe.android.connect.example.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeEmbeddedComponentService @Inject constructor() : EmbeddedComponentService {
    override var serverBaseUrl: String = ""
        private set

    override val publishableKey: MutableStateFlow<String?> = MutableStateFlow(null)

    override val accounts: MutableStateFlow<List<Merchant>?> = MutableStateFlow(null)

    override fun setBackendBaseUrl(url: String) {
        serverBaseUrl = url
    }

    override suspend fun getAccounts(): GetAccountsResponse =
        coroutineScope {
            val publishableKey = async { loadPublishableKey() }
            val availableMerchants = async { accounts.filterNotNull().first() }
            GetAccountsResponse(
                publishableKey = publishableKey.await(),
                availableMerchants = availableMerchants.await(),
            )
        }

    override suspend fun loadPublishableKey(): String {
        return publishableKey.filterNotNull().first()
    }

    override suspend fun fetchClientSecret(account: String): String {
        return "secret"
    }
}
