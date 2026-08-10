package com.stripe.android.link.ui.paymentmethod

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.ui.LinkScreenshotSurface
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodBody
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class PaymentMethodScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @get:Rule
    val enableKlarnaFormRemovalRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableKlarnaFormRemoval,
        isEnabled = false,
    )

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `form with button disabled`() {
        snapshot()
    }

    @Test
    fun `form with button disabled & validation`() {
        snapshot(
            state = state(
                isValidating = true
            )
        )
    }

    @Test
    fun `form with button enabled`() {
        snapshot(
            state = state(
                primaryButtonState = PrimaryButtonState.Enabled
            )
        )
    }

    @Test
    fun `form with error message`() {
        snapshot(
            state = state(
                errorMessage = "Something went wrong".resolvableString
            )
        )
    }

    @Test
    fun `klarna form`() {
        snapshot(
            state = state(
                paymentMethodCode = PaymentMethod.Type.Klarna.code,
            ),
        )
    }

    private fun snapshot(
        state: PaymentMethodState = state()
    ) {
        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                LinkScreenshotSurface {
                    PaymentMethodBody(
                        state = state,
                        onFormFieldValuesChanged = {},
                        onPayClicked = {},
                        onDisabledPayClicked = {},
                    )
                }
            }
        }
    }

    private fun state(
        paymentMethodCode: String = PaymentMethod.Type.Card.code,
        primaryButtonState: PrimaryButtonState = PrimaryButtonState.Disabled,
        errorMessage: ResolvableString? = null,
        isValidating: Boolean = false,
    ): PaymentMethodState {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.Klarna.code,
                ),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            ),
            checkoutSessionResponse = null,
        )
        val uiDefinitionArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            linkConfigurationCoordinator = null,
            linkInlineHandler = null,
            onLinkInlineSignupStateChanged = { throw AssertionError("Not expected") },
            autocompleteAddressInteractorFactory = null,
        )
        val formElements = metadata.formElementsForCode(
            code = paymentMethodCode,
            uiDefinitionFactoryArgumentsFactory = uiDefinitionArgumentsFactory,
        )
        return PaymentMethodState(
            formArguments = FormArgumentsFactory.create(
                paymentMethodCode = paymentMethodCode,
                metadata = metadata,
            ),
            formElements = formElements ?: emptyList(),
            primaryButtonState = primaryButtonState,
            primaryButtonLabel = "$50".resolvableString,
            errorMessage = errorMessage,
            isValidating = isValidating,
            paymentMethodCreateParams = null
        )
    }
}
