package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.forms.AffirmForm
import com.stripe.android.ui.core.forms.AfterpayClearpayForm
import com.stripe.android.ui.core.forms.AuBecsDebitForm
import com.stripe.android.ui.core.forms.BancontactForm
import com.stripe.android.ui.core.forms.EpsForm
import com.stripe.android.ui.core.forms.GiropayForm
import com.stripe.android.ui.core.forms.IdealForm
import com.stripe.android.ui.core.forms.KlarnaForm
import com.stripe.android.ui.core.forms.P24Form
import com.stripe.android.ui.core.forms.PaypalForm
import com.stripe.android.ui.core.forms.SepaDebitForm
import com.stripe.android.ui.core.forms.SofortForm

class NewLpms {
    companion object {
        fun values() = listOf( // 12
            AfterpayClearpayJson,
            AffirmJson,
            KlarnaJson,
            IdealJson,
            SofortJson,
            BancontactJson,
            SepaDebitJson,
            EpsJson,
            GiropayJson,
            P24Json,
            PayPalJson,
            AuBecsDebitJson,
            CardJson
        )

        // Shipping requirement still enforced in the mobile SDK
        val AfterpayClearpayJson: SharedDataSpec = SharedDataSpec(
            "afterpay_clearpay",
            fields = AfterpayClearpayForm
        )

        // Shipping requirement still enforced in the mobile SDK
        val AffirmJson: SharedDataSpec = SharedDataSpec(
            "affirm",
            fields = AffirmForm
        )

        val KlarnaJson: SharedDataSpec = SharedDataSpec(
            "klarna",
            fields = KlarnaForm
        )

        val CardJson: SharedDataSpec = SharedDataSpec(
            "card",
            fields = listOf(CardDetailsSectionSpec(), CardBillingSpec())
        )

        val BancontactJson: SharedDataSpec = SharedDataSpec(
            "bancontact",
            fields = BancontactForm
        )

        val SofortJson: SharedDataSpec = SharedDataSpec(
            "sofort",
            fields = SofortForm
        )

        val SepaDebitJson: SharedDataSpec = SharedDataSpec(
            "sepa_debit",
            fields = SepaDebitForm
        )

        val AuBecsDebitJson: SharedDataSpec = SharedDataSpec(
            "au_becs_debit",
            fields = AuBecsDebitForm
        )

        val IdealJson: SharedDataSpec = SharedDataSpec(
            "ideal",
            fields = IdealForm
        )

        val EpsJson: SharedDataSpec = SharedDataSpec(
            "eps",
            fields = EpsForm
        )

        val P24Json: SharedDataSpec = SharedDataSpec(
            "p24",
            fields = P24Form
        )

        val GiropayJson: SharedDataSpec = SharedDataSpec(
            "giropay",
            fields = GiropayForm
        )

        val PayPalJson: SharedDataSpec = SharedDataSpec(
            "paypal",
            fields = PaypalForm
        )
    }
}
