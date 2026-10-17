package com.stripe.android.model

@OptIn(DelicateCardDetailsApi::class)
fun CardParams.copy(
    number: String = this.number,
    expMonth: Int = this.expMonth,
    expYear: Int = this.expYear,
    cvc: String? = this.cvc,
    name: String? = this.name,
    address: Address? = this.address,
    currency: String? = this.currency,
    metadata: Map<String, String>? = this.metadata,
    loggingTokens: Set<String> = this.attribution
): CardParams {
    return CardParams(
        brand = this.brand,
        loggingTokens = loggingTokens,
        number = number,
        expMonth = expMonth,
        expYear = expYear,
        cvc = cvc,
        name = name,
        address = address,
        currency = currency,
        networks = this.networks,
        metadata = metadata
    )
}
