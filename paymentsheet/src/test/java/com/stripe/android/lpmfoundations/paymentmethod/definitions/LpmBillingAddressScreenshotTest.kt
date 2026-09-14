package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
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
        val metadata = config.metadata()

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                VerticalModeFormUI(
                    interactor = FakeVerticalModeFormInteractor.create(
                        paymentMethodCode = config.paymentMethodType.code,
                        metadata = metadata,
                    ),
                    showsWalletHeader = false,
                )
            }
        }
    }
}
