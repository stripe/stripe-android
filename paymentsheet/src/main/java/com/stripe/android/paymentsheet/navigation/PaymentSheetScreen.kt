package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcCompletionState
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionInteractor
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionPaymentSheetScreen
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.EditPaymentMethod
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodTabLayoutUI
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodsTopContentPadding
import com.stripe.android.paymentsheet.ui.SelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodUI
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutUI
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable
import com.stripe.android.R as PaymentsCoreR

internal val formBottomContentPadding = 20.dp
internal val horizontalModeWalletsDividerSpacing = 16.dp
internal val verticalModeWalletsDividerSpacing = 24.dp

internal data class BuyButtonState(
    val visible: Boolean,
    val buyButtonOverride: BuyButtonOverride? = null
) {
    data class BuyButtonOverride(
        val label: ResolvableString,
        val lockEnabled: Boolean
    )
}

internal sealed interface PaymentSheetScreen {

    enum class AnimationStyle {
        /**
         * Does not animate the PrimaryButton (it's either shown or displayed without animation).
         * The primary button is not part of an animation block, so it helps with visual anchoring between screens.
         */
        PrimaryButtonAnchored,

        /**
         * The full page is animated, including the primary button.
         * Helps make it so that pages that aren't expected to have an visual anchor around the primary button to
         * animate more smoothly.
         */
        FullPage,
    }

    val buyButtonState: StateFlow<BuyButtonState>
    val showsContinueButton: Boolean
    val topContentPadding: Dp
    val bottomContentPadding: Dp
    val walletsDividerSpacing: Dp
    val animationStyle: AnimationStyle
        get() = AnimationStyle.FullPage
    val showsMandates: Boolean

    fun topBarState(): StateFlow<PaymentSheetTopBarState?>

    fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?>

    fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean>

    @Composable
    fun Content(modifier: Modifier)

    object Loading : PaymentSheetScreen {

        override val buyButtonState = stateFlowOf(
            BuyButtonState(visible = false)
        )
        override val showsContinueButton: Boolean = false
        override val topContentPadding: Dp = 0.dp
        override val bottomContentPadding: Dp = 0.dp
        override val walletsDividerSpacing: Dp = horizontalModeWalletsDividerSpacing
        override val showsMandates: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(null)
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(null)
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(false)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            BottomSheetLoadingIndicator(modifier)
        }
    }

    class SelectSavedPaymentMethods(
        private val interactor: SelectSavedPaymentMethodsInteractor,
        val cvcRecollectionState: CvcRecollectionState = CvcRecollectionState.NotRequired,
    ) : PaymentSheetScreen, Closeable {

        sealed interface CvcRecollectionState {
            data object NotRequired : CvcRecollectionState
            class Required(val cvcControllerFlow: StateFlow<CvcController>) : CvcRecollectionState
        }

        override val buyButtonState = stateFlowOf(
            BuyButtonState(visible = true)
        )
        override val showsContinueButton: Boolean = false
        override val topContentPadding: Dp = SavedPaymentMethodsTopContentPadding
        override val bottomContentPadding: Dp = 0.dp
        override val walletsDividerSpacing: Dp = horizontalModeWalletsDividerSpacing
        override val animationStyle: AnimationStyle = AnimationStyle.PrimaryButtonAnchored
        override val showsMandates: Boolean = true

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return interactor.state.mapAsStateFlow { state ->
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = false,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Maybe(
                        isEditing = state.isEditing,
                        canEdit = state.canEdit,
                        onEditIconPressed = {
                            interactor.handleViewAction(SelectSavedPaymentMethodsInteractor.ViewAction.ToggleEdit)
                        },
                    ),
                )
            }
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(
                if (isCompleteFlow && isWalletEnabled) {
                    null
                } else {
                    R.string.stripe_paymentsheet_select_your_payment_method.resolvableString
                }
            )
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(isCompleteFlow)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            SavedPaymentMethodTabLayoutUI(
                interactor = interactor,
                cvcRecollectionState = cvcRecollectionState,
                modifier = modifier,
            )
        }

        override fun close() {
            interactor.close()
        }
    }

    class AddAnotherPaymentMethod(
        private val interactor: AddPaymentMethodInteractor,
    ) : PaymentSheetScreen, Closeable {

        override val buyButtonState = stateFlowOf(
            BuyButtonState(visible = true)
        )
        override val showsContinueButton: Boolean = true
        override val topContentPadding: Dp = 0.dp
        override val bottomContentPadding: Dp = formBottomContentPadding
        override val walletsDividerSpacing: Dp = horizontalModeWalletsDividerSpacing
        override val animationStyle: AnimationStyle = AnimationStyle.PrimaryButtonAnchored
        override val showsMandates: Boolean = true

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = true,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return interactor.state.mapAsStateFlow { state ->
                if (isWalletEnabled || isCompleteFlow) {
                    null
                } else {
                    if (state.supportedPaymentMethods.singleOrNull()?.code == PaymentMethod.Type.Card.code) {
                        PaymentsCoreR.string.stripe_title_add_a_card.resolvableString
                    } else {
                        R.string.stripe_paymentsheet_choose_payment_method.resolvableString
                    }
                }
            }
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(isCompleteFlow)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            AddPaymentMethod(interactor = interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    class AddFirstPaymentMethod(
        private val interactor: AddPaymentMethodInteractor,
    ) : PaymentSheetScreen, Closeable {

        override val buyButtonState = stateFlowOf(
            BuyButtonState(visible = true)
        )
        override val showsContinueButton: Boolean = true
        override val topContentPadding: Dp = 0.dp
        override val bottomContentPadding: Dp = formBottomContentPadding
        override val walletsDividerSpacing: Dp = horizontalModeWalletsDividerSpacing
        override val showsMandates: Boolean = true

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = false,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return interactor.state.mapAsStateFlow { state ->
                if (isWalletEnabled) {
                    null
                } else if (isCompleteFlow) {
                    R.string.stripe_paymentsheet_add_payment_method_title.resolvableString
                } else {
                    if (state.supportedPaymentMethods.singleOrNull()?.code == PaymentMethod.Type.Card.code) {
                        PaymentsCoreR.string.stripe_title_add_a_card.resolvableString
                    } else {
                        R.string.stripe_paymentsheet_choose_payment_method.resolvableString
                    }
                }
            }
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(true)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            AddPaymentMethod(interactor = interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    class EditPaymentMethod(
        val interactor: ModifiableEditPaymentMethodViewInteractor,
    ) : PaymentSheetScreen, Closeable {

        override val buyButtonState = stateFlowOf(
            BuyButtonState(visible = false)
        )
        override val showsContinueButton: Boolean = false
        override val topContentPadding: Dp = 0.dp
        override val bottomContentPadding: Dp = 0.dp
        override val walletsDividerSpacing: Dp = horizontalModeWalletsDividerSpacing
        override val showsMandates: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = true,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(PaymentsCoreR.string.stripe_title_update_card.resolvableString)
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(false)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            EditPaymentMethod(interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    class VerticalMode(private val interactor: PaymentMethodVerticalLayoutInteractor) : PaymentSheetScreen {

        override val buyButtonState = stateFlowOf(
            BuyButtonState(visible = true)
        )
        override val showsContinueButton: Boolean = true
        override val topContentPadding: Dp = 0.dp
        override val bottomContentPadding: Dp = formBottomContentPadding
        override val walletsDividerSpacing: Dp = verticalModeWalletsDividerSpacing
        override val showsMandates: Boolean = true

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = false,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(
                if (isWalletEnabled) {
                    null
                } else if (isCompleteFlow) {
                    R.string.stripe_paymentsheet_select_payment_method.resolvableString
                } else {
                    R.string.stripe_paymentsheet_choose_payment_method.resolvableString
                }
            )
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return interactor.showsWalletsHeader
        }

        @Composable
        override fun Content(modifier: Modifier) {
            PaymentMethodVerticalLayoutUI(interactor, modifier.padding(horizontal = 20.dp))
        }
    }

    class VerticalModeForm(
        private val interactor: VerticalModeFormInteractor,
        private val showsWalletHeader: Boolean = false,
    ) : PaymentSheetScreen, Closeable {

        override val buyButtonState = stateFlowOf(
            BuyButtonState(visible = true)
        )
        override val showsContinueButton: Boolean = true
        override val topContentPadding: Dp = 0.dp
        override val bottomContentPadding: Dp = formBottomContentPadding
        override val walletsDividerSpacing: Dp = verticalModeWalletsDividerSpacing
        override val showsMandates: Boolean = true

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = interactor.canGoBack(),
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(null)
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(showsWalletHeader)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            VerticalModeFormUI(interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    class ManageSavedPaymentMethods(private val interactor: ManageScreenInteractor) : PaymentSheetScreen, Closeable {
        override val buyButtonState = stateFlowOf(
            BuyButtonState(visible = false)
        )
        override val showsContinueButton: Boolean = false
        override val topContentPadding: Dp = 0.dp
        override val bottomContentPadding: Dp = 0.dp
        override val walletsDividerSpacing: Dp = verticalModeWalletsDividerSpacing
        override val showsMandates: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return interactor.state.mapAsStateFlow { state ->
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = true,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Maybe(
                        isEditing = state.isEditing,
                        canEdit = state.canEdit,
                        onEditIconPressed = {
                            interactor.handleViewAction(ManageScreenInteractor.ViewAction.ToggleEdit)
                        },
                    ),
                )
            }
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return interactor.state.mapAsStateFlow { state ->
                val title = if (state.isEditing) {
                    R.string.stripe_paymentsheet_manage_payment_methods
                } else {
                    R.string.stripe_paymentsheet_select_your_payment_method
                }

                title.resolvableString
            }
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> =
            stateFlowOf(false)

        @Composable
        override fun Content(modifier: Modifier) {
            ManageScreenUI(interactor = interactor)
        }

        override fun close() {
            interactor.close()
        }
    }

    class CvcRecollection(private val interactor: CvcRecollectionInteractor) : PaymentSheetScreen {
        override val buyButtonState = stateFlowOf(
            BuyButtonState(
                visible = true,
                buyButtonOverride = BuyButtonState.BuyButtonOverride(
                    label = resolvableString(R.string.stripe_paymentsheet_confirm),
                    lockEnabled = false
                )
            )
        )
        override val showsContinueButton: Boolean = false
        override val topContentPadding: Dp = 0.dp
        override val bottomContentPadding: Dp = formBottomContentPadding
        override val walletsDividerSpacing: Dp = verticalModeWalletsDividerSpacing
        override val showsMandates: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return interactor.cvcCompletionState.mapAsStateFlow { complete ->
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = false,
                    isLiveMode = interactor.viewState.value.isTestMode.not(),
                    editable = PaymentSheetTopBarState.Editable.Maybe(
                        isEditing = complete is CvcCompletionState.Incomplete,
                        canEdit = false,
                        onEditIconPressed = {}
                    ),
                )
            }
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean) = stateFlowOf(null)

        override fun showsWalletsHeader(isCompleteFlow: Boolean) = stateFlowOf(false)

        @Composable
        override fun Content(modifier: Modifier) {
            CvcRecollectionPaymentSheetScreen(interactor)
        }
    }

    class UpdatePaymentMethod(
        val interactor: UpdatePaymentMethodInteractor,
    ) : PaymentSheetScreen {
        override val buyButtonState: StateFlow<BuyButtonState> = stateFlowOf(
            BuyButtonState(visible = false)
        )
        override val showsContinueButton: Boolean = false
        override val topContentPadding: Dp = 0.dp
        override val bottomContentPadding: Dp = 0.dp
        override val walletsDividerSpacing: Dp = verticalModeWalletsDividerSpacing
        override val showsMandates: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = true,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(interactor.screenTitle)
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> = stateFlowOf(false)

        @Composable
        override fun Content(modifier: Modifier) {
            UpdatePaymentMethodUI(interactor, modifier)
        }
    }
}
