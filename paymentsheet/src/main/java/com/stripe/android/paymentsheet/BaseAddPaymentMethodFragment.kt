package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkInlineSignup
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.paymentsheet.forms.FormController
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.model.getPMAddForm
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.ACHText
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountScreen
import com.stripe.android.paymentsheet.ui.Loading
import com.stripe.android.paymentsheet.ui.PaymentMethodForm
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseElementUI
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@FlowPreview
internal abstract class BaseAddPaymentMethodFragment : Fragment() {
    abstract val viewModelFactory: ViewModelProvider.Factory
    abstract val sheetViewModel: BaseSheetViewModel<*>

    val collectBankAccountLauncher = CollectBankAccountLauncher.create(
        this,
        ::handleCollectBankAccountResult
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        val argumentsFlow = MutableStateFlow<FormFragmentArguments?>(null)

        setContent {
            val isRepositoryReady by sheetViewModel.isResourceRepositoryReady.observeAsState(false)

            if (isRepositoryReady == true) {
                val processing by sheetViewModel.processing.observeAsState(false)
                val linkConfig by sheetViewModel.linkConfiguration.observeAsState()
                val linkAccountStatus by linkConfig?.let {
                    sheetViewModel.linkLauncher.getAccountStatusFlow(it).collectAsState(null)
                } ?: mutableStateOf(null)

                val formController = remember {
                    FormController(
                        requireContext(),
                        argumentsFlow,
                        sheetViewModel.lpmResourceRepository,
                        sheetViewModel.addressResourceRepository
                    )
                }

                var selectedPaymentMethodCode: String by rememberSaveable {
                    mutableStateOf(
                        sheetViewModel.newPaymentSelection?.paymentMethodCreateParams?.typeCode
                            ?: sheetViewModel.supportedPaymentMethods.first().code
                    )
                }
                val selectedItem = remember(selectedPaymentMethodCode) {
                    sheetViewModel.supportedPaymentMethods.first {
                        it.code == selectedPaymentMethodCode
                    }
                }

                val showLinkInlineSignup = sheetViewModel.isLinkEnabled.value == true &&
                    sheetViewModel.stripeIntent.value
                        ?.linkFundingSources?.contains(PaymentMethod.Type.Card.code) == true &&
                    selectedItem.code == PaymentMethod.Type.Card.code &&
                    linkAccountStatus == AccountStatus.SignedOut

                LaunchedEffect(selectedItem, showLinkInlineSignup) {
                    argumentsFlow.emit(
                        createFormArguments(selectedItem, showLinkInlineSignup)
                    )
                }

                val formValues by formController.completeFormValues.collectAsState(null)
                LaunchedEffect(formValues) {
                    sheetViewModel.updateSelection(
                        transformToPaymentSelection(
                            formValues,
                            selectedItem
                        )
                    )
                }

                PaymentsTheme {
                    PaymentElement(
                        formController = formController,
                        enabled = !processing,
                        supportedPaymentMethods = sheetViewModel.supportedPaymentMethods,
                        selectedItem = selectedItem,
                        onItemSelectedListener = { selectedLpm ->
                            if (selectedItem != selectedLpm) {
                                sheetViewModel.updatePrimaryButtonUIState(null)
                                selectedPaymentMethodCode = selectedLpm.code
                            }
                        },
                        showLinkInlineSignup = showLinkInlineSignup,
                        linkPaymentLauncher = sheetViewModel.linkLauncher,
                        onLinkSignupStateChanged = ::onLinkSignupStateChanged
                    )
                }
            } else {
                Loading()
            }
        }
    }

    @FlowPreview
    @Composable
    internal fun PaymentElement(
        formController: FormController,
        enabled: Boolean,
        supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
        selectedItem: LpmRepository.SupportedPaymentMethod,
        showLinkInlineSignup: Boolean,
        linkPaymentLauncher: LinkPaymentLauncher,
        onItemSelectedListener: (LpmRepository.SupportedPaymentMethod) -> Unit,
        onLinkSignupStateChanged: (LinkPaymentLauncher.Configuration, InlineSignupViewState) -> Unit,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (supportedPaymentMethods.size > 1) {
                PaymentMethodsUI(
                    selectedIndex = supportedPaymentMethods.indexOf(
                        selectedItem
                    ),
                    isEnabled = enabled,
                    paymentMethods = supportedPaymentMethods,
                    onItemSelectedListener = onItemSelectedListener,
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
                )
            }

            if (selectedItem.code == PaymentMethod.Type.USBankAccount.code) {
                val formArgs by formController.argumentsFlow.collectAsState(null)

                formArgs?.let {
                    USBankAccountForm(it)
                }
            } else {
                PaymentMethodForm(
                    formController = formController,
                    enabled = enabled,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            if (showLinkInlineSignup) {
                LinkInlineSignup(
                    linkPaymentLauncher = linkPaymentLauncher,
                    enabled = enabled,
                    onStateChanged = onLinkSignupStateChanged,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .fillMaxWidth()
                )
            }
        }
    }

    var callback: (CollectBankAccountResult) -> Unit =  {}
    fun handleCollectBankAccountResult(result: CollectBankAccountResult) {
        callback(result)
    }

    @Composable
    fun USBankAccountForm(
        formArgs: FormFragmentArguments
    ) {
        val viewModel: USBankAccountFormViewModel = viewModel(
            factory = USBankAccountFormViewModel.Factory(
                applicationSupplier = { requireActivity().application },
                argsSupplier = {
                    val savedPaymentMethod =
                        sheetViewModel.newPaymentSelection as? PaymentSelection.New.USBankAccount

                    USBankAccountFormViewModel.Args(
                        formArgs = formArgs,
                        isCompleteFlow = sheetViewModel is PaymentSheetViewModel,
                        clientSecret = clientSecret,
                        savedPaymentMethod = savedPaymentMethod,
                        shippingDetails = sheetViewModel.config?.shippingDetails,
                        onConfirmStripeIntent = { params ->
                            (sheetViewModel as? PaymentSheetViewModel)?.confirmStripeIntent(params)
                        },
                        onUpdateSelectionAndFinish = { paymentSelection ->
                            sheetViewModel.updateSelection(paymentSelection)
                            sheetViewModel.onFinish()
                        }
                    )
                },
                owner = this
            )
        )

        val scope = rememberCoroutineScope()

        val primaryButtonState by sheetViewModel.primaryButtonState.observeAsState()
        LaunchedEffect(primaryButtonState) {
            // When the primary button state is StartProcessing or FinishProcessing
            // we should disable the inputs of this form. StartProcessing shows the loading
            // spinner, FinishProcessing shows the checkmark animation
            viewModel.setProcessing(
                primaryButtonState is PrimaryButton.State.StartProcessing ||
                    primaryButtonState is PrimaryButton.State.FinishProcessing
            )
        }

        LaunchedEffect(viewModel) {
            callback = viewModel.setLauncher(collectBankAccountLauncher)

            scope.launch {
                viewModel.requiredFields.collect {
                    sheetViewModel.updatePrimaryButtonUIState(
                        sheetViewModel.primaryButtonUIState.value?.copy(
                            enabled = it
                        )
                    )
                }
            }

            scope.launch {
                viewModel.saveForFutureUse.collect { saved ->
                    updateMandateText(
                        viewModel,
                        if (saved) {
                            getString(
                                R.string.stripe_paymentsheet_ach_save_mandate,
                                viewModel.formattedMerchantName()
                            )
                        } else {
                            ACHText.getContinueMandateText(requireContext())
                        }
                    )
                }
            }
        }

        val currentScreenState by viewModel.currentScreenState.collectAsState()
        val processing by viewModel.processing.collectAsState(false)

        LaunchedEffect(currentScreenState) {
            sheetViewModel.onError(currentScreenState.error)

            val shouldProcess =
                currentScreenState is USBankAccountFormScreenState.NameAndEmailCollection ||
                    completePayment
            val enabled = if (
                currentScreenState is
                    USBankAccountFormScreenState.NameAndEmailCollection
            ) {
                viewModel.requiredFields.value
            } else {
                true
            }

            updatePrimaryButton(
                text = currentScreenState.primaryButtonText,
                onClick = {
                    viewModel.handlePrimaryButtonClick(currentScreenState)
                },
                enabled = enabled,
                shouldProcess = shouldProcess
            )

            updateMandateText(
                viewModel,
                currentScreenState.mandateText
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            USBankAccountScreen(
                currentScreenState = currentScreenState,
                enabled = !processing,
                saveForFutureUseUI = { saveForFutureUsage ->
                    if (formArgs.showCheckbox) {
                        SaveForFutureUseElementUI(
                            true,
                            viewModel.saveForFutureUseElement.apply {
                                this.controller.onValueChange(saveForFutureUsage)
                            }
                        )
                    }
                },
                nameController = viewModel.nameController,
                emailController = viewModel.emailController,
                onConfirm = {
                    viewModel.reset()
                }
            )
        }
    }

    private val completePayment by lazy {
        sheetViewModel is PaymentSheetViewModel
    }

    private val clientSecret by lazy {
        when (val intent = sheetViewModel.stripeIntent.value) {
            is PaymentIntent -> PaymentIntentClientSecret(intent.clientSecret!!)
            is SetupIntent -> SetupIntentClientSecret(intent.clientSecret!!)
            else -> null
        }
    }

    private fun updatePrimaryButton(
        text: String?,
        onClick: () -> Unit,
        shouldProcess: Boolean = true,
        enabled: Boolean = true,
        visible: Boolean = true
    ) {
        sheetViewModel.updatePrimaryButtonState(PrimaryButton.State.Ready)
        sheetViewModel.updatePrimaryButtonUIState(
            PrimaryButton.UIState(
                label = text,
                onClick = {
                    if (shouldProcess) {
                        sheetViewModel.updatePrimaryButtonState(
                            PrimaryButton.State.StartProcessing
                        )
                    }
                    onClick()
                    sheetViewModel.updatePrimaryButtonUIState(
                        sheetViewModel.primaryButtonUIState.value?.copy(
                            onClick = null
                        )
                    )
                },
                enabled = enabled,
                visible = visible
            )
        )
    }

    private fun updateMandateText(
        viewModel: USBankAccountFormViewModel,
        mandateText: String?
    ) {
        val microdepositsText =
            if (
                viewModel.currentScreenState.value
                    is USBankAccountFormScreenState.VerifyWithMicrodeposits
            ) {
                getString(
                    R.string.stripe_paymentsheet_microdeposit,
                    viewModel.formattedMerchantName()
                )
            } else {
                ""
            }
        val updatedText = mandateText?.let {
            """
                $microdepositsText
                
                $mandateText
            """.trimIndent()
        } ?: run { null }
        sheetViewModel.updateBelowButtonText(updatedText)
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

    private fun onLinkSignupStateChanged(
        config: LinkPaymentLauncher.Configuration,
        viewState: InlineSignupViewState
    ) {
        sheetViewModel.updatePrimaryButtonUIState(
            if (viewState.useLink) {
                val userInput = viewState.userInput
                if (userInput != null &&
                    sheetViewModel.selection.value != null
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

    private fun createFormArguments(
        selectedItem: LpmRepository.SupportedPaymentMethod,
        showLinkInlineSignup: Boolean
    ): FormFragmentArguments {
        val layoutFormDescriptor = selectedItem.getPMAddForm(
            requireNotNull(sheetViewModel.stripeIntent.value),
            sheetViewModel.config
        )
        return FormFragmentArguments(
            paymentMethodCode = selectedItem.code,
            showCheckbox = layoutFormDescriptor.showCheckbox && !showLinkInlineSignup,
            showCheckboxControlledFields = sheetViewModel.newPaymentSelection?.let {
                sheetViewModel.newPaymentSelection?.customerRequestedSave ==
                    PaymentSelection.CustomerRequestedSave.RequestReuse
            } ?: layoutFormDescriptor.showCheckboxControlledFields,
            merchantName = sheetViewModel.merchantName,
            amount = sheetViewModel.amount.value,
            billingDetails = sheetViewModel.config?.defaultBillingDetails,
            shippingDetails = sheetViewModel.config?.shippingDetails,
            injectorKey = sheetViewModel.injectorKey,
            initialPaymentMethodCreateParams =
            sheetViewModel.newPaymentSelection?.paymentMethodCreateParams?.typeCode
                ?.takeIf {
                    it == selectedItem.code
                }?.let {
                    when (val selection = sheetViewModel.newPaymentSelection) {
                        is PaymentSelection.New.GenericPaymentMethod ->
                            selection.paymentMethodCreateParams
                        is PaymentSelection.New.Card ->
                            selection.paymentMethodCreateParams
                        else -> null
                    }
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
                    this,
                    customerRequestedSave = formFieldValues.userRequestedReuse
                )
            }
        }
    }
}
