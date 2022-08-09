package com.stripe.android.model

import com.stripe.android.CardNumberFixtures
import com.stripe.android.cards.Bin

internal object BinFixtures {
    val VISA = requireNotNull(Bin.create(CardNumberFixtures.VISA_NO_SPACES))
    val MASTERCARD = requireNotNull(Bin.create(CardNumberFixtures.MASTERCARD_NO_SPACES))
    val AMEX = requireNotNull(Bin.create(CardNumberFixtures.AMEX_NO_SPACES))
    val JCB = requireNotNull(Bin.create(CardNumberFixtures.JCB_NO_SPACES))
    val DISCOVER = requireNotNull(Bin.create(CardNumberFixtures.DISCOVER_NO_SPACES))
    val DINERSCLUB14 = requireNotNull(Bin.create(CardNumberFixtures.DINERS_CLUB_14_NO_SPACES))
    val DINERSCLUB16 = requireNotNull(Bin.create(CardNumberFixtures.DINERS_CLUB_16_NO_SPACES))
    val UNIONPAY16 = requireNotNull(Bin.create("3568400000000000"))

    val FAKE = requireNotNull(Bin.create("999999"))
}
