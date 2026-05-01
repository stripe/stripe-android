package com.stripe.android.payments.samsungpay

internal class FakeTokenExchangeHandler : TokenExchangeHandler {
    var tokenToReturn: String = "tok_fake_123"
    var errorToThrow: Exception? = null
    var lastRequest: SamsungPayTokenRequest? = null
    var callCount: Int = 0
        private set

    override suspend fun exchangeForToken(request: SamsungPayTokenRequest): String {
        callCount++
        lastRequest = request
        errorToThrow?.let { throw it }
        return tokenToReturn
    }
}
