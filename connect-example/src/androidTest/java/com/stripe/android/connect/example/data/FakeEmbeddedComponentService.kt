package com.stripe.android.connect.example.data

import com.stripe.android.connect.example.core.Async
import com.stripe.android.connect.example.core.Success
import com.stripe.android.connect.example.core.Uninitialized
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeEmbeddedComponentService @Inject constructor() : EmbeddedComponentService {
    override var serverBaseUrl: String = ""
        private set

    override val publishableKey: MutableStateFlow<String?> = MutableStateFlow(null)

    override val accounts: MutableStateFlow<Async<List<Merchant>>> = MutableStateFlow(Uninitialized)

    override fun setBackendBaseUrl(url: String) {
        serverBaseUrl = url
    }

    override suspend fun getAccounts(): GetAccountsResponse =
        coroutineScope {
            val publishableKey = async { loadPublishableKey() }
            val availableMerchants = async { accounts.filterIsInstance<Success<List<Merchant>>>().first() }
            GetAccountsResponse(
                publishableKey = publishableKey.await(),
                availableMerchants = availableMerchants.await().value
            )
        }

    override suspend fun loadPublishableKey(): String {
        return publishableKey.filterNotNull().first()
    }

    override suspend fun fetchClientSecret(account: String): String {
        return "secret"
    }
}
