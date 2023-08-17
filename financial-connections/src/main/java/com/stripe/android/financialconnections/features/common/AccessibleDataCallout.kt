@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.Permissions
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.uicore.image.StripeImage

private const val COLLAPSE_ACCOUNTS_THRESHOLD = 5

@Composable
internal fun AccessibleDataCallout(
    model: AccessibleDataCalloutModel,
    onLearnMoreClick: () -> Unit
) {
    AccessibleDataCalloutBox {
        AccessibleDataText(
            model = model,
            onLearnMoreClick = onLearnMoreClick
        )
    }
}

@Composable
internal fun AccessibleDataCalloutWithAccounts(
    model: AccessibleDataCalloutModel,
    institution: FinancialConnectionsInstitution,
    accounts: List<PartnerAccount>,
    onLearnMoreClick: () -> Unit
) {
    AccessibleDataCalloutBox {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (accounts.count() >= COLLAPSE_ACCOUNTS_THRESHOLD) {
                AccountRow(
                    iconUrl = institution.icon?.default,
                    text = institution.name,
                    subText = stringResource(
                        id = R.string.stripe_success_infobox_accounts,
                        accounts.count()
                    )
                )
            } else {
                accounts.forEach {
                    AccountRow(
                        iconUrl = institution.icon?.default,
                        text = listOfNotNull(
                            it.name,
                            it.redactedAccountNumbers
                        ).joinToString(" ")
                    )
                }
            }

            Divider(color = colors.backgroundBackdrop)
            AccessibleDataText(
                model = model,
                onLearnMoreClick = onLearnMoreClick
            )
        }
    }
}

@Composable
private fun AccountRow(
    text: String,
    subText: String? = null,
    iconUrl: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
            when {
                iconUrl.isNullOrEmpty() -> InstitutionPlaceholder(modifier)
                else -> StripeImage(
                    url = iconUrl,
                    imageLoader = LocalImageLoader.current,
                    contentDescription = null,
                    modifier = modifier,
                    errorContent = { InstitutionPlaceholder(modifier) }
                )
            }
            Text(
                text,
                style = typography.captionTightEmphasized,
                color = colors.textSecondary
            )
        }
        if (subText != null) {
            Text(
                subText,
                style = typography.captionTightEmphasized,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun AccessibleDataText(
    model: AccessibleDataCalloutModel,
    onLearnMoreClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val permissionsReadable = remember(model.permissions) { model.permissions.toStringRes() }
    AnnotatedText(
        text = TextResource.StringId(
            value = when {
                model.isNetworking -> when (model.businessName) {
                    null -> R.string.stripe_data_accessible_callout_through_link_no_business
                    else -> R.string.stripe_data_accessible_callout_through_link
                }

                model.isStripeDirect -> R.string.stripe_data_accessible_callout_stripe_direct

                else -> when (model.businessName) {
                    null -> R.string.stripe_data_accessible_callout_through_stripe_no_business
                    else -> R.string.stripe_data_accessible_callout_through_stripe
                }
            },
            args = listOfNotNull(
                model.businessName,
                readableListOfPermissions(permissionsReadable)
            )
        ),
        onClickableTextClick = {
            uriHandler.openUri(model.dataPolicyUrl)
            onLearnMoreClick()
        },
        defaultStyle = typography.caption.copy(
            color = colors.textSecondary
        ),
        annotationStyles = mapOf(
            StringAnnotation.CLICKABLE to typography.captionEmphasized
                .toSpanStyle()
                .copy(color = colors.textBrand),
            StringAnnotation.BOLD to typography.captionEmphasized
                .toSpanStyle()
                .copy(color = colors.textSecondary)
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
            .clip(shape = RoundedCornerShape(8.dp))
            .background(color = colors.backgroundContainer)
            .padding(12.dp),
        content = content
    )
}

@Composable
private fun readableListOfPermissions(permissionsReadable: List<Int>): String =
    permissionsReadable
        // TODO@carlosmuvi localize enumeration of permissions once more languages are supported.
        .map { stringResource(id = it) }
        .foldIndexed("") { index, current, arg ->
            when {
                index == 0 -> arg.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }

                permissionsReadable.lastIndex == index -> "$current and $arg"
                else -> "$current, $arg"
            }
        }

private fun List<Permissions>.toStringRes(): List<Int> = mapNotNull {
    when (it) {
        Permissions.BALANCES -> R.string.stripe_data_accessible_type_balances
        Permissions.OWNERSHIP -> R.string.stripe_data_accessible_type_ownership
        Permissions.PAYMENT_METHOD,
        Permissions.ACCOUNT_NUMBERS -> R.string.stripe_data_accessible_type_accountdetails

        Permissions.TRANSACTIONS -> R.string.stripe_data_accessible_type_transactions
        Permissions.UNKNOWN -> null
    }
}.distinct()

internal data class AccessibleDataCalloutModel(
    val businessName: String?,
    val permissions: List<Permissions>,
    val isStripeDirect: Boolean,
    val isNetworking: Boolean,
    val dataPolicyUrl: String
)

@Preview(
    group = "Data Callout",
    name = "Default"
)
@Composable
internal fun AccessibleDataCalloutPreview() {
    FinancialConnectionsPreview {
        AccessibleDataCallout(
            AccessibleDataCalloutModel(
                businessName = "My business",
                permissions = listOf(
                    Permissions.PAYMENT_METHOD,
                    Permissions.BALANCES,
                    Permissions.OWNERSHIP,
                    Permissions.TRANSACTIONS,
                    Permissions.ACCOUNT_NUMBERS
                ),
                isNetworking = false,
                isStripeDirect = false,
                dataPolicyUrl = ""
            ),
            onLearnMoreClick = {}
        )
    }
}

@Preview(
    group = "Data Callout",
    name = "Many Accounts"
)
@Composable
@Suppress("LongMethod")
internal fun AccessibleDataCalloutWithManyAccountsPreview() {
    FinancialConnectionsPreview {
        AccessibleDataCalloutWithAccounts(
            AccessibleDataCalloutModel(
                businessName = "My business",
                permissions = listOf(
                    Permissions.PAYMENT_METHOD,
                    Permissions.BALANCES,
                    Permissions.OWNERSHIP,
                    Permissions.TRANSACTIONS
                ),
                isStripeDirect = false,
                isNetworking = false,
                dataPolicyUrl = ""
            ),
            accounts = partnerAccountsForPreview(),
            institution = FinancialConnectionsInstitution(
                id = "id",
                name = "name",
                url = "url",
                featured = true,
                icon = null,
                logo = null,
                featuredOrder = null,
                mobileHandoffCapable = false
            ),
            onLearnMoreClick = {}
        )
    }
}

@Preview(
    group = "Data Callout",
    name = "Many Accounts and Stripe Direct"
)
@Composable
@Suppress("LongMethod")
internal fun AccessibleDataCalloutStripeDirectPreview() {
    FinancialConnectionsPreview {
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
                isNetworking = false,
                dataPolicyUrl = ""
            ),
            accounts = partnerAccountsForPreview(),
            institution = FinancialConnectionsInstitution(
                id = "id",
                name = "name",
                url = "url",
                featured = true,
                icon = null,
                logo = null,
                featuredOrder = null,
                mobileHandoffCapable = false
            ),
            onLearnMoreClick = {}
        )
    }
}

@Preview(
    group = "Data Callout",
    name = "Networking"
)
@Composable
@Suppress("LongMethod")
internal fun AccessibleDataCalloutNetworkingPreview() {
    FinancialConnectionsPreview {
        AccessibleDataCalloutWithAccounts(
            AccessibleDataCalloutModel(
                businessName = "My business",
                permissions = listOf(
                    Permissions.PAYMENT_METHOD,
                    Permissions.BALANCES,
                    Permissions.OWNERSHIP,
                    Permissions.TRANSACTIONS
                ),
                isStripeDirect = false,
                isNetworking = true,
                dataPolicyUrl = ""
            ),
            accounts = partnerAccountsForPreview(),
            institution = FinancialConnectionsInstitution(
                id = "id",
                name = "name",
                url = "url",
                featured = true,
                icon = null,
                logo = null,
                featuredOrder = null,
                mobileHandoffCapable = false
            ),
            onLearnMoreClick = {}
        )
    }
}

@Preview(
    group = "Data Callout",
    name = "Multiple accounts"
)
@Composable
internal fun AccessibleDataCalloutWithMultipleAccountsPreview() {
    FinancialConnectionsPreview {
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
                isNetworking = false,
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
                    _allowSelection = true,
                    allowSelectionMessage = "",
                    supportedPaymentMethodTypes = emptyList()
                ),
                PartnerAccount(
                    authorization = "Authorization",
                    category = FinancialConnectionsAccount.Category.CASH,
                    id = "id2",
                    name = "Account 2 - no acct numbers",
                    subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
                    _allowSelection = true,
                    allowSelectionMessage = "",
                    supportedPaymentMethodTypes = emptyList()
                )
            ),
            institution = FinancialConnectionsInstitution(
                id = "id",
                name = "name",
                url = "url",
                featured = true,
                icon = null,
                logo = null,
                featuredOrder = null,
                mobileHandoffCapable = false
            ),
            onLearnMoreClick = {}
        )
    }
}

@Preview(
    group = "Data Callout",
    name = "One account"
)
@Composable
internal fun AccessibleDataCalloutWithOneAccountPreview() {
    FinancialConnectionsPreview {
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
                isNetworking = false,
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
                    _allowSelection = true,
                    allowSelectionMessage = "",
                    supportedPaymentMethodTypes = emptyList()
                )
            ),
            institution = FinancialConnectionsInstitution(
                id = "id",
                name = "name",
                url = "url",
                featured = true,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            onLearnMoreClick = {}
        )
    }
}

@Composable
private fun partnerAccountsForPreview() = listOf(
    PartnerAccount(
        authorization = "Authorization",
        institutionName = "Random bank",
        category = FinancialConnectionsAccount.Category.CASH,
        id = "id1",
        name = "Account 1 - no acct numbers",
        _allowSelection = true,
        allowSelectionMessage = "",
        subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
        supportedPaymentMethodTypes = emptyList()
    ),
    PartnerAccount(
        authorization = "Authorization",
        category = FinancialConnectionsAccount.Category.CASH,
        id = "id2",
        name = "Account 2 - no acct numbers",
        _allowSelection = true,
        allowSelectionMessage = "",
        subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
        supportedPaymentMethodTypes = emptyList()
    ),
    PartnerAccount(
        authorization = "Authorization",
        category = FinancialConnectionsAccount.Category.CASH,
        id = "id3",
        name = "Account 3 - no acct numbers",
        _allowSelection = true,
        allowSelectionMessage = "",
        subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
        supportedPaymentMethodTypes = emptyList()
    ),
    PartnerAccount(
        authorization = "Authorization",
        category = FinancialConnectionsAccount.Category.CASH,
        id = "id4",
        name = "Account 4 - no acct numbers",
        _allowSelection = true,
        allowSelectionMessage = "",
        subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
        supportedPaymentMethodTypes = emptyList()
    ),
    PartnerAccount(
        authorization = "Authorization",
        category = FinancialConnectionsAccount.Category.CASH,
        id = "id5",
        name = "Account 5 - no acct numbers",
        _allowSelection = true,
        allowSelectionMessage = "",
        subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
        supportedPaymentMethodTypes = emptyList()
    )
)
