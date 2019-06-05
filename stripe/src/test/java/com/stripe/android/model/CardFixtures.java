package com.stripe.android.model;

public final class CardFixtures {
    public static final Card MINIMUM_CARD =
            Card.create("4242424242424242", 1, 2050, "123");


    private static final String CARD_ADDRESS_L1 = "123 Main Street";
    private static final String CARD_ADDRESS_L2 = "906";
    private static final String CARD_CITY = "San Francisco";
    private static final String CARD_COUNTRY = "US";
    private static final String CARD_CURRENCY = "USD";
    private static final String CARD_CVC = "123";
    private static final String CARD_NAME = "J Q Public";
    private static final String CARD_NUMBER = "4242424242424242";
    private static final String CARD_STATE = "CA";
    private static final String CARD_ZIP = "94107";

    public static final Card CARD = new Card.Builder(CARD_NUMBER, 8, 2019, CARD_CVC)
                .addressCity(CARD_CITY)
                .addressLine1(CARD_ADDRESS_L1)
                .addressLine2(CARD_ADDRESS_L2)
                .addressCountry(CARD_COUNTRY)
                .addressState(CARD_STATE)
                .addressZip(CARD_ZIP)
                .currency(CARD_CURRENCY)
                .name(CARD_NAME)
                .build();

    private CardFixtures() {}
}
