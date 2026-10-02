package com.stripe.android.paymentelement.embedded.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.AddPaymentMethodForm
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodIcon
import com.stripe.android.paymentsheet.verticalmode.DisplayablePaymentMethod
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_HEADER_TITLE
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState

internal const val PREFER_FORM_FOOTER_TEST_TAG = "prefer_form_more_payment_methods"
internal const val PREFER_FORM_FOOTER_ICON_TEST_TAG = "prefer_form_payment_method_icon"
internal const val PREFER_FORM_FOOTER_COUNT_TEST_TAG = "prefer_form_payment_method_count"
internal const val PREFER_FORM_REDIRECT_CONFIRMATION_TEST_TAG = "prefer_form_redirect_confirmation"
internal const val PREFER_FORM_REDIRECT_PAYMENT_METHOD_TEST_TAG = "prefer_form_redirect_payment_method"
private val FooterIconWidth = 30.dp
private const val MaxPreviewIcons = 3

@Composable
internal fun PreferFormHeaderUI(enabled: Boolean) {
    val textColor = MaterialTheme.colors.onSurface
    Text(
        text = stringResource(R.string.stripe_wallet_collapsed_payment),
        style = MaterialTheme.typography.h4,
        color = if (enabled) textColor else textColor.copy(alpha = 0.6f),
        modifier = Modifier
            .padding(bottom = 12.dp)
            .testTag(TEST_TAG_HEADER_TITLE),
    )
}

@Composable
internal fun PreferFormUI(
    interactor: AddPaymentMethodInteractor,
    showFooter: Boolean,
    paymentMethodCount: Int,
    onMorePaymentMethods: () -> Unit,
) {
    val state by interactor.state.collectAsState()
    val horizontalPadding = PaddingValues(0.dp)
    AddPaymentMethodForm(
        interactor = interactor,
        horizontalPadding = horizontalPadding,
    )
    if (showFooter) {
        Spacer(Modifier.height(16.dp))
        PreferFormFooter(
            alternatives = state.supportedPaymentMethods.filterNot {
                it.code == state.selectedPaymentMethodCode
            },
            paymentMethodCount = paymentMethodCount,
            enabled = !state.processing,
            onClick = onMorePaymentMethods,
        )
    }
}

@Composable
internal fun PreferFormFooter(
    alternatives: List<SupportedPaymentMethod>,
    paymentMethodCount: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    MorePaymentMethodsFooter(
        paymentMethodCount = paymentMethodCount,
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
    paymentMethodCount: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    MorePaymentMethodsFooter(
        paymentMethodCount = paymentMethodCount,
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
internal fun PreferFormRedirectConfirmation(paymentMethod: DisplayablePaymentMethod) {
    val context = LocalContext.current
    val imageLoader = remember {
        DefaultStripeImageLoader(context.applicationContext)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PREFER_FORM_REDIRECT_CONFIRMATION_TEST_TAG),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PaymentMethodIcon(
                    iconRes = paymentMethod.icon(),
                    iconUrl = paymentMethod.iconUrl(),
                    imageLoader = imageLoader,
                    iconRequiresTinting = paymentMethod.iconRequiresTinting,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag(PREFER_FORM_REDIRECT_PAYMENT_METHOD_TEST_TAG),
                    contentAlignment = Alignment.Center,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = paymentMethod.displayName.resolve(),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.stripe_ic_redirect_desktop),
                    contentDescription = null,
                    tint = MaterialTheme.stripeColors.subtitle,
                    modifier = Modifier.size(width = 48.dp, height = 40.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.stripe_redirect_confirmation),
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.stripeColors.subtitle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MorePaymentMethodsFooter(
    paymentMethodCount: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    icons: @Composable (imageLoader: StripeImageLoader) -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember {
        DefaultStripeImageLoader(context.applicationContext)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PREFER_FORM_FOOTER_TEST_TAG)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stripe_wallet_pay_another_way),
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icons(imageLoader)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                            shape = CircleShape,
                        ),
                ) {
                    Text(
                        text = "+$paymentMethodCount",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
                        modifier = Modifier.testTag(PREFER_FORM_FOOTER_COUNT_TEST_TAG),
                    )
                }
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
