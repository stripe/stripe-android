package com.stripe.android

import com.stripe.android.cards.CardNumber

/**
 * See [Basic test card numbers](https://stripe.com/docs/testing#cards)
 */
internal object CardNumberFixtures {
    const val AMEX_NO_SPACES = "378282246310005"
    const val AMEX_WITH_SPACES = "3782 822463 10005"
    val AMEX_BIN = AMEX_NO_SPACES.take(6)
    val AMEX = CardNumber.Unvalidated(AMEX_NO_SPACES)

    const val VISA_NO_SPACES = "4242424242424242"
    const val VISA_WITH_SPACES = "4242 4242 4242 4242"
    val VISA_BIN = VISA_NO_SPACES.take(6)
    val VISA = CardNumber.Unvalidated(VISA_NO_SPACES)

    const val VISA_DEBIT_NO_SPACES = "4000056655665556"
    const val VISA_DEBIT_WITH_SPACES = "4000 0566 5566 5556"
    val VISA_DEBIT = CardNumber.Unvalidated(VISA_DEBIT_NO_SPACES)

    const val MASTERCARD_NO_SPACES = "5555555555554444"
    const val MASTERCARD_WITH_SPACES = "5555 5555 5555 4444"
    val MASTERCARD_BIN = MASTERCARD_NO_SPACES.take(6)
    val MASTERCARD = CardNumber.Unvalidated(MASTERCARD_NO_SPACES)

    const val DINERS_CLUB_14_NO_SPACES = "36227206271667"
    const val DINERS_CLUB_14_WITH_SPACES = "3622 720627 1667"
    val DINERS_CLUB_14_BIN = DINERS_CLUB_14_NO_SPACES.take(6)
    val DINERS_CLUB_14 = CardNumber.Unvalidated(DINERS_CLUB_14_NO_SPACES)

    const val DINERS_CLUB_16_NO_SPACES = "3056930009020004"
    const val DINERS_CLUB_16_WITH_SPACES = "3056 9300 0902 0004"
    val DINERS_CLUB_16_BIN = DINERS_CLUB_16_NO_SPACES.take(6)
    val DINERS_CLUB_16 = CardNumber.Unvalidated(DINERS_CLUB_16_NO_SPACES)

    const val DISCOVER_NO_SPACES = "6011000990139424"
    const val DISCOVER_WITH_SPACES = "6011 0009 9013 9424"
    val DISCOVER_BIN = DISCOVER_NO_SPACES.take(6)
    val DISCOVER = CardNumber.Unvalidated(DISCOVER_NO_SPACES)

    const val JCB_NO_SPACES = "3566002020360505"
    const val JCB_WITH_SPACES = "3566 0020 2036 0505"
    val JCB_BIN = JCB_NO_SPACES.take(6)
    val JCB = CardNumber.Unvalidated(JCB_NO_SPACES)

    const val UNIONPAY_NO_SPACES = "6200000000000005"
    const val UNIONPAY_WITH_SPACES = "6200 0000 0000 0005"
    val UNIONPAY_BIN = UNIONPAY_NO_SPACES.take(6)
    val UNIONPAY = CardNumber.Unvalidated(UNIONPAY_NO_SPACES)
}
