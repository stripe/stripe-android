package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class SofortUiDefinitionFactoryTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    private val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("sofort"),
        )
    )

    @Test
    fun testSofort() {
        paparazziRule.snapshot {
            SofortDefinition.CreateFormUi(
                metadata = metadata
            )
        }
    }

    @Test
    fun testSofortWithSetupIntent() {
        paparazziRule.snapshot {
            SofortDefinition.CreateFormUi(
                metadata = metadata.copy(
                    stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                        paymentMethodTypes = listOf("sofort")
                    )
                )
            )
        }
    }
}
