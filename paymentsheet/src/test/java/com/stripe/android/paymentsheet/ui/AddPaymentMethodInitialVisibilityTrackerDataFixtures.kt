package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.unit.dp

internal object AddPaymentMethodInitialVisibilityTrackerDataFixtures {
    private val PAYMENT_METHOD_CODES = listOf("card", "klarna", "affirm", "amazon_pay", "bufo_pay", "llama_pay")

    val NO_VISIBLE_ITEMS = AddPaymentMethodInitialVisibilityTrackerData(
        paymentMethodCodes = emptyList(),
        tabWidth = 100.dp,
        screenWidth = 110.dp,
        innerPadding = 5.dp,
    )

    val ONE_ITEM = AddPaymentMethodInitialVisibilityTrackerData(
        paymentMethodCodes = PAYMENT_METHOD_CODES.take(1),
        tabWidth = 100.dp,
        screenWidth = 110.dp,
        innerPadding = 5.dp,
    )

    val ONE_ITEM_EXPECTED_VISIBLE = PAYMENT_METHOD_CODES.take(1)

    val TWO_ITEMS = AddPaymentMethodInitialVisibilityTrackerData(
        paymentMethodCodes = PAYMENT_METHOD_CODES.take(2),
        tabWidth = 100.dp,
        screenWidth = 210.dp,
        innerPadding = 5.dp,
    )

    val TWO_ITEMS_EXPECTED_VISIBLE = PAYMENT_METHOD_CODES.take(2)

    val THREE_ITEMS = AddPaymentMethodInitialVisibilityTrackerData(
        paymentMethodCodes = PAYMENT_METHOD_CODES.take(3),
        tabWidth = 100.dp,
        screenWidth = 320.dp,
        innerPadding = 5.dp,
    )

    val THREE_ITEMS_EXPECTED_VISIBLE = PAYMENT_METHOD_CODES.take(3)

    // 105 * 3 = 315
    // 100 * 0.96 = 96 [95% is the threshold]
    // 315 + 96 = 411
    val FOUR_ITEMS_ONE_95_PERCENT_VISIBLE = AddPaymentMethodInitialVisibilityTrackerData(
        paymentMethodCodes = PAYMENT_METHOD_CODES.take(4),
        tabWidth = 100.dp,
        screenWidth = 411.dp,
        innerPadding = 5.dp,
    )

    val FOUR_ITEMS_ONE_95_PERCENT_VISIBLE_EXPECTED_VISIBLE = PAYMENT_METHOD_CODES.take(4)

    // maxItemsThatFit = floor( 400 / (105)) =  3
    // remainingWidth = 400 - (3 * 105) = 85
    // 85/100 = 85% < 95%, therefore not considered visible
    val FOUR_ITEMS_ONE_BELOW_THRESHOLD = AddPaymentMethodInitialVisibilityTrackerData(
        paymentMethodCodes = PAYMENT_METHOD_CODES.take(4),
        tabWidth = 100.dp,
        screenWidth = 400.dp,
        innerPadding = 5.dp,
    )

    val FOUR_ITEMS_ONE_BELOW_THRESHOLD_EXPECTED_VISIBLE = PAYMENT_METHOD_CODES.take(3)
    val FOUR_ITEMS_ONE_BELOW_THRESHOLD_EXPECTED_HIDDEN = PAYMENT_METHOD_CODES.subList(3, 4)

    // 105 * 3 = 315
    // 100 * 0.5 = 50 [95% is the threshold]
    // 315 + 50 = 365
    val MANY_ITEMS_ONE_PARTIALLY_VISIBLE = AddPaymentMethodInitialVisibilityTrackerData(
        paymentMethodCodes = listOf("card", "klarna", "affirm", "amazon_pay", "bufo_pay", "llama_pay"),
        tabWidth = 100.dp,
        screenWidth = 365.dp,
        innerPadding = 5.dp,
    )

    val MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_VISIBLE = PAYMENT_METHOD_CODES.take(3)
    val MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_HIDDEN = PAYMENT_METHOD_CODES.drop(3)
}
