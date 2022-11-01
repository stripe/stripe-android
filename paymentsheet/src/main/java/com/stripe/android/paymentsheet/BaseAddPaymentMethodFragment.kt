package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.PaymentElementConfig
import com.stripe.android.elements.PaymentElementViewModel
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkInlineSignedIn
import com.stripe.android.link.ui.inline.LinkInlineSignup
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.Loading
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.PaymentsTheme
import kotlinx.coroutines.FlowPreview

@FlowPreview
internal abstract class BaseAddPaymentMethodFragment : Fragment() {
    abstract val viewModelFactory: ViewModelProvider.Factory
    abstract val sheetViewModel: BaseSheetViewModel<*>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            val isRepositoryReady by sheetViewModel.isResourceRepositoryReady.observeAsState()

            if (isRepositoryReady == true) {
                val paymentElementViewModel: PaymentElementViewModel = viewModel(
                    factory = PaymentElementViewModel.Factory(
                        supportedPaymentMethods = sheetViewModel.supportedPaymentMethods,
                        paymentElementConfig = PaymentElementConfig(
                            stripeIntent = requireNotNull(sheetViewModel.stripeIntent.value),
                            merchantName = sheetViewModel.merchantName,
                            initialSelection = sheetViewModel.newPaymentSelection,
                            defaultBillingDetails = sheetViewModel.config?.defaultBillingDetails,
                            shippingDetails = sheetViewModel.config?.shippingDetails,
                            hasCustomerConfiguration = sheetViewModel.config?.customer != null,
                            allowsDelayedPaymentMethods = sheetViewModel.config?.allowsDelayedPaymentMethods == true
                        ),
                        context = requireContext()
                    )
                )

                val enabled by sheetViewModel.processing.map { !it }.observeAsState(false)

                PaymentsTheme {
                    AddPaymentMethod(
                        paymentElementViewModel = paymentElementViewModel,
                        enabled = enabled
                    )
                }
            } else {
                Loading()
            }
        }
    }

    @Composable
    internal fun AddPaymentMethod(
        paymentElementViewModel: PaymentElementViewModel,
        enabled: Boolean
    ) {
        var isLinkInlineActive by remember {
            mutableStateOf(sheetViewModel.newPaymentSelection is PaymentSelection.New.LinkInline)
        }
        val selectedItem by paymentElementViewModel.selectedPaymentMethod.collectAsState()
        val arguments by paymentElementViewModel.formArgumentsFlow.collectAsState()
        val paymentSelection by paymentElementViewModel.paymentSelectionFlow.collectAsState()

        // This is how the arguments are shared with the USBankAccountFormFragment, will be
        // removed once USBankAccountFormFragment is refactored out of a Fragment
        (activity as? BaseSheetActivity<*>)?.formArgs = arguments

        val linkConfig by sheetViewModel.linkConfiguration.observeAsState()
        val linkAccountStatus by linkConfig?.let {
            sheetViewModel.linkLauncher.getAccountStatusFlow(it).collectAsState(null)
        } ?: mutableStateOf(null)

        val showLinkInlineSignup = showLinkInlineSignupView(
            selectedItem.code,
            linkAccountStatus,
            isLinkInlineActive
        )

        LaunchedEffect(paymentSelection) {
            sheetViewModel.updateSelection(paymentSelection)
        }

        LaunchedEffect(selectedItem) {
            if (selectedItem.code != PaymentMethod.Type.USBankAccount.code) {
                sheetViewModel.updatePrimaryButtonUIState(null)
            }
        }

        var linkSignupState by remember { mutableStateOf<InlineSignupViewState?>(null) }

        LaunchedEffect(paymentSelection, linkSignupState, isLinkInlineActive) {
            if (selectedItem.code == PaymentMethod.Type.Link.code) {
                updateButtonState(paymentSelection, linkSignupState, isLinkInlineActive, linkConfig)
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            PaymentElement(
                viewModel = paymentElementViewModel,
                enabled = enabled,
                showCheckbox = arguments.showCheckbox && !showLinkInlineSignup,
                injector = sheetViewModel.injector,
                modifier = Modifier.padding(top = 20.dp)
            )

            if (showLinkInlineSignup) {
                val modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal),
                        vertical = 6.dp
                    )
                if (isLinkInlineActive) {
                    LinkInlineSignedIn(
                        linkPaymentLauncher = sheetViewModel.linkLauncher,
                        onLogout = {
                            isLinkInlineActive = false
                        },
                        modifier = modifier
                    )
                } else {
                    LinkInlineSignup(
                        linkPaymentLauncher = sheetViewModel.linkLauncher,
                        enabled = enabled,
                        onStateChanged = { _, inlineSignupViewState ->
                            linkSignupState = inlineSignupViewState
                        },
                        modifier = modifier
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sheetViewModel.headerText.value =
            getString(R.string.stripe_paymentsheet_add_payment_method_title)

        sheetViewModel.eventReporter.onShowNewPaymentOptionForm(
            linkEnabled = sheetViewModel.isLinkEnabled.value ?: false,
            activeLinkSession = sheetViewModel.activeLinkSession.value ?: false
        )
    }

    private fun showLinkInlineSignupView(
        paymentMethodCode: String,
        linkAccountStatus: AccountStatus?,
        isLinkInlineActive: Boolean
    ): Boolean {
        return sheetViewModel.isLinkEnabled.value == true &&
            sheetViewModel.stripeIntent.value
            ?.linkFundingSources?.contains(PaymentMethod.Type.Card.code) == true &&
            paymentMethodCode == PaymentMethod.Type.Card.code &&
            (
                linkAccountStatus in setOf(
                    AccountStatus.NeedsVerification,
                    AccountStatus.VerificationStarted,
                    AccountStatus.SignedOut
                ) || isLinkInlineActive
                )
    }

    private fun updateButtonState(
        paymentSelection: PaymentSelection?,
        linkSignupState: InlineSignupViewState?,
        isLinkInlineActive: Boolean,
        linkConfig: LinkPaymentLauncher.Configuration?
    ) = linkConfig?.let {
        if (linkSignupState != null) {
            sheetViewModel.updatePrimaryButtonUIState(
                if (linkSignupState.useLink) {
                    val userInput = linkSignupState.userInput
                    if (userInput != null &&
                        paymentSelection != null
                    ) {
                        PrimaryButton.UIState(
                            label = null,
                            onClick = {
                                sheetViewModel.payWithLinkInline(
                                    linkConfig,
                                    userInput
                                )
                            },
                            enabled = true,
                            visible = true
                        )
                    } else {
                        PrimaryButton.UIState(
                            label = null,
                            onClick = null,
                            enabled = false,
                            visible = true
                        )
                    }
                } else {
                    null
                }
            )
        } else if (isLinkInlineActive) {
            (paymentSelection as? PaymentSelection.New.Card)?.let {
                sheetViewModel.updatePrimaryButtonUIState(
                    PrimaryButton.UIState(
                        label = null,
                        onClick = {
                            sheetViewModel.payWithLinkInline(
                                linkConfig,
                                null
                            )
                        },
                        enabled = true,
                        visible = true
                    )
                )
            }
        }
    }
}
