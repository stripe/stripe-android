package com.stripe.android.ui.core.elements

import com.stripe.android.ui.core.forms.AffirmForm
import com.stripe.android.ui.core.forms.AfterpayClearpayForm
import com.stripe.android.ui.core.forms.AuBecsDebitForm
import com.stripe.android.ui.core.forms.BancontactForm
import com.stripe.android.ui.core.forms.CardForm
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
        fun values() = listOf(//12
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
            async = false,
            fields = AfterpayClearpayForm
        )

        // Shipping requirement still enforced in the mobile SDK
        val AffirmJson: SharedDataSpec = SharedDataSpec(
            "affirm",
            async = false,
            fields = AffirmForm
        )

        val KlarnaJson: SharedDataSpec = SharedDataSpec(
            "klarna",
            async = false,
            fields = KlarnaForm
        )

        val CardJson: SharedDataSpec = SharedDataSpec(
            "card",
            async = false,
            fields = CardForm
        )

        val BancontactJson: SharedDataSpec = SharedDataSpec(
            "bancontact",
            async = false,
            fields = BancontactForm
        )

        val SofortJson: SharedDataSpec = SharedDataSpec(
            "sofort",
            async = true,
            fields = SofortForm
        )

        val SepaDebitJson: SharedDataSpec = SharedDataSpec(
            "sepa_debit",
            async = true,
            fields = SepaDebitForm
        )

        val AuBecsDebitJson: SharedDataSpec = SharedDataSpec(
            "au_becs_debit",
            async = false,
            fields = AuBecsDebitForm
        )

        val IdealJson: SharedDataSpec = SharedDataSpec(
            "ideal",
            async = false,
            fields = IdealForm
        )

        val EpsJson: SharedDataSpec = SharedDataSpec(
            "eps",
            async = false,
            fields = EpsForm
        )

        val P24Json: SharedDataSpec = SharedDataSpec(
            "p24",
            async = false,
            fields = P24Form
        )

        val GiropayJson: SharedDataSpec = SharedDataSpec(
            "giropay",
            async = false,
            fields = GiropayForm
        )

        val PayPalJson: SharedDataSpec = SharedDataSpec(
            "paypal",
            async = false,
            fields = PaypalForm
        )
    }
}
