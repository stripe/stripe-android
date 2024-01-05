@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.Permissions
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun MerchantDataAccessText(
    model: MerchantDataAccessModel,
    onLearnMoreClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val permissionsReadable = remember(model.permissions) { model.permissions.toStringRes() }
    AnnotatedText(
        text = TextResource.StringId(
            value = when {
                model.isStripeDirect -> R.string.stripe_data_accessible_callout_stripe
                else -> when (model.businessName) {
                    null -> R.string.stripe_data_accessible_callout_business
                    else -> R.string.stripe_data_accessible_callout_no_business
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
        defaultStyle = FinancialConnectionsTheme.v3Typography.labelSmall.copy(
            color = FinancialConnectionsTheme.v3Colors.textDefault,
            textAlign = TextAlign.Center
        ),
    )
}

@Composable
private fun readableListOfPermissions(permissionsReadable: List<Int>): String =
    permissionsReadable
        // TODO@carlosmuvi localize enumeration of permissions once more languages are supported.
        .map { stringResource(id = it) }
        .foldIndexed("") { index, current, arg ->
            when {
                index == 0 -> arg
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

internal data class MerchantDataAccessModel(
    val businessName: String?,
    val permissions: List<Permissions>,
    val isStripeDirect: Boolean,
    val dataPolicyUrl: String
)

@Preview(
    group = "Merchant data access text",
    name = "Default"
)
@Composable
internal fun MerchantDataAccessTextPreview() {
    FinancialConnectionsPreview {
        MerchantDataAccessText(
            MerchantDataAccessModel(
                businessName = "My business",
                permissions = listOf(
                    Permissions.PAYMENT_METHOD,
                    Permissions.BALANCES,
                    Permissions.OWNERSHIP,
                    Permissions.TRANSACTIONS,
                    Permissions.ACCOUNT_NUMBERS
                ),
                isStripeDirect = false,
                dataPolicyUrl = ""
            ),
            onLearnMoreClick = {}
        )
    }
}
