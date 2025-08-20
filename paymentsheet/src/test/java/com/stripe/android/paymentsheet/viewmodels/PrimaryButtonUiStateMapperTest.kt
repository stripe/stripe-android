package com.stripe.android.paymentsheet.viewmodels

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.Turbine
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FakeCvcRecollectionInteractor
import com.stripe.android.paymentsheet.FakeSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeVerticalModeFormInteractor
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.CvcConfig
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrimaryButtonUiStateMapperTest {

    @Test
    fun `Chooses custom button over default one`() = runTest {
        val usBankButton = PrimaryButton.UIState(
            label = "US Bank Account FTW".resolvableString,
            onClick = {},
            enabled = false,
            lockVisible = true,
        )

        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(
                    FakeSelectSavedPaymentMethodsInteractor()
                )
            ),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(usBankButton),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()).isEqualTo(usBankButton)
        }
    }

    @Test
    fun `Enables button if selection is not null`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(
                    FakeSelectSavedPaymentMethodsInteractor()
                )
            ),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isTrue()
        }
    }

    @Test
    fun `Disables button if selection is null`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(
                    FakeSelectSavedPaymentMethodsInteractor()
                )
            ),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isFalse()
        }
    }

    @Test
    fun `Disables button if editing or processing, even if selection is not null`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(
                    FakeSelectSavedPaymentMethodsInteractor()
                )
            ),
            buttonsEnabledFlow = stateFlowOf(false),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isFalse()
        }
    }

    @Test
    fun `Hides button in complete flow if on a screen that isn't supposed to show it`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(PaymentSheetScreen.Loading),
            buttonsEnabledFlow = stateFlowOf(false),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `Hides button in custom flow if pm doesn't need confirm on hides confirm screen`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(
                    FakeSelectSavedPaymentMethodsInteractor()
                )
            ),
            buttonsEnabledFlow = stateFlowOf(false),
            selectionFlow = stateFlowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCustomFlow()

        result.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `Hides button in custom flow if pm requires confirm, & screen hides Mandate, hides confirm `() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.ManageSavedPaymentMethods(
                    FakeManageScreenInteractor()
                )
            ),
            buttonsEnabledFlow = stateFlowOf(false),
            selectionFlow = stateFlowOf(usBankAccountSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCustomFlow()

        result.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `Show button in custom flow if pm requires confirm, & screen shows mandates, hides confirm`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(
                    FakeSelectSavedPaymentMethodsInteractor()
                )
            ),
            buttonsEnabledFlow = stateFlowOf(false),
            selectionFlow = stateFlowOf(usBankAccountSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCustomFlow()

        result.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `Show button in custom flow if pm requires confirm, & screen shows mandates, shows confirm`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.AddFirstPaymentMethod(
                    FakeAddPaymentMethodInteractor(
                        initialState = FakeAddPaymentMethodInteractor.createState(
                            paymentMethodCode = PaymentMethod.Type.USBankAccount.code
                        )
                    )
                )
            ),
            buttonsEnabledFlow = stateFlowOf(false),
            selectionFlow = stateFlowOf(usBankAccountSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCustomFlow()

        result.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `Shows lock icon correctly based on the flow type`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(
                    FakeSelectSavedPaymentMethodsInteractor()
                )
            ),
            buttonsEnabledFlow = stateFlowOf(false),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(usBankAccountSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val resultWithLock = mapper.forCompleteFlow()
        val resultWithoutLock = mapper.forCustomFlow()

        resultWithLock.test {
            assertThat(awaitItem()?.lockVisible).isTrue()
        }

        resultWithoutLock.test {
            assertThat(awaitItem()?.lockVisible).isFalse()
        }
    }

    @Test
    fun `Disables button if cvc is required and input is incomplete`() = runTest {
        val cvcFlow = stateFlowOf(CvcController(CvcConfig(), stateFlowOf(CardBrand.Unknown)))
        val screen = stateFlowOf(
            PaymentSheetScreen.SelectSavedPaymentMethods(
                FakeSelectSavedPaymentMethodsInteractor(),
                PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState.Required(cvcFlow)
            )
        )
        val selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.createCard()
        )
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = screen,
            buttonsEnabledFlow = stateFlowOf(true),
            selectionFlow = stateFlowOf(selection),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isFalse()
        }
    }

    @Test
    fun `Enables button if cvc is required and input is complete`() = runTest {
        val cvcFlow = stateFlowOf(CvcController(CvcConfig(), stateFlowOf(CardBrand.Unknown)))
        val screen = stateFlowOf(
            PaymentSheetScreen.SelectSavedPaymentMethods(
                FakeSelectSavedPaymentMethodsInteractor(),
                PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState.Required(cvcFlow)
            )
        )
        val selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.createCard()
        )
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = screen,
            buttonsEnabledFlow = stateFlowOf(true),
            selectionFlow = stateFlowOf(selection),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isTrue()
        }
    }

    @Test
    fun `Enables button if cvc is not required and input is incomplete`() = runTest {
        val screen = stateFlowOf(
            PaymentSheetScreen.SelectSavedPaymentMethods(FakeSelectSavedPaymentMethodsInteractor())
        )
        val selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.createCard()
        )
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = screen,
            buttonsEnabledFlow = stateFlowOf(true),
            selectionFlow = stateFlowOf(selection),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isTrue()
        }
    }

    @Test
    fun `Enables button if cvc is required and selection is not saved card`() = runTest {
        val cvcFlow = stateFlowOf(CvcController(CvcConfig(), stateFlowOf(CardBrand.Unknown)))
        val screen = stateFlowOf(
            PaymentSheetScreen.SelectSavedPaymentMethods(
                FakeSelectSavedPaymentMethodsInteractor(),
                PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState.Required(cvcFlow)
            )
        )
        val selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD
        )
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = screen,
            buttonsEnabledFlow = stateFlowOf(true),
            selectionFlow = stateFlowOf(selection),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isTrue()
        }
    }

    @Test
    fun `Formats currency correctly based on per app localization`() = runTest {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("fr")
        AppCompatDelegate.setApplicationLocales(appLocale)

        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(FakeSelectSavedPaymentMethodsInteractor())
            ),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            val item = awaitItem()
            assertThat(
                item?.label
            ).isEqualTo(
                resolvableString(
                    id = com.stripe.android.ui.core.R.string.stripe_pay_button_amount,
                    formatArgs = arrayOf("12,34 \$US")
                )
            )
            assertThat(item?.lockVisible).isTrue()
        }
    }

    @Test
    fun `Formats currency correctly with no per app localization`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(FakeSelectSavedPaymentMethodsInteractor())
            ),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        assertThat(AppCompatDelegate.getApplicationLocales()).isEqualTo(LocaleListCompat.getEmptyLocaleList())

        result.test {
            val item = awaitItem()
            assertThat(
                item?.label
            ).isEqualTo(
                resolvableString(
                    id = com.stripe.android.ui.core.R.string.stripe_pay_button_amount,
                    formatArgs = arrayOf("\$12.34")
                )
            )
            assertThat(item?.lockVisible).isTrue()
        }
    }

    @Test
    fun `screen buy button override should be used for complete flow when available`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.CvcRecollection(FakeCvcRecollectionInteractor())
            ),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            val item = awaitItem()
            assertThat(item?.label).isEqualTo(resolvableString(R.string.stripe_paymentsheet_confirm))
            assertThat(item?.lockVisible).isFalse()
        }
    }

    @Test
    fun `screen buy button override should not be used for complete flow when unavailable`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.SelectSavedPaymentMethods(FakeSelectSavedPaymentMethodsInteractor())
            ),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            val item = awaitItem()
            assertThat(
                item?.label
            ).isEqualTo(
                resolvableString(
                    id = com.stripe.android.ui.core.R.string.stripe_pay_button_amount,
                    formatArgs = arrayOf("\$12.34")
                )
            )
            assertThat(item?.lockVisible).isTrue()
        }
    }

    @Test
    fun `screen buy button override should not be used for custom flow`() = runTest {
        val usBankButton = PrimaryButton.UIState(
            label = "US Bank Account FTW".resolvableString,
            onClick = {},
            enabled = false,
            lockVisible = true,
        )

        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(
                PaymentSheetScreen.CvcRecollection(FakeCvcRecollectionInteractor())
            ),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(usBankButton),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCustomFlow()

        result.test {
            assertThat(awaitItem()).isEqualTo(usBankButton)
        }
    }

    @Test
    fun `button should not be enabled but clickable while disabled in the 'AddFirstPaymentMethod' form screen`() =
        clickableWhileDisabledTest(
            buttonsEnabled = true,
            screen = PaymentSheetScreen.AddFirstPaymentMethod(
                FakeAddPaymentMethodInteractor(
                    initialState = FakeAddPaymentMethodInteractor.createState(
                        paymentMethodCode = PaymentMethod.Type.USBankAccount.code
                    )
                )
            )
        ) { state, disabledButtonTurbine ->
            assertThat(state.enabled).isFalse()
            assertThat(state.canClickWhileDisabled).isTrue()

            state.onDisabledClick()

            assertThat(disabledButtonTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `button should not be enabled but clickable while disabled in the 'AddAnotherPaymentMethod' form screen`() =
        clickableWhileDisabledTest(
            buttonsEnabled = true,
            screen = PaymentSheetScreen.AddAnotherPaymentMethod(
                FakeAddPaymentMethodInteractor(
                    initialState = FakeAddPaymentMethodInteractor.createState(
                        paymentMethodCode = PaymentMethod.Type.USBankAccount.code
                    )
                )
            )
        ) { state, disabledButtonTurbine ->
            assertThat(state.enabled).isFalse()
            assertThat(state.canClickWhileDisabled).isTrue()

            state.onDisabledClick()

            assertThat(disabledButtonTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `button should not be enabled but clickable while disabled in the 'VerticalMode' form screen`() =
        clickableWhileDisabledTest(
            buttonsEnabled = true,
            screen = PaymentSheetScreen.VerticalModeForm(
                FakeVerticalModeFormInteractor.create(
                    paymentMethodCode = "card",
                    metadata = PaymentMethodMetadataFactory.create(),
                )
            )
        ) { state, disabledButtonTurbine ->
            assertThat(state.enabled).isFalse()
            assertThat(state.canClickWhileDisabled).isTrue()

            state.onDisabledClick()

            assertThat(disabledButtonTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `button should not be enabled or clickable while disabled in the 'AddFirstPaymentMethod' form screen`() =
        clickableWhileDisabledTest(
            buttonsEnabled = false,
            screen = PaymentSheetScreen.AddFirstPaymentMethod(
                FakeAddPaymentMethodInteractor(
                    initialState = FakeAddPaymentMethodInteractor.createState(
                        paymentMethodCode = PaymentMethod.Type.USBankAccount.code
                    )
                )
            )
        ) { state, disabledButtonTurbine ->
            assertThat(state.enabled).isFalse()
            assertThat(state.canClickWhileDisabled).isFalse()

            state.onDisabledClick()
            disabledButtonTurbine.expectNoEvents()
        }

    @Test
    fun `button should not be enabled or clickable while disabled in the 'AddAnotherPaymentMethod' form screen`() =
        clickableWhileDisabledTest(
            buttonsEnabled = false,
            screen = PaymentSheetScreen.AddAnotherPaymentMethod(
                FakeAddPaymentMethodInteractor(
                    initialState = FakeAddPaymentMethodInteractor.createState(
                        paymentMethodCode = PaymentMethod.Type.USBankAccount.code
                    )
                )
            )
        ) { state, disabledButtonTurbine ->
            assertThat(state.enabled).isFalse()
            assertThat(state.canClickWhileDisabled).isFalse()

            state.onDisabledClick()
            disabledButtonTurbine.expectNoEvents()
        }

    @Test
    fun `button should not be enabled or clickable while disabled in the 'VerticalMode' form screen`() =
        clickableWhileDisabledTest(
            buttonsEnabled = false,
            screen = PaymentSheetScreen.VerticalModeForm(
                FakeVerticalModeFormInteractor.create(
                    paymentMethodCode = "card",
                    metadata = PaymentMethodMetadataFactory.create(),
                )
            )
        ) { state, disabledButtonTurbine ->
            assertThat(state.enabled).isFalse()
            assertThat(state.canClickWhileDisabled).isFalse()

            state.onDisabledClick()
            disabledButtonTurbine.expectNoEvents()
        }

    @Test
    fun `button should not be clickable while disabled if not form screen`() =
        clickableWhileDisabledTest(
            buttonsEnabled = true,
            screen = PaymentSheetScreen.VerticalMode(
                FakePaymentMethodVerticalLayoutInteractor.create()
            ),
        ) { state, disabledButtonTurbine ->
            assertThat(state.canClickWhileDisabled).isFalse()

            state.onDisabledClick()
            disabledButtonTurbine.expectNoEvents()
        }

    private fun clickableWhileDisabledTest(
        buttonsEnabled: Boolean,
        screen: PaymentSheetScreen,
        block: suspend (state: PrimaryButton.UIState, onDisabledClickTurbine: Turbine<Unit>) -> Unit,
    ) = runTest {
        val onDisabledClickTurbine = Turbine<Unit>()

        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(screen),
            buttonsEnabledFlow = stateFlowOf(buttonsEnabled),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false),
            onDisabledClick = {
                onDisabledClickTurbine.add(Unit)
            }
        )

        val customResult = mapper.forCustomFlow()

        turbineScope {
            onDisabledClickTurbine.expectNoEvents()

            val resultTurbine = customResult.testIn(this)

            val button = resultTurbine.awaitItem()

            assertThat(button).isNotNull()

            val nonNullButton = requireNotNull(button)

            block(nonNullButton, onDisabledClickTurbine)

            resultTurbine.expectNoEvents()
            onDisabledClickTurbine.expectNoEvents()

            resultTurbine.cancelAndIgnoreRemainingEvents()
        }

        val completeResult = mapper.forCompleteFlow()

        turbineScope {
            onDisabledClickTurbine.expectNoEvents()

            val resultTurbine = completeResult.testIn(this)

            val button = resultTurbine.awaitItem()

            assertThat(button).isNotNull()

            val nonNullButton = requireNotNull(button)

            block(nonNullButton, onDisabledClickTurbine)

            resultTurbine.expectNoEvents()
            onDisabledClickTurbine.expectNoEvents()

            resultTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createMapper(
        isProcessingPayment: Boolean,
        config: PaymentSheet.Configuration = PaymentSheet.Configuration("Some Name"),
        currentScreenFlow: StateFlow<PaymentSheetScreen>,
        buttonsEnabledFlow: StateFlow<Boolean>,
        amountFlow: StateFlow<Amount?> = stateFlowOf(null),
        selectionFlow: StateFlow<PaymentSelection?>,
        customPrimaryButtonUiStateFlow: StateFlow<PrimaryButton.UIState?>,
        cvcFlow: StateFlow<Boolean>,
        onDisabledClick: () -> Unit = {},
    ): PrimaryButtonUiStateMapper {
        return PrimaryButtonUiStateMapper(
            config = config,
            isProcessingPayment = isProcessingPayment,
            currentScreenFlow = currentScreenFlow,
            buttonsEnabledFlow = buttonsEnabledFlow,
            amountFlow = amountFlow,
            selectionFlow = selectionFlow,
            customPrimaryButtonUiStateFlow = customPrimaryButtonUiStateFlow,
            cvcCompleteFlow = cvcFlow,
            onClick = {},
            onDisabledClick = onDisabledClick,
        )
    }

    private fun cardSelection(): PaymentSelection {
        return PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
    }

    private fun usBankAccountSelection(): PaymentSelection {
        return PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT,
        )
    }
}
