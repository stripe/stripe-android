package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AffirmDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AfterpayClearpayDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AlipayDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AlmaDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AmazonPayDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AuBecsDebitDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BacsDebitDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BancontactDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BlikDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BoletoDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CashAppPayDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.EpsDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.FpxDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.GiroPayDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.GrabPayDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.IdealDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.KlarnaDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.KonbiniDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.MobilePayDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.OxxoDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.P24Definition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.PayPalDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.RevolutPayDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.SepaDebitDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.SofortDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.SwishDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.TwintDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.UpiDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.UsBankAccountDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.WeChatPayDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.ZipDefinition
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object PaymentMethodRegistry {

    val all: Set<PaymentMethodDefinition> = setOf(
        AffirmDefinition,
        AfterpayClearpayDefinition,
        AlipayDefinition,
        AlmaDefinition,
        AmazonPayDefinition,
        AuBecsDebitDefinition,
        BacsDebitDefinition,
        BancontactDefinition,
        BlikDefinition,
        BoletoDefinition,
        CardDefinition,
        CashAppPayDefinition,
        EpsDefinition,
        FpxDefinition,
        GiroPayDefinition,
        GrabPayDefinition,
        IdealDefinition,
        KlarnaDefinition,
        KonbiniDefinition,
        MobilePayDefinition,
        OxxoDefinition,
        P24Definition,
        PayPalDefinition,
        RevolutPayDefinition,
        SepaDebitDefinition,
        SofortDefinition,
        SwishDefinition,
        TwintDefinition,
        UpiDefinition,
        UsBankAccountDefinition,
        WeChatPayDefinition,
        ZipDefinition,
    )

    val definitionsByCode: Map<String, PaymentMethodDefinition> by lazy {
        all.associateBy { it.type.code }
    }

    fun filterSupportedPaymentMethods(
        metadata: PaymentMethodMetadata,
        sharedDataSpecs: List<SharedDataSpec>,
    ): List<SupportedPaymentMethod> {
        return all.filter {
            it.isSupported(metadata)
        }.mapNotNull { paymentMethodDefinition ->
            val sharedDataSpec = sharedDataSpecs.firstOrNull { it.type == paymentMethodDefinition.type.code }
            if (sharedDataSpec != null) {
                paymentMethodDefinition.supportedPaymentMethod(metadata, sharedDataSpec)
            } else {
                null
            }
        }
    }
}
