package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.consent.ConsentTextBuilder
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.features.fullName
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.Permissions
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AccessibleDataCallout(
    model: AccessibleDataCalloutModel,
) {
    AccessibleDataCalloutBox {
        AccessibleDataText(model)
    }
}

@Composable
internal fun AccessibleDataCalloutWithAccounts(
    model: AccessibleDataCalloutModel,
    accounts: List<PartnerAccount>
) {
    AccessibleDataCalloutBox {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            accounts.forEach {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        it.fullName,
                        style = FinancialConnectionsTheme.typography.captionEmphasized,
                        color = FinancialConnectionsTheme.colors.textSecondary
                    )
                }
            }
            Divider()
            AccessibleDataText(model)
        }
    }
}

@Composable
private fun AccessibleDataText(
    model: AccessibleDataCalloutModel,
) {
    val uriHandler = LocalUriHandler.current
    val permissionsReadable = remember(model.permissions) { model.permissions.toStringRes() }
    AnnotatedText(
        text = TextResource.StringId(
            value = when (model.isStripeDirect) {
                true -> when (model.businessName) {
                    null -> R.string.data_accessible_callout_through_stripe_no_business
                    else -> R.string.data_accessible_callout_through_stripe
                }
                false -> when (model.businessName) {
                    null -> R.string.data_accessible_callout_no_business
                    else -> R.string.data_accessible_callout
                }
            },
            args = listOfNotNull(
                model.businessName,
                readableListOfPermissions(permissionsReadable)
            )
        ),
        onClickableTextClick = { uriHandler.openUri(model.dataPolicyUrl) },
        defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
            color = FinancialConnectionsTheme.colors.textSecondary
        ),
        annotationStyles = mapOf(
            StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.captionEmphasized
                .toSpanStyle()
                .copy(color = FinancialConnectionsTheme.colors.textBrand),
            StringAnnotation.BOLD to FinancialConnectionsTheme.typography.captionEmphasized
                .toSpanStyle()
                .copy(color = FinancialConnectionsTheme.colors.textSecondary),
        )
    )
}

@Composable
private fun AccessibleDataCalloutBox(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                shape = RoundedCornerShape(8.dp),
                color = FinancialConnectionsTheme.colors.borderDefault,
            )
            .background(color = FinancialConnectionsTheme.colors.backgroundContainer)
            .padding(12.dp),
        content = content
    )
}

@Composable
private fun readableListOfPermissions(permissionsReadable: List<Int>): String =
    permissionsReadable.map {
        stringResource(id = it).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }.foldIndexed("") { index, current, arg ->
        // TODO@carlosmuvi localize enumeration of permissions once more languages are supported.
        when {
            index == 0 -> arg
            permissionsReadable.lastIndex == index -> "$current and $arg"
            else -> "$current, $arg"
        }
    }

private fun List<Permissions>.toStringRes(): List<Int> = mapNotNull {
    when (it) {
        Permissions.BALANCES -> R.string.data_accessible_type_balances
        Permissions.OWNERSHIP -> R.string.data_accessible_type_ownership
        Permissions.PAYMENT_METHOD -> R.string.data_accessible_type_accountdetails
        Permissions.TRANSACTIONS -> R.string.data_accessible_type_transactions
        Permissions.UNKNOWN -> null
    }
}

internal data class AccessibleDataCalloutModel(
    val businessName: String?,
    val permissions: List<Permissions>,
    val isStripeDirect: Boolean,
    val dataPolicyUrl: String
) {
    companion object {
        fun fromManifest(manifest: FinancialConnectionsSessionManifest): AccessibleDataCalloutModel =
            AccessibleDataCalloutModel(
                businessName = ConsentTextBuilder.getBusinessName(manifest),
                permissions = manifest.permissions,
                isStripeDirect = manifest.isStripeDirect ?: false,
                dataPolicyUrl = FinancialConnectionsUrlResolver.getDataPolicyUrl(manifest)
            )
    }
}

@Preview
@Composable
internal fun AccessibleDataCalloutPreview() {
    FinancialConnectionsTheme {
        AccessibleDataCallout(
            AccessibleDataCalloutModel(
                businessName = "My business",
                permissions = listOf(
                    Permissions.PAYMENT_METHOD,
                    Permissions.BALANCES,
                    Permissions.OWNERSHIP,
                    Permissions.TRANSACTIONS
                ),
                isStripeDirect = true,
                dataPolicyUrl = ""
            )
        )
    }
}

@Preview
@Composable
internal fun AccessibleDataCalloutWithMultipleAccountsPreview() {
    FinancialConnectionsTheme {
        AccessibleDataCalloutWithAccounts(
            AccessibleDataCalloutModel(
                businessName = "My business",
                permissions = listOf(
                    Permissions.PAYMENT_METHOD,
                    Permissions.BALANCES,
                    Permissions.OWNERSHIP,
                    Permissions.TRANSACTIONS
                ),
                isStripeDirect = true,
                dataPolicyUrl = ""
            ),
            accounts = listOf(
                PartnerAccount(
                    authorization = "Authorization",
                    institutionName = "Random bank",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id1",
                    name = "Account 1",
                    balanceAmount = 1000,
                    displayableAccountNumbers = "1234",
                    currency = "$",
                    subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                    supportedPaymentMethodTypes = emptyList(),
                ),
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id2",
                    name = "Account 2 - no acct numbers",
                    subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
                    supportedPaymentMethodTypes = emptyList()
                )
            )
        )
    }
}

@Preview
@Composable
internal fun AccessibleDataCalloutWithOneAccountPreview() {
    FinancialConnectionsTheme {
        AccessibleDataCalloutWithAccounts(
            AccessibleDataCalloutModel(
                businessName = "My business",
                permissions = listOf(
                    Permissions.PAYMENT_METHOD,
                    Permissions.BALANCES,
                    Permissions.OWNERSHIP,
                    Permissions.TRANSACTIONS
                ),
                isStripeDirect = true,
                dataPolicyUrl = ""
            ),
            accounts = listOf(
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id1",
                    name = "Account 1",
                    balanceAmount = 1000,
                    displayableAccountNumbers = "1234",
                    currency = "$",
                    subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                    supportedPaymentMethodTypes = emptyList(),
                )
            )
        )
    }
}
