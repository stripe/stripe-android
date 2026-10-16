package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
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
        @TestParameter(valuesProvider = LpmBillingAddressTestConfigurationProvider::class)
        config: LpmBillingAddressTestConfiguration,
    ) {
        snapshotForm(config.paymentMethodType, config.metadata())
    }

    @Test
    fun testContactInformation(
        @TestParameter(value = ["GoPay", "MoMo"])
        paymentMethodType: PaymentMethod.Type,
    ) {
        val metadata = LpmBillingAddressTestConfiguration(
            paymentMethodType = paymentMethodType,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ).metadata().copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            ),
        )

        snapshotForm(paymentMethodType, metadata)
    }

    private fun snapshotForm(
        paymentMethodType: PaymentMethod.Type,
        metadata: PaymentMethodMetadata,
    ) {
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

    @Test
    fun testNgBankTransferWithContactInformation() {
        val paymentMethodType = PaymentMethod.Type.NgBankTransfer
        val metadata = LpmBillingAddressTestConfiguration(
            paymentMethodType = paymentMethodType,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ).metadata().copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            ),
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
