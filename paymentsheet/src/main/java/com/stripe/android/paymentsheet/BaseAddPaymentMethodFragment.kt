package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.Loading
import com.stripe.android.paymentsheet.ui.PaymentElement
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow

@FlowPreview
internal abstract class BaseAddPaymentMethodFragment : Fragment() {
    abstract val sheetViewModel: BaseSheetViewModel

    private lateinit var imageLoader: StripeImageLoader

    private val linkHandler: LinkHandler
        get() = sheetViewModel.linkHandler
    private val linkLauncher: LinkPaymentLauncher
        get() = linkHandler.linkLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageLoader = StripeImageLoader(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        val showCheckboxFlow = MutableStateFlow(false)

        setContent {
            PaymentsTheme {
                AddPaymentMethod(showCheckboxFlow)
            }
        }
    }

    @Composable
    internal fun AddPaymentMethod(
        showCheckboxFlow: MutableStateFlow<Boolean>
    ) {
        val isRepositoryReady by sheetViewModel.isResourceRepositoryReady.observeAsState()
        val processing by sheetViewModel.processing.collectAsState(false)

        val linkConfig by linkHandler.linkConfiguration.collectAsState()
        val linkAccountStatus by linkConfig?.let {
            linkLauncher.getAccountStatusFlow(it).collectAsState(null)
        } ?: mutableStateOf(null)

        if (isRepositoryReady == true) {
            var selectedPaymentMethodCode: String by rememberSaveable {
                mutableStateOf(getInitiallySelectedPaymentMethodType())
            }

            val selectedItem = remember(selectedPaymentMethodCode) {
                sheetViewModel.supportedPaymentMethods.first {
                    it.code == selectedPaymentMethodCode
                }
            }

            val showLinkInlineSignup = showLinkInlineSignupView(
                selectedPaymentMethodCode,
                linkAccountStatus
            )

            val arguments = remember(selectedItem, showLinkInlineSignup) {
                sheetViewModel.createFormArguments(selectedItem, showLinkInlineSignup)
            }

            LaunchedEffect(arguments) {
                showCheckboxFlow.emit(arguments.showCheckbox)
            }

            val paymentSelection by sheetViewModel.selection.observeAsState()
            val linkInlineSelection by linkHandler.linkInlineSelection.collectAsState()
            var linkSignupState by remember {
                mutableStateOf<InlineSignupViewState?>(null)
            }

            LaunchedEffect(paymentSelection, linkSignupState, linkInlineSelection) {
                val state = linkSignupState
                if (state != null) {
                    onLinkSignupStateChanged(linkConfig!!, state, paymentSelection)
                } else if (linkInlineSelection != null) {
                    (paymentSelection as? PaymentSelection.New.Card)?.let {
                        sheetViewModel.updatePrimaryButtonUIState(
                            PrimaryButton.UIState(
                                label = null,
                                onClick = {
                                    sheetViewModel.payWithLinkInline(
                                        linkConfig!!,
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

            Column(modifier = Modifier.fillMaxWidth()) {
                PaymentElement(
                    sheetViewModel = sheetViewModel,
                    enabled = !processing,
                    supportedPaymentMethods = sheetViewModel.supportedPaymentMethods,
                    selectedItem = selectedItem,
                    showLinkInlineSignup = showLinkInlineSignup,
                    linkPaymentLauncher = linkLauncher,
                    showCheckboxFlow = showCheckboxFlow,
                    onItemSelectedListener = { selectedLpm ->
                        if (selectedItem != selectedLpm) {
                            sheetViewModel.updatePrimaryButtonUIState(null)
                            selectedPaymentMethodCode = selectedLpm.code
                        }
                    },
                    onLinkSignupStateChanged = { _, inlineSignupViewState ->
                        linkSignupState = inlineSignupViewState
                    },
                    formArguments = arguments,
                    onFormFieldValuesChanged = { formValues ->
                        sheetViewModel.updateSelection(
                            transformToPaymentSelection(
                                formValues,
                                selectedItem
                            )
                        )
                    }
                )
            }
        } else {
            Loading()
        }
    }

    private fun getInitiallySelectedPaymentMethodType() =
        when (val selection = sheetViewModel.newPaymentSelection) {
            is PaymentSelection.New.LinkInline -> PaymentMethod.Type.Card.code
            is PaymentSelection.New.Card,
            is PaymentSelection.New.USBankAccount,
            is PaymentSelection.New.GenericPaymentMethod ->
                selection.paymentMethodCreateParams.typeCode
            else -> sheetViewModel.supportedPaymentMethods.first().code
        }

    private fun showLinkInlineSignupView(
        paymentMethodCode: String,
        linkAccountStatus: AccountStatus?
    ): Boolean {
        val validStatusStates = setOf(
            AccountStatus.NeedsVerification,
            AccountStatus.VerificationStarted,
            AccountStatus.SignedOut
        )
        val linkInlineSelectionValid = sheetViewModel.linkHandler.linkInlineSelection.value != null
        return sheetViewModel.linkHandler.isLinkEnabled.value && sheetViewModel.stripeIntent.value
            ?.linkFundingSources?.contains(PaymentMethod.Type.Card.code) == true &&
            paymentMethodCode == PaymentMethod.Type.Card.code &&
            (linkAccountStatus in validStatusStates || linkInlineSelectionValid)
    }

    private fun onLinkSignupStateChanged(
        config: LinkPaymentLauncher.Configuration,
        viewState: InlineSignupViewState,
        paymentSelection: PaymentSelection?
    ) {
        sheetViewModel.updatePrimaryButtonUIState(
            if (viewState.useLink) {
                val userInput = viewState.userInput
                if (userInput != null &&
                    paymentSelection != null
                ) {
                    PrimaryButton.UIState(
                        label = null,
                        onClick = {
                            sheetViewModel.payWithLinkInline(
                                config,
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
    }

    @VisibleForTesting
    internal fun transformToPaymentSelection(
        formFieldValues: FormFieldValues?,
        selectedPaymentMethodResources: LpmRepository.SupportedPaymentMethod
    ) = formFieldValues?.let {
        FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
            formFieldValues.fieldValuePairs
                .filterNot { entry ->
                    entry.key == IdentifierSpec.SaveForFutureUse ||
                        entry.key == IdentifierSpec.CardBrand
                },
            selectedPaymentMethodResources.code,
            selectedPaymentMethodResources.requiresMandate
        ).run {
            if (selectedPaymentMethodResources.code == PaymentMethod.Type.Card.code) {
                PaymentSelection.New.Card(
                    paymentMethodCreateParams = this,
                    brand = CardBrand.fromCode(
                        formFieldValues.fieldValuePairs[IdentifierSpec.CardBrand]?.value
                    ),
                    customerRequestedSave = formFieldValues.userRequestedReuse

                )
            } else {
                PaymentSelection.New.GenericPaymentMethod(
                    getString(selectedPaymentMethodResources.displayNameResource),
                    selectedPaymentMethodResources.iconResource,
                    selectedPaymentMethodResources.lightThemeIconUrl,
                    selectedPaymentMethodResources.darkThemeIconUrl,
                    this,
                    customerRequestedSave = formFieldValues.userRequestedReuse
                )
            }
        }
    }
}
