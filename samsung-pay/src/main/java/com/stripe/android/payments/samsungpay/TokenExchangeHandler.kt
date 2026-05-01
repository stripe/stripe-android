package com.stripe.android.payments.samsungpay

/**
 * Handler for exchanging a Samsung Pay credential for a Stripe token.
 *
 * Implement this interface to call your merchant server, which should:
 * 1. Receive the [SamsungPayTokenRequest] fields
 * 2. Call `POST /v1/tokens` with your secret key and the Samsung Pay credential data
 * 3. Return the resulting Stripe token ID (e.g., `"tok_xxx"`)
 *
 * Your server must have the `can_set_samsung_pay_tokenization_method` gate enabled.
 *
 * Example server-side request:
 * ```
 * POST /v1/tokens
 * card[number]               = <DPAN>
 * card[exp_month]            = <expiry month>
 * card[exp_year]             = <expiry year>
 * card[tokenization_method]  = samsung_pay
 * card[cryptogram]           = <base64 cryptogram from SamsungPayTokenRequest.cryptogram>
 * ```
 */
fun interface TokenExchangeHandler {
    /**
     * Exchange a Samsung Pay credential for a Stripe token ID.
     *
     * This is called on a background thread after the user approves payment
     * on the Samsung Pay sheet.
     *
     * @param request The parsed Samsung Pay credential fields.
     * @return A Stripe token ID (e.g., `"tok_xxx"`).
     * @throws Exception if the exchange fails. The error will be wrapped in
     *   [SamsungPayException] and delivered as [SamsungPayLauncher.Result.Failed].
     */
    suspend fun exchangeForToken(request: SamsungPayTokenRequest): String
}
