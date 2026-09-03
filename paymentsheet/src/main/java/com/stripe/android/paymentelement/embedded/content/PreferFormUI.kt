package com.stripe.android.paymentelement.embedded.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.AddPaymentMethodForm
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodIcon
import com.stripe.android.paymentsheet.verticalmode.DisplayablePaymentMethod
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormHeaderUI
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeFormInsets
import com.stripe.android.uicore.utils.collectAsState

internal const val PREFER_FORM_FOOTER_TEST_TAG = "prefer_form_more_payment_methods"
internal const val PREFER_FORM_FOOTER_ICON_TEST_TAG = "prefer_form_payment_method_icon"
private val FooterIconWidth = 30.dp
private const val MaxPreviewIcons = 3

@Composable
internal fun PreferFormUI(
    interactor: AddPaymentMethodInteractor,
    showFooter: Boolean,
    onMorePaymentMethods: () -> Unit,
) {
    val state by interactor.state.collectAsState()
    state.supportedPaymentMethods
        .firstOrNull { it.code == state.selectedPaymentMethodCode }
        ?.let { paymentMethod ->
            VerticalModeFormHeaderUI(
                isEnabled = !state.processing,
                formHeaderInformation = paymentMethod.asFormHeaderInformation(state.incentive),
            )
        }
    AddPaymentMethodForm(interactor = interactor)
    if (showFooter) {
        Spacer(Modifier.height(16.dp))
        PreferFormFooter(
            alternatives = state.supportedPaymentMethods.filterNot {
                it.code == state.selectedPaymentMethodCode
            },
            enabled = !state.processing,
            onClick = onMorePaymentMethods,
        )
    }
}

@Composable
internal fun PreferFormFooter(
    alternatives: List<SupportedPaymentMethod>,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    MorePaymentMethodsFooter(
        enabled = enabled,
        onClick = onClick,
    ) { imageLoader ->
        alternatives.take(MaxPreviewIcons).forEach { paymentMethod ->
            PaymentMethodIcon(
                iconRes = paymentMethod.icon(),
                iconUrl = paymentMethod.iconUrl(),
                imageLoader = imageLoader,
                iconRequiresTinting = paymentMethod.iconRequiresTinting,
                modifier = Modifier
                    .size(width = FooterIconWidth, height = 20.dp)
                    .testTag(PREFER_FORM_FOOTER_ICON_TEST_TAG),
                contentAlignment = Alignment.Center,
            )
        }
    }
}

@Composable
internal fun VerticalModeMorePaymentMethodsFooter(
    alternatives: List<DisplayablePaymentMethod>,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    MorePaymentMethodsFooter(
        enabled = enabled,
        onClick = onClick,
    ) { imageLoader ->
        alternatives.take(MaxPreviewIcons).forEach { paymentMethod ->
            PaymentMethodIcon(
                iconRes = paymentMethod.icon(),
                iconUrl = paymentMethod.iconUrl(),
                imageLoader = imageLoader,
                iconRequiresTinting = paymentMethod.iconRequiresTinting,
                modifier = Modifier
                    .size(width = FooterIconWidth, height = 20.dp)
                    .testTag(PREFER_FORM_FOOTER_ICON_TEST_TAG),
                contentAlignment = Alignment.Center,
            )
        }
    }
}

@Composable
private fun MorePaymentMethodsFooter(
    enabled: Boolean,
    onClick: () -> Unit,
    icons: @Composable (imageLoader: StripeImageLoader) -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember {
        DefaultStripeImageLoader(context.applicationContext)
    }
    val formInsets = MaterialTheme.stripeFormInsets.getOuterFormInsets()
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        modifier = Modifier
            .padding(formInsets)
            .fillMaxWidth()
            .testTag(PREFER_FORM_FOOTER_TEST_TAG)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stripe_more_payment_methods),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                icons(imageLoader)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_ctil_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
