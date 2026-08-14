package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.paymentsheet.verticalmode.FakeVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class LpmBillingAddressScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.padding(16.dp),
    )

    @Test
    fun testScreen(
        @TestParameter(valuesProvider = LpmBillingAddressScreenshotPaymentMethodTypeProvider::class)
        paymentMethodType: PaymentMethod.Type,
        @TestParameter(valuesProvider = LpmBillingAddressScreenshotModeProvider::class)
        mode: LpmBillingAddressBaselineMode,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf(paymentMethodType.code),
            ),
            billingDetailsCollectionConfiguration = mode.billingDetailsCollectionConfiguration(),
        )

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                VerticalModeFormUI(
                    interactor = FakeVerticalModeFormInteractor.create(
                        paymentMethodCode = paymentMethodType.code,
                        metadata = metadata,
                    ),
                    showsWalletHeader = false,
                )
            }
        }
    }
}

internal object LpmBillingAddressScreenshotPaymentMethodTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<*> = listOf(
        value(PaymentMethod.Type.Boleto).withName("Boleto"),
        value(PaymentMethod.Type.SepaDebit).withName("SEPA Debit"),
        value(PaymentMethod.Type.Wero).withName("Wero"),
        value(PaymentMethod.Type.Klarna).withName("Klarna"),
        value(PaymentMethod.Type.BacsDebit).withName("Bacs Debit"),
        value(PaymentMethod.Type.Oxxo).withName("OXXO"),
    )
}

internal object LpmBillingAddressScreenshotModeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<*> = listOf(
        value(LpmBillingAddressBaselineMode.Never).withName("Never"),
        value(LpmBillingAddressBaselineMode.AutomaticWithoutTax).withName("Automatic without tax"),
        value(LpmBillingAddressBaselineMode.Full).withName("Full"),
    )
}
