@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.elements.ece

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.checkout.asPaymentSheet
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.LinkState
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class DefaultAvailableExpressButtonTypesFactoryTest {
    @Test
    fun `calculates visible button count`(
        @TestParameter(valuesProvider = VisibleButtonCountCaseProvider::class)
        testCase: VisibleButtonCountCase,
    ) {
        val result = calculateVisibleButtonCount(
            buttonCount = testCase.buttonCount,
            maxColumns = testCase.maxColumns,
            maxRows = testCase.maxRows,
        )

        assertThat(result).isEqualTo(testCase.expected)
    }

    @Test
    fun `create returns no express button types when ECE configuration is absent`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = null,
        )

        assertThat(availableExpressButtonTypes).isEmpty()
    }

    @Test
    fun `create returns no express button types when ECE metadata is absent`() {
        val availableExpressButtonTypes = DefaultAvailableExpressButtonTypesFactory().create(
            paymentMethodMetadata = null,
            expressCheckoutElementConfiguration = ExpressCheckoutElement.Configuration().build(),
            requiresShippingAddress = false,
        )

        assertThat(availableExpressButtonTypes).isEmpty()
    }

    @Test
    fun `create keeps only express button types returned by metadata`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.GooglePay),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        )
    }

    @Test
    fun `create filters out Google Pay when ECE disables it`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .googlePayConfiguration(
                    GooglePayConfiguration().display(GooglePayConfiguration.Display.Never)
                ),
        )

        assertThat(availableExpressButtonTypes).isEmpty()
    }

    @Test
    fun `create filters out Link when disabled by configuration`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = ExpressCheckoutElement.Configuration()
                .linkConfiguration(
                    ExpressCheckoutElement.Configuration.LinkConfiguration()
                        .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never)
                ),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        )
    }

    @Test
    fun `create returns all available express button types`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.Link,
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        ).inOrder()
    }

    @Test
    fun `create limits express button types to layout capacity`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = configurationWithLayout(maxColumns = 1, maxRows = 1),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        )
    }

    @Test
    fun `create adds google pay to end of list when it would otherwise be removed`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.Link, WalletType.GooglePay),
            configuration = configurationWithLayout(maxColumns = 1, maxRows = 2),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.Link,
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        ).inOrder()
    }

    @Test
    fun `create does not limit express button types when only max columns is configured`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = configurationWithLayout(maxColumns = 1, maxRows = null),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.Link,
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        ).inOrder()
    }

    @Test
    fun `create does not limit express button types when only max rows is configured`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link, WalletType.GooglePay),
            configuration = configurationWithLayout(maxColumns = null, maxRows = 1),
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.Link,
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        ).inOrder()
    }

    @Test
    fun `create keeps link when it is the only available type within layout capacity`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link),
            configuration = configurationWithLayout(maxColumns = 1, maxRows = 1),
        )

        assertThat(availableExpressButtonTypes).containsExactly(ExpressButtonType.Link)
    }

    @Test
    fun `create keeps google pay when shipping address is required`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.GooglePay),
            requiresShippingAddress = true,
        )

        assertThat(availableExpressButtonTypes).containsExactly(
            ExpressButtonType.GooglePay(TEST_GOOGLE_PAY_CONFIGURATION),
        )
    }

    @Test
    fun `create filters out link when shipping address is required`() {
        val availableExpressButtonTypes = create(
            availableWallets = listOf(WalletType.Link),
            requiresShippingAddress = true,
        )

        assertThat(availableExpressButtonTypes).isEmpty()
    }

    private fun create(
        availableWallets: List<WalletType>,
        configuration: ExpressCheckoutElement.Configuration? = ExpressCheckoutElement.Configuration(),
        requiresShippingAddress: Boolean = false,
        linkLoginState: LinkState.LoginState = LinkState.LoginState.LoggedOut,
    ): List<ExpressButtonType> {
        val configurationState = configuration?.build()
        return DefaultAvailableExpressButtonTypesFactory().create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                availableWallets = availableWallets,
                linkConfiguration = configurationState?.linkConfiguration?.asPaymentSheet()
                    ?: PaymentSheet.LinkConfiguration(),
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION,
                    loginState = linkLoginState,
                    signupMode = null,
                ),
            ),
            expressCheckoutElementConfiguration = configurationState,
            requiresShippingAddress = requiresShippingAddress,
        )
    }

    private fun configurationWithLayout(
        maxColumns: Int?,
        maxRows: Int?,
    ): ExpressCheckoutElement.Configuration {
        return ExpressCheckoutElement.Configuration()
            .appearance(
                ExpressCheckoutElement.Configuration.Appearance()
                    .buttonLayout(
                        ExpressCheckoutElement.Configuration.Appearance.ButtonLayout()
                            .maxColumns(maxColumns)
                            .maxRows(maxRows)
                    )
            )
    }

    private companion object {
        val TEST_GOOGLE_PAY_CONFIGURATION = GooglePayConfiguration().build()
    }
}

internal object VisibleButtonCountCaseProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<VisibleButtonCountCase> = listOf(
        VisibleButtonCountCase(
            name = "No buttons",
            buttonCount = 0,
            maxColumns = 2,
            maxRows = 2,
            expected = 0,
        ),
        VisibleButtonCountCase(
            name = "No limits",
            buttonCount = 5,
            maxColumns = null,
            maxRows = null,
            expected = 5,
        ),
        VisibleButtonCountCase(
            name = "Columns only",
            buttonCount = 5,
            maxColumns = 2,
            maxRows = null,
            expected = 5,
        ),
        VisibleButtonCountCase(
            name = "Rows only",
            buttonCount = 5,
            maxColumns = null,
            maxRows = 2,
            expected = 5,
        ),
        VisibleButtonCountCase(
            name = "Capacity below available buttons",
            buttonCount = 5,
            maxColumns = 2,
            maxRows = 2,
            expected = 4,
        ),
        VisibleButtonCountCase(
            name = "Capacity above available buttons",
            buttonCount = 3,
            maxColumns = 2,
            maxRows = 2,
            expected = 3,
        ),
    )
}

internal data class VisibleButtonCountCase(
    val name: String,
    val buttonCount: Int,
    val maxColumns: Int?,
    val maxRows: Int?,
    val expected: Int,
) {
    override fun toString(): String = name
}
