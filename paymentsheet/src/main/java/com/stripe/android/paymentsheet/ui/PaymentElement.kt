package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkInlineSignup
import com.stripe.android.link.ui.inline.LinkOptionalInlineSignup
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountForm
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Provider

@Composable
internal fun PaymentElement(
    formViewModelSubComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
    enabled: Boolean,
    supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
    selectedItem: LpmRepository.SupportedPaymentMethod,
    linkSignupMode: LinkSignupMode?,
    linkConfigurationCoordinator: LinkConfigurationCoordinator?,
    showCheckboxFlow: Flow<Boolean>,
    onItemSelectedListener: (LpmRepository.SupportedPaymentMethod) -> Unit,
    onLinkSignupStateChanged: (LinkConfiguration, InlineSignupViewState) -> Unit,
    formArguments: FormArguments,
    usBankAccountFormArguments: USBankAccountFormArguments,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
) {
    // The PaymentMethodForm has a reference to a FormViewModel, which is scoped to a key. This is to ensure that
    // the FormViewModel is recreated when the PaymentElement is recomposed.
    val uuid = rememberSaveable { UUID.randomUUID().toString() }

    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }

    val horizontalPadding = dimensionResource(
        id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (supportedPaymentMethods.size > 1) {
            PaymentMethodsUI(
                selectedIndex = supportedPaymentMethods.indexOf(selectedItem),
                isEnabled = enabled,
                paymentMethods = supportedPaymentMethods,
                onItemSelectedListener = onItemSelectedListener,
                imageLoader = imageLoader,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        Box(modifier = Modifier.animateContentSize()) {
            if (selectedItem.code == PaymentMethod.Type.USBankAccount.code) {
                USBankAccountForm(
                    formArgs = formArguments,
                    usBankAccountFormArgs = usBankAccountFormArguments,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            } else {
                PaymentMethodForm(
                    uuid = uuid,
                    args = formArguments,
                    enabled = enabled,
                    onFormFieldValuesChanged = onFormFieldValuesChanged,
                    showCheckboxFlow = showCheckboxFlow,
                    formViewModelSubComponentBuilderProvider = formViewModelSubComponentBuilderProvider,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
            }
        }

        if (linkConfigurationCoordinator != null && linkSignupMode != null) {
            when (linkSignupMode) {
                LinkSignupMode.InsteadOfSaveForFutureUse -> {
                    LinkInlineSignup(
                        linkConfigurationCoordinator = linkConfigurationCoordinator,
                        enabled = enabled,
                        onStateChanged = onLinkSignupStateChanged,
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding, vertical = 6.dp)
                            .fillMaxWidth(),
                    )
                }
                LinkSignupMode.AlongsideSaveForFutureUse -> {
                    LinkOptionalInlineSignup(
                        linkConfigurationCoordinator = linkConfigurationCoordinator,
                        enabled = enabled,
                        onStateChanged = onLinkSignupStateChanged,
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding, vertical = 6.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}
