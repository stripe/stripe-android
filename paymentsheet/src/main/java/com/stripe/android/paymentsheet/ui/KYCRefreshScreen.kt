package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.model.DateOfBirth
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R

@Composable
@Suppress("LongMethod")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun KYCRefreshScreen(
    appearance: LinkAppearance?,
    kycInfo: VerifyKYCInfo,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onConfirm: () -> Unit
) {
    val name = "${kycInfo.firstName} ${kycInfo.lastName}"
    val dob = "%02d/%02d/%d".format(kycInfo.dateOfBirth.month, kycInfo.dateOfBirth.day, kycInfo.dateOfBirth.year)
    val ssnLast4 = kycInfo.idNumberLastFour ?: ""
    val address = kycInfo.address.formattedAddress()

    DefaultLinkTheme(appearance = appearance) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinkTheme.colors.surfacePrimary)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                TopNavigationBar(
                    onClose = onClose,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "Confirm your information",
                    style = LinkTheme.typography.title,
                    color = LinkTheme.colors.textPrimary,
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = LinkTheme.colors.surfaceSecondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        InfoRow(title = "Name", value = name)
                        Divider(color = LinkTheme.colors.textPrimary.copy(alpha = 0.12f))
                        InfoRow(title = "Date of Birth", value = dob)
                        Divider(color = LinkTheme.colors.textPrimary.copy(alpha = 0.12f))
                        InfoRow(title = "Last 4 digits of SSN", value = ssnLast4)
                        Divider(color = LinkTheme.colors.textPrimary.copy(alpha = 0.12f))
                        InfoRow(title = "Address", value = address, icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.stripe_ic_kyc_verify_edit_ref),
                                contentDescription = "Edit Address",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .height(18.dp)
                                    .width(18.dp)
                            )
                        }, onIconTap = onEdit)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onConfirm,
                    shape = LinkTheme.shapes.primaryButton,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = LinkTheme.colors.buttonBrand,
                        contentColor = LinkTheme.colors.onButtonBrand,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LinkTheme.shapes.primaryButtonHeight)
                ) {
                    Text(
                        "Confirm",
                        style = LinkTheme.typography.body.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface VerifyKYCInfo {
    val firstName: String
    val lastName: String
    val dateOfBirth: DateOfBirth
    val idNumberLastFour: String?
    val address: PaymentSheet.Address
}

@Composable
private fun TopNavigationBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = getLinkIcon()),
            contentDescription = "Link",
            tint = Color.Unspecified,
            modifier = Modifier
                .height(72.dp)
                .width(88.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onClose,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(LinkTheme.colors.textPrimary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = LinkTheme.colors.textPrimary,
                    modifier = Modifier.align(Alignment.Center)
                        .size(22.dp)
                )
            }
        }
    }
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LinkTheme.typography.caption,
                color = LinkTheme.colors.textSecondary
            )
            Text(
                text = value,
                style = LinkTheme.typography.body,
                color = LinkTheme.colors.textPrimary
            )
        }
        icon?.let {
            IconButton(onClick = onIconTap ?: {}) {
                it()
            }
        }
    }
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
