package com.stripe.android.model

internal object CardFixtures {
    @JvmField
    val MINIMUM_CARD = Card.create("4242424242424242", 1, 2050, "123")

    private const val CARD_ADDRESS_L1 = "123 Main Street"
    private const val CARD_ADDRESS_L2 = "906"
    private const val CARD_CITY = "San Francisco"
    private const val CARD_COUNTRY = "US"
    private const val CARD_CURRENCY = "USD"
    private const val CARD_CVC = "123"
    private const val CARD_NAME = "J Q Public"
    private const val CARD_NUMBER = "4242424242424242"
    private const val CARD_STATE = "CA"
    private const val CARD_ZIP = "94107"

    @JvmField
    val CARD = Card.Builder(CARD_NUMBER, 8, 2019, CARD_CVC)
        .addressCity(CARD_CITY)
        .addressLine1(CARD_ADDRESS_L1)
        .addressLine2(CARD_ADDRESS_L2)
        .addressCountry(CARD_COUNTRY)
        .addressState(CARD_STATE)
        .addressZip(CARD_ZIP)
        .currency(CARD_CURRENCY)
        .name(CARD_NAME)
        .build()
}
