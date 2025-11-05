package com.stripe.android.link.onramp.ui

import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.LinkAppBar
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.DateOfBirth
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

@Composable
@Suppress("LongMethod")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun OnrampKycRefreshScreen(
    appearance: LinkAppearance?,
    kycInfo: VerifyKYCInfo,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onConfirm: () -> Unit
) {
    val name = "${kycInfo.firstName} ${kycInfo.lastName}"
    val dob = getLocalizedDob(kycInfo.dateOfBirth)
    val ssnLast4 = kycInfo.idNumberLastFour ?: ""
    val address = kycInfo.address.formattedAddress()
    val sheetState = rememberStripeBottomSheetState()
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            action()
        }
    }

    ElementsBottomSheetLayout(
        state = sheetState,
        onDismissed = { dismissThen(onClose) }
    ) {
        DefaultLinkTheme(appearance = appearance) {
            BackHandler {
                dismissThen(onClose)
            }

            val buttonVerticalSpace = LinkTheme.shapes.primaryButtonHeight + 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LinkTheme.colors.surfacePrimary)
                    .padding(top = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
                LinkAppBar(
                    state = LinkAppBarState(
                        showHeader = true,
                        canNavigateBack = false,
                        title = null,
                        isElevated = false
                    ),
                    onBackPressed = { dismissThen(onClose) },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(
                        id = R.string.stripe_link_onramp_kyc_verification_screen_title
                    ),
                    style = LinkTheme.typography.title,
                    color = LinkTheme.colors.textPrimary,
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = buttonVerticalSpace)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LinkTheme.colors.surfaceSecondary)
                            .verticalScroll(rememberScrollState())
                    ) {
                        InfoRow(
                            title = stringResource(
                                id = R.string.stripe_link_onramp_kyc_verification_name_field_title
                            ),
                            value = name
                        )
                        KycDivider()
                        InfoRow(
                            title = stringResource(
                                id = R.string.stripe_link_onramp_kyc_verification_dob_field_title
                            ),
                            value = dob
                        )
                        KycDivider()
                        InfoRow(
                            title = stringResource(
                                id = R.string.stripe_link_onramp_kyc_verification_ssn_field_title
                            ),
                            value = ssnLast4
                        )
                        KycDivider()
                        InfoRow(
                            title = stringResource(
                                id = R.string.stripe_link_onramp_kyc_verification_address_field_title
                            ),
                            value = address,
                            icon = {
                                val iconTint = LinkTheme.colors.iconPrimary

                                Icon(
                                    painter = painterResource(id = R.drawable.stripe_ic_edit_outlined_symbol),
                                    contentDescription = "Edit Address",
                                    tint = iconTint,
                                    modifier = Modifier
                                        .height(12.dp)
                                        .width(12.dp)
                                )
                            },
                            onIconTap = { dismissThen(onEdit) }
                        )
                    }

                    PrimaryButton(
                        label = stringResource(R.string.stripe_link_onramp_kyc_verification_confirm_button_text),
                        state = PrimaryButtonState.Enabled,
                        onButtonClick = { dismissThen(onConfirm) },
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun KycDivider() {
    Divider(color = LinkTheme.colors.textPrimary.copy(alpha = 0.12f))
}

@Composable
private fun InfoRow(
    title: String,
    value: String,
    icon: (@Composable () -> Unit)? = null,
    onIconTap: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .padding(start = 16.dp)
            .padding(end = if (icon == null) 16.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textTertiary
            )
            Text(
                text = value,
                style = LinkTheme.typography.body,
                color = LinkTheme.colors.textPrimary
            )
        }

        if (icon != null) {
            IconButton(
                onClick = onIconTap ?: {},
                content = { icon() },
                modifier = Modifier
                    .padding(start = 16.dp)
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class VerifyKYCInfo(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: DateOfBirth,
    val idNumberLastFour: String?,
    val address: PaymentSheet.Address
)

@Composable
private fun getLocalizedDob(dateOfBirth: DateOfBirth): String {
    val context = LocalContext.current
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale
    }

    val calendar = Calendar.getInstance().apply {
        set(dateOfBirth.year, dateOfBirth.month - 1, dateOfBirth.day)
    }

    val pattern = DateFormat.getBestDateTimePattern(locale, "ddMMyyyy")
    return SimpleDateFormat(pattern, locale).format(calendar.time)
}

private fun PaymentSheet.Address.formattedAddress(): String {
    return listOf(
        line1,
        line2,
        city,
        state,
        country,
        postalCode
    )
        .map { it?.trim() }
        .filter { !it.isNullOrEmpty() }
        .joinToString(", ")
}
