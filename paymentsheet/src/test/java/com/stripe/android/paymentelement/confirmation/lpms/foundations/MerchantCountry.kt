package com.stripe.android.paymentelement.confirmation.lpms.foundations

enum class MerchantCountry(val publishableKey: String) {
    US(MerchantCountryKeys.US_PUBLISHABLE_KEY),
    SG(MerchantCountryKeys.SG_PUBLISHABLE_KEY),
    MY(MerchantCountryKeys.MY_PUBLISHABLE_KEY),
    BE(MerchantCountryKeys.BE_PUBLISHABLE_KEY),
    GB(MerchantCountryKeys.GB_PUBLISHABLE_KEY),
    MX(MerchantCountryKeys.MX_PUBLISHABLE_KEY),
    AU(MerchantCountryKeys.AU_PUBLISHABLE_KEY),
    JP(MerchantCountryKeys.JP_PUBLISHABLE_KEY),
    BR(MerchantCountryKeys.BR_PUBLISHABLE_KEY),
    FR(MerchantCountryKeys.FR_PUBLISHABLE_KEY),
    TH(MerchantCountryKeys.TH_PUBLISHABLE_KEY),
    DE(MerchantCountryKeys.DE_PUBLISHABLE_KEY),
    IT(MerchantCountryKeys.IT_PUBLISHABLE_KEY);
}

private object MerchantCountryKeys {
    const val US_PUBLISHABLE_KEY =
        "pk_test_ErsyMEOTudSjQR8hh0VrQr5X008sBXGOu6"
    const val AU_PUBLISHABLE_KEY =
        "pk_test_GNmlCJ6AFgWXm4mJYiyWSOWN00KIIiri7F"
    const val MX_PUBLISHABLE_KEY =
        "pk_test_51GvAY5HNG4o8pO5lDEegY72rkF1TMiMyuTxSFJsmsH7U0KjTwmEf2VuXHVHecil64QA8za8Um2uSsFsfrG0BkzFo00sb1uhblF"
    const val SG_PUBLISHABLE_KEY =
        "pk_test_51H7oXMAOnZToJom1hqiSvNGsUVTrG1SaXRSBon9xcEp0yDFAxEh5biA4n0ty6paEsD5Mo5ps1b7Taj9WAHQzjup800m8A8Nc3u"
    const val BE_PUBLISHABLE_KEY =
        "pk_test_51HZi0VArGMi59tL4sIXUjwXbMiM5uSHVfsKjNXcepJ80C5niX4bCm5rJ3CeDI1vjZ5Mz55Phsmw9QqjoZTsBFoWh009RQaGx0R"
    const val BR_PUBLISHABLE_KEY =
        "pk_test_51JYFFjJQVROkWvqT6Hy9pW7uPb6UzxT3aACZ0W3olY8KunzDE9mm6OxE5W2EHcdZk7LxN6xk9zumFbZL8zvNwixR0056FVxQmt"
    const val GB_PUBLISHABLE_KEY =
        "pk_test_51KmkHbGoesj9fw9QAZJlz1qY4dns8nFmLKc7rXiWKAIj8QU7NPFPwSY1h8mqRaFRKQ9njs9pVJoo2jhN6ZKSDA4h00mjcbGF7b"
    const val MY_PUBLISHABLE_KEY =
        "pk_test_vGCjSmT6Idy5zwfGBKnlq5rd00JT2vbrHb"
    const val JP_PUBLISHABLE_KEY =
        "pk_test_51NpIYRIq2LmpyICoBLPaTxfWFW4I34pnWuBjKXf8CgOlVih7Ni6oDfPRHGTzBEnpsrHiPvqP2UyydilqY66BWp8N00mQCJ1PU5"
    const val FR_PUBLISHABLE_KEY =
        "pk_test_51JtgfQKG6vc7r7YCU0qQNOkDaaHrEgeHgGKrJMNfuWwaKgXMLzPUA1f8ZlCNPonIROLOnzpUnJK1C1xFH3M3Mz8X00Q6O4GfUt"
    const val TH_PUBLISHABLE_KEY =
        "pk_test_51NpEAWBgCYKNuUnnoBpaJZQYWOO6UpLtcioKggla08zpvDDy0cjfGKZdl5BsU8Gm5ilJNCqT7laCsqvyc0LndskG00pnPnJSpD"
    const val DE_PUBLISHABLE_KEY =
        "pk_test_51PSnNaAlz2yHYCNZgjajit4L8Hl1rDDPPCj9XhHNZWRSi4vwHhrHIbTgstLJptPSzwQVl1HlyqhwWRs1rBJHag8W00sM0SOXIL"
    const val IT_PUBLISHABLE_KEY =
        "pk_test_51PSnETIFbdis1OxTALF4Z8ugUQpVS06UQDVahMSmwrbEYphjNYitXtOSqMPVKfzl3jukg6gLLrtZNnPlDrRbDpMd00U0tId6iv"
}
