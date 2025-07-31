package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.Address
import com.stripe.android.elements.BillingDetails
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.ViewAction
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.stripe.android.paymentsheet.R as PaymentSheetR
import com.stripe.android.ui.core.R as StripeUiCoreR

@Suppress("LargeClass")
class DefaultPaymentMethodVerticalLayoutInteractorTest {

    @Test
    fun state_updatesWhenProcessingUpdates() = runScenario {
        interactor.state.test {
            awaitItem().run {
                assertThat(isProcessing).isFalse()
            }
            processingSource.value = true
            awaitItem().run {
                assertThat(isProcessing).isTrue()
            }
        }
    }

    @Test
    fun state_updatesWhenSelectionUpdates() {
        val paymentMethodTypes = listOf("card", "cashapp")
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = paymentMethodTypes
                )
            ),
            formTypeForCode = { FormHelper.FormType.Empty },
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayablePaymentMethods).isNotEmpty()
                    assertThat(selection).isNull()
                }
            }
            selectionSource.value = PaymentSelection.New.GenericPaymentMethod(
                label = "CashApp".resolvableString,
                iconResource = 0,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(),
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            )

            interactor.state.test {
                awaitItem().run {
                    assertThat(displayablePaymentMethods).isNotEmpty()
                    assertThat((selection as PaymentMethodVerticalLayoutInteractor.Selection.New).code)
                        .isEqualTo("cashapp")
                }
            }
        }
    }

    @Test
    fun state_returnsCorrectSelectionForSavedPM() = runScenario(
        formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
    ) {
        val savedSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )
        selectionSource.value = savedSelection
        mostRecentlySelectedSavedPaymentMethodSource.value = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods).isNotEmpty()
                assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.Saved)
            }
        }
    }

    @Test
    fun `state has manage_all saved payment method action when multiple PMs are available, can edit and remove`() {
        runScenario(
            initialPaymentMethods = PaymentMethodFixtures.createCards(3),
        ) {
            canRemove.value = true

            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL
                    )
                }
            }
        }
    }

    @Test
    fun `state has manage_all action when multiple PMs are available, cannot edit and cannot remove`() {
        runScenario(
            initialPaymentMethods = PaymentMethodFixtures.createCards(3),
        ) {
            canRemove.value = false

            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL
                    )
                }
            }
        }
    }

    @Test
    fun `state has edit_card_brand saved PM action when one saved PM, can edit, and can remove`() {
        runScenario(
            initialPaymentMethods = listOf(PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD),
        ) {
            canRemove.value = true

            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE
                    )
                }
            }
        }
    }

    @Test
    fun `state has edit card brand saved payment method action when one saved PM, can edit, and cannot remove`() {
        runScenario(
            initialPaymentMethods = listOf(PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD),
        ) {
            canRemove.value = false

            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE
                    )
                }
            }
        }
    }

    @Test
    fun `state has manage one saved payment method action when one saved PM, cannot edit, and can remove`() {
        runScenario(
            initialPaymentMethods = PaymentMethodFactory.cards(1), // Creates a non-modifiable card
        ) {
            canRemove.value = true

            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE
                    )
                }
            }
        }
    }

    @Test
    fun `state has no saved payment method action when one saved PM, cannot edit, and cannot remove`() {
        runScenario(
            initialPaymentMethods = PaymentMethodFactory.cards(1), // Creates a non-modifiable card
        ) {
            canRemove.value = false

            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE
                    )
                }
            }
        }
    }

    @Test
    fun `state has no saved payment method action when multiple saved PMs and cannot remove`() {
        runScenario(
            initialPaymentMethods = PaymentMethodFactory.cards(1),
        ) {
            canRemove.value = false

            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE
                    )
                }
            }
        }
    }

    @Test
    fun `Passes promo badge information along to affected payment method`() {
        val incentive = PaymentMethodIncentive(
            identifier = "link_instant_debits",
            displayText = "$5",
        )

        runScenario(incentive = incentive) {
            interactor.state.test {
                val paymentMethods = awaitItem().displayablePaymentMethods
                val instantDebits = paymentMethods.first { it.code == "link" }
                assertThat(instantDebits.promoBadge).isEqualTo("$5")
            }
        }
    }

    @Test
    fun `Does not pass promo badge information along to non-affected payment methods`() {
        val incentive = PaymentMethodIncentive(
            identifier = "a_weird_payment_method",
            displayText = "$5",
        )

        runScenario(incentive = incentive) {
            interactor.state.test {
                val paymentMethods = awaitItem().displayablePaymentMethods
                val instantDebits = paymentMethods.first { it.code == "link" }
                assertThat(instantDebits.promoBadge).isNull()
            }
        }
    }

    @Test
    fun `saved PM selection is removed if only saved pm is removed`() {
        val displayedPM = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        runScenario(
            initialPaymentMethods = listOf(displayedPM),
            initialMostRecentlySelectedSavedPaymentMethod = displayedPM,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }

            mostRecentlySelectedSavedPaymentMethodSource.value = null
            paymentMethodsSource.value = emptyList()

            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNull()
                }
            }
        }
    }

    @Test
    fun `saved PM selection is updated if most recently selected saved pm is removed`() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val displayedPM = paymentMethods[0]
        runScenario(
            initialPaymentMethods = paymentMethods,
            initialMostRecentlySelectedSavedPaymentMethod = displayedPM,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }

            mostRecentlySelectedSavedPaymentMethodSource.value = null
            val updatedPaymentMethods = paymentMethods.minus(displayedPM)
            paymentMethodsSource.value = updatedPaymentMethods

            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(updatedPaymentMethods[0])
                }
            }
        }
    }

    @Test
    fun `removing multiple saved PMs leads to correct displayed saved PM`() {
        val paymentMethods = PaymentMethodFixtures.createCards(5)
        val displayedPM = paymentMethods[2]
        runScenario(
            initialPaymentMethods = paymentMethods,
            initialMostRecentlySelectedSavedPaymentMethod = displayedPM,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }

            var updatedPaymentMethods = paymentMethods.subList(1, 4) // remove first and last PMs
            paymentMethodsSource.value = updatedPaymentMethods

            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }

            mostRecentlySelectedSavedPaymentMethodSource.value = null
            updatedPaymentMethods = paymentMethods.minus(displayedPM)
            paymentMethodsSource.value = updatedPaymentMethods

            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(updatedPaymentMethods[0])
                }
            }
        }
    }

    @Test
    fun `state displays most recently selected PM if it exists`() {
        val displayedPM = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        runScenario(
            initialPaymentMethods = PaymentMethodFixtures.createCards(3).plus(displayedPM),
            initialMostRecentlySelectedSavedPaymentMethod = displayedPM,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }
        }
    }

    @Test
    fun `state displays first saved PM if it exists`() {
        val displayedPM = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        runScenario(
            initialPaymentMethods = listOf(displayedPM).plus(PaymentMethodFixtures.createCards(3)),
            initialMostRecentlySelectedSavedPaymentMethod = null,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }
        }
    }

    @Test
    fun `state updates displayed saved PM if it changes`() {
        val displayedPM = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentMethods = listOf(displayedPM).plus(PaymentMethodFixtures.createCards(3))
        runScenario(
            initialPaymentMethods = paymentMethods,
            initialMostRecentlySelectedSavedPaymentMethod = displayedPM,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }

            val newDisplayedSavedPm = paymentMethods[2]
            mostRecentlySelectedSavedPaymentMethodSource.value = newDisplayedSavedPm

            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(newDisplayedSavedPm)
                }
            }
        }
    }

    @Test
    fun `state has correct displayablePaymentMethods based on saved payment methods for cards`() {
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp")
                )
            ),
            formTypeForCode = { FormHelper.FormType.Empty },
            initialPaymentMethods = PaymentMethodFixtures.createCards(1),
        ) {
            interactor.state.test {
                assertThat(awaitItem().displayablePaymentMethods.first().displayName)
                    .isEqualTo(PaymentSheetR.string.stripe_paymentsheet_new_card.resolvableString)
                ensureAllEventsConsumed()

                // The text shouldn't say new card when another saved payment method type exists.
                paymentMethodsSource.value = listOf(PaymentMethodFixtures.US_BANK_ACCOUNT)
                // Updating paymentMethodsSource causes 2 total emissions, we only care about the last one.
                skipItems(1)

                assertThat(awaitItem().displayablePaymentMethods.first().displayName)
                    .isEqualTo(StripeUiCoreR.string.stripe_paymentsheet_payment_method_card.resolvableString)
            }
        }
    }

    @Test
    fun `calling state_displayablePaymentMethods_onClick calls ViewAction_PaymentMethodSelected`() {
        var onFormFieldValuesChangedCalled = false
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp")
                )
            ),
            formTypeForCode = { FormHelper.FormType.Empty },
            onFormFieldValuesChanged = { fieldValues, selectedPaymentMethodCode ->
                fieldValues.run {
                    assertThat(fieldValuePairs).isEmpty()
                    assertThat(userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
                }
                assertThat(selectedPaymentMethodCode).isEqualTo("cashapp")
                onFormFieldValuesChangedCalled = true
            },
            reportPaymentMethodTypeSelected = {},
        ) {
            val paymentMethod = interactor.state.value.displayablePaymentMethods.first { it.code == "cashapp" }
            paymentMethod.onClick()
            assertThat(onFormFieldValuesChangedCalled).isTrue()
        }
    }

    @Test
    fun noSavedPaymentMethods_noDisplayedSavedPaymentMethod() = runScenario(
        initialPaymentMethods = emptyList(),
        initialMostRecentlySelectedSavedPaymentMethod = null,
    ) {
        interactor.state.test {
            awaitItem().run {
                assertThat(displayedSavedPaymentMethod).isNull()
            }
        }
    }

    @Test
    fun stateDoesNotReturnWalletPaymentMethodsWhenInPaymentSheet() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        ),
        canShowWalletsInline = false,
    ) {
        walletsState.value = WalletsState(
            link = WalletsState.Link("email@email.com"),
            googlePay = WalletsState.GooglePay(
                buttonType = GooglePayButtonType.Pay,
                allowCreditCards = true,
                billingAddressParameters = null,
            ),
            buttonsEnabled = true,
            dividerTextResource = 0,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods.map { it.code }).containsNoneOf("link", "google_pay")
            }
        }
        interactor.showsWalletsHeader.test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun stateIncludesWalletPaymentMethodsWhenInFlowController() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        ),
        canShowWalletsInline = true,
    ) {
        walletsState.value = WalletsState(
            link = WalletsState.Link("email@email.com"),
            googlePay = WalletsState.GooglePay(
                buttonType = GooglePayButtonType.Pay,
                allowCreditCards = true,
                billingAddressParameters = null,
            ),
            buttonsEnabled = true,
            dividerTextResource = 0,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods.map { it.code })
                    .isEqualTo(listOf("link", "google_pay", "card", "cashapp"))
            }
        }
        interactor.showsWalletsHeader.test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun walletDisplayablePaymentMethodsUpdateWalletSelection() {
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp")
                )
            ),
            canShowWalletsInline = true,
        ) {
            walletsState.value = WalletsState(
                link = WalletsState.Link("email@email.com"),
                googlePay = WalletsState.GooglePay(
                    buttonType = GooglePayButtonType.Pay,
                    allowCreditCards = true,
                    billingAddressParameters = null,
                ),
                buttonsEnabled = true,
                dividerTextResource = 0,
                onGooglePayPressed = {},
                onLinkPressed = {},
            )
            assertThat(selection.value).isNull()

            val displayablePaymentMethods = interactor.state.value.displayablePaymentMethods
            displayablePaymentMethods.first { it.code == "link" }.onClick()
            assertThat(selection.value).isEqualTo(PaymentSelection.Link())
            assertThat(updateSelectionTurbine.awaitItem()).isFalse()
            displayablePaymentMethods.first { it.code == "google_pay" }.onClick()
            assertThat(selection.value).isEqualTo(PaymentSelection.GooglePay)
            assertThat(updateSelectionTurbine.awaitItem()).isFalse()
        }
    }

    @Test
    fun walletDisplayablePaymentMethodsLink_invokesRowSelectionCallback() {
        var rowSelectionCallbackInvoked = false
        runScenario(
            invokeRowSelectionCallback = { rowSelectionCallbackInvoked = true },
            paymentMethodMetadata = metadataWithOnlyPaymentMethodTypes,
            canShowWalletsInline = true,
        ) {
            walletsState.value = linkAndGooglePayWalletState

            val displayablePaymentMethods = interactor.state.value.displayablePaymentMethods
            displayablePaymentMethods.first { it.code == "link" }.onClick()

            assertThat(rowSelectionCallbackInvoked).isTrue()
            assertThat(updateSelectionTurbine.awaitItem()).isFalse()
        }
    }

    @Test
    fun walletDisplayablePaymentMethodsGooglePay_invokesRowSelectionCallback() {
        var rowSelectionCallbackInvoked = false
        runScenario(
            invokeRowSelectionCallback = { rowSelectionCallbackInvoked = true },
            paymentMethodMetadata = metadataWithOnlyPaymentMethodTypes,
            canShowWalletsInline = true,
        ) {
            walletsState.value = linkAndGooglePayWalletState

            val displayablePaymentMethods = interactor.state.value.displayablePaymentMethods
            displayablePaymentMethods.first { it.code == "google_pay" }.onClick()

            assertThat(rowSelectionCallbackInvoked).isTrue()
            assertThat(updateSelectionTurbine.awaitItem()).isFalse()
        }
    }

    @Test
    fun stateDoesNotReturnWalletPaymentMethodsWhenInFlowControllerAndGooglePayIsNotAvailable() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        ),
        canShowWalletsInline = true,
    ) {
        walletsState.value = WalletsState(
            link = WalletsState.Link("email@email.com"),
            googlePay = null,
            buttonsEnabled = true,
            dividerTextResource = 0,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods.map { it.code })
                    .isEqualTo(listOf("card", "cashapp"))
            }
        }
        interactor.showsWalletsHeader.test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun stateIncludesGooglePayWhenInFlowControllerAndLinkIsNotAvailable() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        ),
        canShowWalletsInline = true,
    ) {
        walletsState.value = WalletsState(
            link = null,
            googlePay = WalletsState.GooglePay(
                buttonType = GooglePayButtonType.Pay,
                allowCreditCards = true,
                billingAddressParameters = null,
            ),
            buttonsEnabled = true,
            dividerTextResource = 0,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods.map { it.code })
                    .isEqualTo(listOf("google_pay", "card", "cashapp"))
            }
        }
        interactor.showsWalletsHeader.test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun stateDoesReturnsWalletPaymentMethodsWhenInEmbeddedAndGooglePayIsNotAvailable() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        ),
        canShowWalletsInline = true,
        canShowWalletButtons = false,
    ) {
        walletsState.value = WalletsState(
            link = WalletsState.Link("email@email.com"),
            googlePay = null,
            buttonsEnabled = true,
            dividerTextResource = 0,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods.map { it.code })
                    .isEqualTo(listOf("link", "card", "cashapp"))
            }
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_transitionsToFormScreen_whenFieldsAllowUserInteraction() {
        var calledFormScreenFactory = false
        var reportedSelectedPaymentMethodType: PaymentMethodCode? = null
        var reportFormShownForPm: PaymentMethodCode? = null
        runScenario(
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
            transitionToFormScreen = {
                calledFormScreenFactory = true
            },
            reportPaymentMethodTypeSelected = { reportedSelectedPaymentMethodType = it },
            reportFormShown = { reportFormShownForPm = it }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("card"))
            assertThat(calledFormScreenFactory).isTrue()
            assertThat(reportedSelectedPaymentMethodType).isEqualTo("card")
            assertThat(reportFormShownForPm).isEqualTo("card")
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_transitionsToFormScreen_whenSelectedIsUsBank() {
        var calledFormScreenFactory = false
        runScenario(
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
            transitionToFormScreen = {
                calledFormScreenFactory = true
            },
            reportPaymentMethodTypeSelected = {},
            reportFormShown = {},
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("us_bank_account"))
            assertThat(calledFormScreenFactory).isTrue()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_transitionsToFormScreen_whenSelectedIsInstantDebits() {
        var calledFormScreenFactory = false
        runScenario(
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
            transitionToFormScreen = {
                calledFormScreenFactory = true
            },
            reportPaymentMethodTypeSelected = {},
            reportFormShown = {},
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("link"))
            assertThat(calledFormScreenFactory).isTrue()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_updatesSelectedLPM() {
        var onFormFieldValuesChangedCalled = false
        var reportedSelectedPaymentMethodType: PaymentMethodCode? = null
        runScenario(
            formTypeForCode = { FormHelper.FormType.Empty },
            onFormFieldValuesChanged = { fieldValues, selectedPaymentMethodCode ->
                fieldValues.run {
                    assertThat(fieldValuePairs).isEmpty()
                    assertThat(userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
                }
                assertThat(selectedPaymentMethodCode).isEqualTo("cashapp")
                onFormFieldValuesChangedCalled = true
            },
            reportPaymentMethodTypeSelected = { reportedSelectedPaymentMethodType = it }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))
            assertThat(onFormFieldValuesChangedCalled).isTrue()
            assertThat(reportedSelectedPaymentMethodType).isEqualTo("cashapp")
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_callsOnFormFieldValuesChanged() {
        var onFormFieldValuesChangedCalled = false
        val paymentMethodTypes = listOf("card", "cashapp")
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes
            )
        )
        runScenario(
            paymentMethodMetadata = paymentMethodMetadata,
            formTypeForCode = { FormHelper.FormType.MandateOnly("Foobar".resolvableString) },
            onFormFieldValuesChanged = { fieldValues, selectedPaymentMethodCode ->
                fieldValues.run {
                    assertThat(fieldValuePairs).isEmpty()
                    assertThat(userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
                }
                assertThat(selectedPaymentMethodCode).isEqualTo("cashapp")
                onFormFieldValuesChangedCalled = true
            },
            reportPaymentMethodTypeSelected = {}
        ) {
            interactor.state.test {
                assertThat(awaitItem().mandate).isNull()
                interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))
                assertThat(onFormFieldValuesChangedCalled).isTrue()
            }
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_callsOnFormFieldValuesChanged_withDefaultBillingDetails_whenRequired() {
        var onFormFieldValuesChangedCalled = false
        val paymentMethodTypes = listOf("card", "cashapp")
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                attachDefaultsToPaymentMethod = true,
            ),
            defaultBillingDetails = BillingDetails(
                name = "Jenny Rosen",
                email = "mail@mail.com",
                phone = "+13105551234",
                address = Address(
                    line1 = "123 Main Street",
                    line2 = "456",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94111"
                ),
            ),
        )
        runScenario(
            paymentMethodMetadata = paymentMethodMetadata,
            formTypeForCode = { FormHelper.FormType.MandateOnly("Foobar".resolvableString) },
            onFormFieldValuesChanged = { fieldValues, selectedPaymentMethodCode ->
                fieldValues.run {
                    assertThat(fieldValuePairs).isEqualTo(
                        mapOf(
                            IdentifierSpec.Name to FormFieldEntry("Jenny Rosen", isComplete = true),
                            IdentifierSpec.Email to FormFieldEntry("mail@mail.com", isComplete = true),
                            IdentifierSpec.Phone to FormFieldEntry("+13105551234", isComplete = true),
                            IdentifierSpec.Line1 to FormFieldEntry("123 Main Street", isComplete = true),
                            IdentifierSpec.Line2 to FormFieldEntry("456", isComplete = true),
                            IdentifierSpec.City to FormFieldEntry("San Francisco", isComplete = true),
                            IdentifierSpec.State to FormFieldEntry("CA", isComplete = true),
                            IdentifierSpec.Country to FormFieldEntry("US", isComplete = true),
                            IdentifierSpec.PostalCode to FormFieldEntry("94111", isComplete = true),
                        )
                    )
                    assertThat(userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
                }
                assertThat(selectedPaymentMethodCode).isEqualTo("cashapp")
                onFormFieldValuesChangedCalled = true
            },
            reportPaymentMethodTypeSelected = {}
        ) {
            interactor.state.test {
                assertThat(awaitItem().mandate).isNull()
                interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))
                assertThat(onFormFieldValuesChangedCalled).isTrue()
            }
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_sameSavedPaymentMethodIsDisplayed() {
        val savedPaymentMethods = PaymentMethodFixtures.createCards(3)
        val displayedPaymentMethod = savedPaymentMethods[2]
        val paymentMethodTypes = listOf("card", "cashapp")
        var currentlySelectedPaymentMethodCode: PaymentMethodCode? = null
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = paymentMethodTypes
                )
            ),
            formTypeForCode = { FormHelper.FormType.Empty },
            onFormFieldValuesChanged = { _, selectedPaymentMethodCode ->
                currentlySelectedPaymentMethodCode = selectedPaymentMethodCode
            },
            initialPaymentMethods = savedPaymentMethods,
            initialMostRecentlySelectedSavedPaymentMethod = displayedPaymentMethod,
            reportPaymentMethodTypeSelected = {},
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPaymentMethod)
                    assertThat(currentlySelectedPaymentMethodCode).isEqualTo("cashapp")
                }
            }
        }
    }

    @Test
    fun handleViewAction_TransitionToManageSavedPaymentMethods_transitionsToManageScreen() {
        var calledManageScreenFactory = false
        runScenario(
            transitionToManageScreen = {
                calledManageScreenFactory = true
            },
        ) {
            interactor.handleViewAction(ViewAction.TransitionToManageSavedPaymentMethods)
            assertThat(calledManageScreenFactory).isTrue()
        }
    }

    @Test
    fun handleViewAction_OnManageOneSavedPaymentMethod_callsOnUpdatePM() {
        var onUpdatePaymentMethodCalled = false
        runScenario(
            onUpdatePaymentMethod = {
                onUpdatePaymentMethodCalled = true
            },
        ) {
            interactor.handleViewAction(
                ViewAction.OnManageOneSavedPaymentMethod(
                    PaymentMethodFixtures.displayableCard()
                )
            )
            assertThat(onUpdatePaymentMethodCalled).isTrue()
        }
    }

    @Test
    fun handleViewAction_OnManageOneSavedPaymentMethod_transitionsToUpdateScreen_whenFeatureEnabled() {
        var updatedPaymentMethod: DisplayableSavedPaymentMethod? = null
        runScenario(
            onUpdatePaymentMethod = {
                updatedPaymentMethod = it
            },
        ) {
            val paymentMethod = PaymentMethodFixtures.displayableCard()
            interactor.handleViewAction(ViewAction.OnManageOneSavedPaymentMethod(paymentMethod))
            assertThat(updatedPaymentMethod).isEqualTo(paymentMethod)
        }
    }

    @Test
    fun handleViewAction_OnUpdatePaymentMethod_transitionsToUpdateScreen() {
        var updatedPaymentMethod: DisplayableSavedPaymentMethod? = null
        runScenario(
            onUpdatePaymentMethod = {
                updatedPaymentMethod = it
            },
        ) {
            val paymentMethod = PaymentMethodFixtures.displayableCard()
            interactor.handleViewAction(ViewAction.OnManageOneSavedPaymentMethod(paymentMethod))
            assertThat(updatedPaymentMethod).isEqualTo(paymentMethod)
        }
    }

    @Test
    fun handleViewAction_SelectSavedPaymentMethod_selectsSavedPm() {
        val savedPaymentMethod = PaymentMethodFixtures.displayableCard()
        var reportedSelectedPaymentMethodType: PaymentMethodCode? = null
        runScenario(
            reportPaymentMethodTypeSelected = { reportedSelectedPaymentMethodType = it }
        ) {
            interactor.handleViewAction(ViewAction.SavedPaymentMethodSelected(savedPaymentMethod.paymentMethod))
            assertThat((selection.value as PaymentSelection.Saved).paymentMethod)
                .isEqualTo(savedPaymentMethod.paymentMethod)
            assertThat(reportedSelectedPaymentMethodType).isEqualTo("saved")
            assertThat(updateSelectionTurbine.awaitItem()).isTrue()
        }
    }

    @Test
    fun handleViewAction_SelectSavedPaymentMethod_invokesRowSelectionCallback() {
        val savedPaymentMethod = PaymentMethodFixtures.displayableCard()
        var rowSelectionCallbackInvoked = false
        runScenario(
            reportPaymentMethodTypeSelected = { true },
            invokeRowSelectionCallback = {
                rowSelectionCallbackInvoked = true
            }
        ) {
            interactor.handleViewAction(ViewAction.SavedPaymentMethodSelected(savedPaymentMethod.paymentMethod))
            assertThat(rowSelectionCallbackInvoked).isTrue()
            assertThat(updateSelectionTurbine.awaitItem()).isTrue()
        }
    }

    @Test
    fun verticalModeScreenSelection_isNotUpdatedToNullWhenOnAnotherScreen() {
        val expectedPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = expectedPaymentSelection,
            formTypeForCode = { FormHelper.FormType.Empty },
        ) {
            selectionSource.value = null

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.New("link"))
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_isUpdatedToNullWhenCurrentScreen() {
        runScenario(
            initialSelection = PaymentSelection.Link(),
            formTypeForCode = { FormHelper.FormType.Empty },
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.New("link"))
                }
                isCurrentScreenSource.value = true
                selectionSource.value = null
                awaitItem().run {
                    assertThat(selection).isNull()
                }
            }
            assertThat(updateSelectionTurbine.awaitItem()).isFalse()
        }
    }

    @Test
    fun verticalModeScreenSelection_isNeverUpdatedToNewPmWithFormFields() {
        val initialPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
        ) {
            selectionSource.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.New("link"))
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_isUpdatedToNewPmWithFormFields_withCustomShouldUpdateVerticalModeSelection() {
        val initialPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
            shouldUpdateVerticalModeSelection = { true }
        ) {
            selectionSource.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.Selection.New(
                            code = "card",
                            changeDetails = "Visa ···· 4242",
                            canBeChanged = true,
                        )
                    )
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_omitsCardBrand_whenUnknownCardBrand() {
        val initialPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
            shouldUpdateVerticalModeSelection = { true }
        ) {
            selectionSource.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION.copy(brand = CardBrand.Unknown)

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.Selection.New(
                            code = "card",
                            changeDetails = "···· 4242",
                            canBeChanged = true,
                        )
                    )
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_retainsLast4_whenUpdatingTemporarySelection() {
        val initialPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
            shouldUpdateVerticalModeSelection = { true }
        ) {
            selectionSource.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.Selection.New(
                            code = "card",
                            changeDetails = "Visa ···· 4242",
                            canBeChanged = true,
                        )
                    )
                }
                temporarySelectionSource.value = "card"
                ensureAllEventsConsumed()
                temporarySelectionSource.value = null
                ensureAllEventsConsumed()
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_retainsCanBeChanged_whenUpdatingTemporarySelection() {
        runScenario(
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
            shouldUpdateVerticalModeSelection = { true }
        ) {
            selectionSource.value = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.Selection.New(
                            code = "cashapp",
                            changeDetails = null,
                            canBeChanged = true,
                        )
                    )
                }
                temporarySelectionSource.value = "cashapp"
                ensureAllEventsConsumed()
                temporarySelectionSource.value = null
                ensureAllEventsConsumed()
            }
        }
    }

    @Test
    fun verticalModeSelectionIsInitialedAsUsBankAccount() {
        var verticalModeSelection: PaymentSelection? = PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION
        runScenario(
            initialSelection = verticalModeSelection,
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
        ) {
            interactor.state.test {
                assertThat(verticalModeSelection).isEqualTo(PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION)
                assertThat(awaitItem().selection).isEqualTo(
                    PaymentMethodVerticalLayoutInteractor.Selection.New(
                        code = "us_bank_account",
                        changeDetails = "···· 6789",
                        canBeChanged = true,
                    )
                )
                temporarySelectionSource.value = "us_bank_account"
                ensureAllEventsConsumed()
                temporarySelectionSource.value = null
                ensureAllEventsConsumed()
            }
        }
    }

    @Test
    fun verticalModeSelectionIsNotCleared_whenInitializing() {
        runScenario(
            initialSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            formTypeForCode = { FormHelper.FormType.UserInteractionRequired },
        ) {
            interactor.state.test {
                assertThat(selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
                assertThat(awaitItem().selection).isEqualTo(
                    PaymentMethodVerticalLayoutInteractor.Selection.New(
                        code = "card",
                        changeDetails = "Visa ···· 4242",
                        canBeChanged = true,
                    )
                )
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeANewPmWithoutFormFields() {
        val initialPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.Empty },
        ) {
            val newSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION
            selectionSource.value = newSelection

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.Selection.New(
                            code = "cashapp",
                            changeDetails = null,
                            canBeChanged = false,
                        )
                    )
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeAnEpm() {
        val initialPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.Empty },
        ) {
            val newSelection = PaymentMethodFixtures.createExternalPaymentMethod(
                PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
            )
            selectionSource.value = newSelection

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection)
                        .isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.New("external_paypal"))
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeASavedPm() {
        val initialPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.Empty },
            initialPaymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        ) {
            val newSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            selectionSource.value = newSelection
            mostRecentlySelectedSavedPaymentMethodSource.value = newSelection.paymentMethod

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.Saved)
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeLink() {
        val initialPaymentSelection = PaymentSelection.GooglePay
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.Empty },
        ) {
            val newSelection = PaymentSelection.Link()
            selectionSource.value = newSelection

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.New("link"))
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeGooglePay() {
        val initialPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.Empty },
        ) {
            val newSelection = PaymentSelection.GooglePay
            selectionSource.value = newSelection

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.New("google_pay"))
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_isUpdatedToTemporarySelection() {
        val initialPaymentSelection = PaymentSelection.Link()
        runScenario(
            initialSelection = initialPaymentSelection,
            formTypeForCode = { FormHelper.FormType.Empty },
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.New("link"))
                }
                temporarySelectionSource.value = "card"
                awaitItem().run {
                    assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.New("card"))
                }
                temporarySelectionSource.value = null
                awaitItem().run {
                    assertThat(selection).isEqualTo(PaymentMethodVerticalLayoutInteractor.Selection.New("link"))
                }
            }
        }
    }

    @Test
    fun whenVerticalModeScreen_becomesCurrentScreen_updateSelectionCalled() {
        val verticalModeSelection = PaymentSelection.GooglePay

        runScenario(
            initialSelection = verticalModeSelection,
            initialIsCurrentScreen = false,
            formTypeForCode = { FormHelper.FormType.Empty },
        ) {
            isCurrentScreenSource.value = true

            assertThat(selection.value).isEqualTo(verticalModeSelection)
            assertThat(updateSelectionTurbine.awaitItem()).isFalse()
        }
    }

    @Test
    fun selectionUpdatesMandate() {
        val paymentMethodTypes = listOf("card", "cashapp")
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes
            )
        )
        runScenario(
            paymentMethodMetadata = paymentMethodMetadata,
            formTypeForCode = { FormHelper.FormType.MandateOnly("Foobar".resolvableString) },
        ) {
            interactor.state.test {
                assertThat(awaitItem().mandate).isNull()
                selectionSource.value = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION
                assertThat(awaitItem().mandate).isEqualTo("Foobar".resolvableString)
            }
        }
    }

    @Test
    fun temporarySelectionUpdatesMandate() {
        val paymentMethodTypes = listOf("card", "cashapp")
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes
            )
        )
        runScenario(
            paymentMethodMetadata = paymentMethodMetadata,
            formTypeForCode = { code ->
                if (code == "cashapp") {
                    FormHelper.FormType.MandateOnly("Foobar".resolvableString)
                } else {
                    FormHelper.FormType.UserInteractionRequired
                }
            },
        ) {
            interactor.state.test {
                assertThat(awaitItem().mandate).isNull()
                selectionSource.value = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION
                assertThat(awaitItem().mandate).isEqualTo("Foobar".resolvableString)
                temporarySelectionSource.value = "card"
                assertThat(awaitItem().mandate).isNull()
                temporarySelectionSource.value = null
                assertThat(awaitItem().mandate).isEqualTo("Foobar".resolvableString)
            }
        }
    }

    @Test
    fun temporarySelection_doesNotAllowChangeDetails_whenSavedCardIsSelected() = runScenario(
        formTypeForCode = {
            FormHelper.FormType.UserInteractionRequired
        },
        shouldUpdateVerticalModeSelection = { true }
    ) {
        selectionSource.value = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        interactor.state.test {
            assertThat(awaitItem().selection).isEqualTo(
                PaymentMethodVerticalLayoutInteractor.Selection.Saved
            )

            // Mimic user pressing on the new card row button.
            temporarySelectionSource.value = "card"
            assertThat(awaitItem().selection).isEqualTo(
                PaymentMethodVerticalLayoutInteractor.Selection.New(
                    code = "card",
                    changeDetails = null,
                    canBeChanged = false,
                )
            )

            // Mimic user cancelling form sheet.
            temporarySelectionSource.value = null
            assertThat(awaitItem().selection).isEqualTo(
                PaymentMethodVerticalLayoutInteractor.Selection.Saved
            )
        }
    }

    @Test
    fun savedSelectionUpdatesMandate() {
        val paymentMethodTypes = listOf("card", "sepa_debit")
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes
            )
        )
        runScenario(
            paymentMethodMetadata = paymentMethodMetadata,
        ) {
            interactor.state.test {
                assertThat(awaitItem().mandate).isNull()
                selectionSource.value = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
                assertThat(awaitItem().mandate)
                    .isEqualTo(
                        resolvableString(
                            id = StripeUiCoreR.string.stripe_sepa_mandate,
                            paymentMethodMetadata.merchantName
                        )
                    )
                selectionSource.value = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                assertThat(awaitItem().mandate).isNull()
            }
        }
    }

    @Test
    fun linkRowButtonShowsEmailSubtitleorReturningConsumer() {
        val walletsState = WalletsState.create(
            isLinkAvailable = true,
            linkEmail = "foo@bar.com",
            isGooglePayReady = true,
            googlePayButtonType = GooglePayButtonType.Pay,
            buttonsEnabled = true,
            paymentMethodTypes = listOf("card"),
            isSetupIntent = false,
            googlePayLauncherConfig = null,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        runScenario(
            initialWalletsState = walletsState,
            canShowWalletsInline = true,
        ) {
            interactor.state.test {
                val linkPaymentMethod = awaitItem().displayablePaymentMethods.firstOrNull { it.code == "link" }
                assertThat(linkPaymentMethod?.subtitle).isEqualTo("foo@bar.com".resolvableString)
            }
        }
    }

    @Test
    fun linkRowButtonShowsGenericSubtitleForUnknownConsumer() {
        val walletsState = WalletsState.create(
            isLinkAvailable = true,
            linkEmail = null,
            isGooglePayReady = true,
            googlePayButtonType = GooglePayButtonType.Pay,
            buttonsEnabled = true,
            paymentMethodTypes = listOf("card"),
            isSetupIntent = false,
            googlePayLauncherConfig = null,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        runScenario(
            initialWalletsState = walletsState,
            canShowWalletsInline = true,
        ) {
            interactor.state.test {
                val linkPaymentMethod = awaitItem().displayablePaymentMethods.firstOrNull { it.code == "link" }
                assertThat(linkPaymentMethod?.subtitle)
                    .isEqualTo(R.string.stripe_link_simple_secure_payments.resolvableString)
            }
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private val linkAndGooglePayWalletState = WalletsState(
        link = WalletsState.Link("email@email.com"),
        googlePay = WalletsState.GooglePay(
            buttonType = GooglePayButtonType.Pay,
            allowCreditCards = true,
            billingAddressParameters = null,
        ),
        buttonsEnabled = true,
        dividerTextResource = 0,
        onGooglePayPressed = {},
        onLinkPressed = {},
    )

    private val metadataWithOnlyPaymentMethodTypes = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "cashapp")
        )
    )

    @Suppress("LongMethod")
    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cbcEligibility = CardBrandChoiceEligibility.create(
                isEligible = true,
                preferredNetworks = emptyList()
            )
        ),
        initialProcessing: Boolean = false,
        initialSelection: PaymentSelection? = null,
        initialIsCurrentScreen: Boolean = false,
        incentive: PaymentMethodIncentive? = null,
        formTypeForCode: (code: String) -> FormHelper.FormType = { notImplemented() },
        onFormFieldValuesChanged: (formValues: FormFieldValues, selectedPaymentMethodCode: String) -> Unit = { _, _ ->
            notImplemented()
        },
        transitionToManageScreen: () -> Unit = { notImplemented() },
        transitionToFormScreen: (selectedPaymentMethodCode: String) -> Unit = { notImplemented() },
        initialPaymentMethods: List<PaymentMethod> = emptyList(),
        initialMostRecentlySelectedSavedPaymentMethod: PaymentMethod? = null,
        onUpdatePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit = { notImplemented() },
        canShowWalletsInline: Boolean = false,
        canShowWalletButtons: Boolean = true,
        canUpdateFullPaymentMethodDetails: Boolean = false,
        reportPaymentMethodTypeSelected: (PaymentMethodCode) -> Unit = { notImplemented() },
        reportFormShown: (PaymentMethodCode) -> Unit = { notImplemented() },
        shouldUpdateVerticalModeSelection: (String?) -> Boolean = { paymentMethodCode ->
            val requiresFormScreen = paymentMethodCode != null &&
                formTypeForCode(paymentMethodCode) == FormHelper.FormType.UserInteractionRequired
            !requiresFormScreen
        },
        invokeRowSelectionCallback: (() -> Unit)? = null,
        initialWalletsState: WalletsState? = null,
        testBlock: suspend TestParams.() -> Unit
    ) {
        val processing: MutableStateFlow<Boolean> = MutableStateFlow(initialProcessing)
        val temporarySelection: MutableStateFlow<PaymentMethodCode?> = MutableStateFlow(null)
        val selection: MutableStateFlow<PaymentSelection?> = MutableStateFlow(initialSelection)
        val paymentMethods: MutableStateFlow<List<PaymentMethod>> = MutableStateFlow(initialPaymentMethods)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> =
            MutableStateFlow(initialMostRecentlySelectedSavedPaymentMethod)
        val walletsState = MutableStateFlow<WalletsState?>(initialWalletsState)
        val canRemove = MutableStateFlow(true)
        val isCurrentScreen: MutableStateFlow<Boolean> = MutableStateFlow(initialIsCurrentScreen)
        val paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(incentive)

        val updateSelectionTurbine = Turbine<Boolean>()

        val interactor = DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = processing,
            temporarySelection = temporarySelection,
            selection = selection,
            paymentMethodIncentiveInteractor = paymentMethodIncentiveInteractor,
            formTypeForCode = formTypeForCode,
            onFormFieldValuesChanged = onFormFieldValuesChanged,
            transitionToManageScreen = transitionToManageScreen,
            transitionToFormScreen = transitionToFormScreen,
            paymentMethods = paymentMethods,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
            providePaymentMethodName = { it!!.resolvableString },
            canRemove = canRemove,
            walletsState = walletsState,
            canShowWalletsInline = canShowWalletsInline,
            canShowWalletButtons = canShowWalletButtons,
            canUpdateFullPaymentMethodDetails = stateFlowOf(canUpdateFullPaymentMethodDetails),
            updateSelection = { paymentSelection, isFormScreen ->
                selection.value = paymentSelection
                updateSelectionTurbine.add(isFormScreen)
            },
            isCurrentScreen = isCurrentScreen,
            reportPaymentMethodTypeSelected = reportPaymentMethodTypeSelected,
            reportFormShown = reportFormShown,
            onUpdatePaymentMethod = onUpdatePaymentMethod,
            shouldUpdateVerticalModeSelection = shouldUpdateVerticalModeSelection,
            dispatcher = UnconfinedTestDispatcher(),
            mainDispatcher = UnconfinedTestDispatcher(),
            invokeRowSelectionCallback = invokeRowSelectionCallback,
        )

        TestParams(
            selection = selection,
            updateSelectionTurbine = updateSelectionTurbine,
            processingSource = processing,
            temporarySelectionSource = temporarySelection,
            selectionSource = selection,
            isCurrentScreenSource = isCurrentScreen,
            mostRecentlySelectedSavedPaymentMethodSource = mostRecentlySelectedSavedPaymentMethod,
            paymentMethodsSource = paymentMethods,
            walletsState = walletsState,
            interactor = interactor,
            canRemove = canRemove,
        ).apply {
            runTest {
                testBlock()
            }
        }

        updateSelectionTurbine.ensureAllEventsConsumed()
    }

    private class TestParams(
        val selection: MutableStateFlow<PaymentSelection?>,
        val updateSelectionTurbine: Turbine<Boolean>,
        val processingSource: MutableStateFlow<Boolean>,
        val temporarySelectionSource: MutableStateFlow<PaymentMethodCode?>,
        val selectionSource: MutableStateFlow<PaymentSelection?>,
        val isCurrentScreenSource: MutableStateFlow<Boolean>,
        val mostRecentlySelectedSavedPaymentMethodSource: MutableStateFlow<PaymentMethod?>,
        val paymentMethodsSource: MutableStateFlow<List<PaymentMethod>>,
        val walletsState: MutableStateFlow<WalletsState?>,
        val canRemove: MutableStateFlow<Boolean>,
        val interactor: PaymentMethodVerticalLayoutInteractor,
    )
}
