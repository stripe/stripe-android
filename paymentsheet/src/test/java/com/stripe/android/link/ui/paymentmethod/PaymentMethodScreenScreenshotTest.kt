package com.stripe.android.link.ui.paymentmethod

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodBody
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.screenshottesting.PaparazziRule
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

    private fun snapshot(
        state: PaymentMethodState = state()
    ) {
        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                DefaultLinkTheme {
                    PaymentMethodBody(
                        state = state,
                        onFormFieldValuesChanged = {},
                        onPayClicked = {}
                    )
                }
            }
        }
    }

    private fun state(
        primaryButtonState: PrimaryButtonState = PrimaryButtonState.Disabled,
        errorMessage: ResolvableString? = null
    ): PaymentMethodState {
        val metadata = PaymentMethodMetadataFactory.create()
        val uiDefinitionArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            linkConfigurationCoordinator = null,
            onLinkInlineSignupStateChanged = { throw AssertionError("Not expected") },
        )
        val formElements = metadata.formElementsForCode(
            code = PaymentMethod.Type.Card.code,
            uiDefinitionFactoryArgumentsFactory = uiDefinitionArgumentsFactory,
        )
        return PaymentMethodState(
            formArguments = FormArgumentsFactory.create(
                paymentMethodCode = PaymentMethod.Type.Card.code,
                metadata = metadata
            ),
            formElements = formElements ?: emptyList(),
            primaryButtonState = primaryButtonState,
            primaryButtonLabel = "$50".resolvableString,
            errorMessage = errorMessage,
            paymentMethodCreateParams = null
        )
    }
}
