package com.stripe.android.common.spms

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.uicore.elements.Controller
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultSavedPaymentMethodLinkFormHelperTest {
    @Test
    fun `link form is unavailable when signup mode is not defined`() = runScenario(
        linkState = LinkState(
            configuration = TestFactory.LINK_CONFIGURATION,
            loginState = LinkState.LoginState.LoggedOut,
            signupMode = null,
        ),
    ) {
        helper.state.test {
            Truth.assertThat(awaitItem()).isEqualTo(SavedPaymentMethodLinkFormHelper.State.Unused)
        }

        Truth.assertThat(helper.formElement).isNull()

        formElementFactoryCreateCalls.expectNoEvents()
    }

    @Test
    fun `link form element is available when signup mode is defined`() {
        val coordinator = FakeLinkConfigurationCoordinator()

        runScenario(
            linkConfigurationCoordinator = coordinator,
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            ),
        ) {
            helper.state.test {
                Truth.assertThat(awaitItem()).isEqualTo(SavedPaymentMethodLinkFormHelper.State.Unused)
            }

            Truth.assertThat(helper.formElement).isNotNull()

            val factoryCreateCall = formElementFactoryCreateCalls.awaitItem()

            Truth.assertThat(factoryCreateCall.signupMode)
                .isEqualTo(LinkSignupMode.InsteadOfSaveForFutureUse)
            Truth.assertThat(factoryCreateCall.configuration)
                .isEqualTo(TestFactory.LINK_CONFIGURATION)
            Truth.assertThat(factoryCreateCall.linkConfigurationCoordinator)
                .isEqualTo(coordinator)
            Truth.assertThat(factoryCreateCall.previousLinkSignupCheckboxSelection)
                .isFalse()
            Truth.assertThat(factoryCreateCall.userInput).isNull()
        }
    }

    @Test
    fun `link form element always uses InsteadOfSaveForFutureUse irregardless of signup mode`() {
        val coordinator = FakeLinkConfigurationCoordinator()

        runScenario(
            linkConfigurationCoordinator = coordinator,
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            ),
        ) {
            Truth.assertThat(helper.formElement).isNotNull()

            val factoryCreateCall = formElementFactoryCreateCalls.awaitItem()

            Truth.assertThat(factoryCreateCall.signupMode)
                .isEqualTo(LinkSignupMode.InsteadOfSaveForFutureUse)
        }
    }

    @Test
    fun `factory is called with input when handle has stored link input`() {
        val userInput = UserInput.SignUp(
            name = "John Doe",
            email = "emaiL@email.com",
            phone = "+11234567890",
            country = "CA",
            consentAction = SignUpConsentAction.SignUpOptInMobileChecked,
        )

        runScenario(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            ),
            handle = SavedStateHandle(
                initialState = mapOf(
                    SPM_LINK_INPUT_KEY to userInput,
                ),
            ),
        ) {
            val call = formElementFactoryCreateCalls.awaitItem()

            Truth.assertThat(call.userInput).isEqualTo(userInput)
        }
    }

    @Test
    fun `factory is called with previousLinkSignupCheckboxSelection true when handle has checkbox selected`() =
        runScenario(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            ),
            handle = SavedStateHandle(
                initialState = mapOf(
                    SPM_LINK_CHECKBOX_SELECTED_KEY to true,
                ),
            ),
        ) {
            val call = formElementFactoryCreateCalls.awaitItem()
            Truth.assertThat(call.previousLinkSignupCheckboxSelection).isTrue()
        }

    @Test
    fun `onLinkInlineSignupStateChanged updates state to Unused when when link is not used`() =
        runScenario(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            ),
        ) {
            val call = formElementFactoryCreateCalls.awaitItem()

            val viewState = InlineSignupViewState(
                userInput = null,
                merchantName = TestFactory.LINK_CONFIGURATION.merchantName,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                fields = emptyList(),
                prefillEligibleFields = emptySet(),
                allowsDefaultOptIn = false,
                linkSignUpOptInFeatureEnabled = false,
                isExpanded = false,
            )

            call.onLinkInlineSignupStateChanged(viewState)

            helper.state.test {
                Truth.assertThat(awaitItem()).isEqualTo(SavedPaymentMethodLinkFormHelper.State.Unused)
            }

            Truth.assertThat(handle.get<Boolean>(SPM_LINK_CHECKBOX_SELECTED_KEY)).isFalse()
            Truth.assertThat(handle.get<UserInput>(SPM_LINK_INPUT_KEY)).isNull()
        }

    @Test
    fun `onLinkInlineSignupStateChanged updates state to Incomplete when link is used but incomplete input`() =
        runScenario(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            ),
        ) {
            val call = formElementFactoryCreateCalls.awaitItem()

            val viewState = InlineSignupViewState(
                userInput = null,
                merchantName = TestFactory.LINK_CONFIGURATION.merchantName,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                fields = emptyList(),
                prefillEligibleFields = emptySet(),
                allowsDefaultOptIn = false,
                linkSignUpOptInFeatureEnabled = false,
                isExpanded = true,
            )

            call.onLinkInlineSignupStateChanged(viewState)

            helper.state.test {
                Truth.assertThat(awaitItem()).isEqualTo(SavedPaymentMethodLinkFormHelper.State.Incomplete)
            }

            Truth.assertThat(handle.get<Boolean>(SPM_LINK_CHECKBOX_SELECTED_KEY)).isTrue()
            Truth.assertThat(handle.get<UserInput>(SPM_LINK_INPUT_KEY)).isNull()
        }

    @Test
    fun `onLinkInlineSignupStateChanged updates state to Complete when user input is present`() =
        runScenario(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            ),
        ) {
            val call = formElementFactoryCreateCalls.awaitItem()

            val userInput = UserInput.SignUp(
                name = "Test User",
                email = "test@example.com",
                phone = "+15551234567",
                country = "US",
                consentAction = SignUpConsentAction.Checkbox,
            )

            val viewState = InlineSignupViewState(
                userInput = userInput,
                merchantName = TestFactory.LINK_CONFIGURATION.merchantName,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                fields = emptyList(),
                prefillEligibleFields = emptySet(),
                allowsDefaultOptIn = false,
                linkSignUpOptInFeatureEnabled = false,
                isExpanded = true,
            )

            call.onLinkInlineSignupStateChanged(viewState)

            helper.state.test {
                val state = awaitItem()

                Truth.assertThat(state).isInstanceOf<SavedPaymentMethodLinkFormHelper.State.Complete>()

                val completeState = state as SavedPaymentMethodLinkFormHelper.State.Complete

                Truth.assertThat(completeState.userInput).isEqualTo(userInput)
            }

            Truth.assertThat(handle.get<Boolean>(SPM_LINK_CHECKBOX_SELECTED_KEY)).isTrue()
            Truth.assertThat(handle.get<UserInput>(SPM_LINK_INPUT_KEY)).isEqualTo(userInput)
        }

    private fun runScenario(
        linkState: LinkState? = null,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = linkState,
        ),
        handle: SavedStateHandle = SavedStateHandle(),
        linkConfigurationCoordinator: LinkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val factory = FakeLinkFormElementFactory()

        val helper = DefaultSavedPaymentMethodLinkFormHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            savedStateHandle = handle,
            linkFormElementFactory = factory,
        )

        block(
            Scenario(
                helper = helper,
                handle = handle,
                formElementFactoryCreateCalls = factory.calls,
            )
        )

        factory.validate()
    }

    private class Scenario(
        val helper: SavedPaymentMethodLinkFormHelper,
        val handle: SavedStateHandle,
        val formElementFactoryCreateCalls: ReceiveTurbine<FakeLinkFormElementFactory.Call>,
    )

    private class FakeLinkFormElementFactory : LinkFormElementFactory {
        private val _calls = Turbine<Call>()
        val calls: ReceiveTurbine<Call> = _calls

        override fun create(
            signupMode: LinkSignupMode,
            configuration: LinkConfiguration,
            linkConfigurationCoordinator: LinkConfigurationCoordinator,
            userInput: UserInput?,
            onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
            previousLinkSignupCheckboxSelection: Boolean
        ): FormElement {
            _calls.add(
                Call(
                    signupMode = signupMode,
                    configuration = configuration,
                    linkConfigurationCoordinator = linkConfigurationCoordinator,
                    userInput = userInput,
                    onLinkInlineSignupStateChanged = onLinkInlineSignupStateChanged,
                    previousLinkSignupCheckboxSelection = previousLinkSignupCheckboxSelection,
                )
            )

            return FakeElement
        }

        fun validate() {
            _calls.ensureAllEventsConsumed()
        }

        class Call(
            val signupMode: LinkSignupMode,
            val configuration: LinkConfiguration,
            val linkConfigurationCoordinator: LinkConfigurationCoordinator,
            val userInput: UserInput?,
            val onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
            val previousLinkSignupCheckboxSelection: Boolean
        )

        object FakeElement : FormElement {
            override val identifier: IdentifierSpec
                get() = throw IllegalStateException("Should not be retrieved!")

            override val controller: Controller
                get() = throw IllegalStateException("Should not be retrieved!")

            override val allowsUserInteraction: Boolean
                get() = throw IllegalStateException("Should not be retrieved!")

            override val mandateText: ResolvableString
                get() = throw IllegalStateException("Should not be retrieved!")

            override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
                throw IllegalStateException("Should not be called!")
            }
        }
    }

    private companion object {
        const val SPM_LINK_CHECKBOX_SELECTED_KEY = "STRIPE_SPM_LINK_CHECKBOX_SELECTED"
        const val SPM_LINK_INPUT_KEY = "STRIPE_SPM_LINK_INPUT"
    }
}
