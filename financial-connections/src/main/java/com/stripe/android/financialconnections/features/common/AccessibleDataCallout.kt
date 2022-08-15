package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.Permissions
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AccessibleDataCallout(
    model: AccessibleDataCalloutModel,
) {
    val permissionsReadable = remember(model.permissions) { model.permissions.toStringRes() }
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                shape = RoundedCornerShape(8.dp),
                color = FinancialConnectionsTheme.colors.borderDefault,
            )
            .background(color = FinancialConnectionsTheme.colors.backgroundContainer)
            .padding(12.dp)
    ) {
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
)
