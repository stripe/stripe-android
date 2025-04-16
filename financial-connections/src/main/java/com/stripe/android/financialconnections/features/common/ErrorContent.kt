package com.stripe.android.financialconnections.features.common

import android.os.Build
import android.view.HapticFeedbackConstants.REJECT
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.exception.AccountNumberRetrievalError
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.pluralStringResource
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import java.text.SimpleDateFormat

@Composable
internal fun UnclassifiedErrorContent(
    allowManualEntry: Boolean = false,
    onCtaClick: () -> Unit
) {
    ErrorContent(
        iconContent = {
            ShapedIcon(
                painter = painterResource(id = R.drawable.stripe_ic_warning),
                contentDescription = null
            )
        },
        title = stringResource(R.string.stripe_error_generic_title),
        content = stringResource(R.string.stripe_error_generic_desc),
        primaryCta = Pair(
            stringResource(
                if (allowManualEntry) R.string.stripe_error_cta_manual_entry else R.string.stripe_error_cta_close
            ),
            onCtaClick
        )
    )
}

@Composable
internal fun InstitutionUnknownErrorContent(
    onSelectAnotherBank: () -> Unit
) {
    ErrorContent(
        iconContent = {
            ShapedIcon(
                painter = painterResource(id = R.drawable.stripe_ic_warning),
                contentDescription = null
            )
        },
        title = stringResource(R.string.stripe_error_generic_title),
        content = stringResource(R.string.stripe_error_unplanned_downtime_desc),
        primaryCta = Pair(
            stringResource(R.string.stripe_error_cta_select_another_bank),
            onSelectAnotherBank
        )
    )
}

@Composable
internal fun InstitutionUnplannedDowntimeErrorContent(
    exception: InstitutionUnplannedDowntimeError,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit
) {
    ErrorContent(
        iconContent = {
            InstitutionIcon(institutionIcon = exception.institution.icon?.default ?: "")
        },
        title = stringResource(
            R.string.stripe_error_unplanned_downtime_title,
            exception.institution.name
        ),
        content = stringResource(R.string.stripe_error_unplanned_downtime_desc),
        primaryCta = Pair(
            stringResource(R.string.stripe_error_cta_select_another_bank),
            onSelectAnotherBank
        ),
        secondaryCta = if (exception.showManualEntry) {
            Pair(
                stringResource(R.string.stripe_error_cta_manual_entry),
                onEnterDetailsManually
            )
        } else {
            null
        }
    )
}

@Composable
internal fun InstitutionPlannedDowntimeErrorContent(
    exception: InstitutionPlannedDowntimeError,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit
) {
    val javaLocale: java.util.Locale = remember { java.util.Locale(Locale.current.language) }
    val readableDate = remember(exception.backUpAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", javaLocale).format(exception.backUpAt)
    }
    ErrorContent(
        iconContent = {
            InstitutionIcon(institutionIcon = exception.institution.icon?.default ?: "")
        },
        title = stringResource(
            R.string.stripe_error_planned_downtime_title,
            exception.institution.name
        ),
        content = stringResource(
            R.string.stripe_error_planned_downtime_desc,
            readableDate
        ),
        primaryCta = Pair(
            stringResource(R.string.stripe_error_cta_select_another_bank),
            onSelectAnotherBank
        ),
        secondaryCta = if (exception.showManualEntry) {
            Pair(
                stringResource(R.string.stripe_error_cta_manual_entry),
                onEnterDetailsManually
            )
        } else {
            null
        }
    )
}

@Composable
internal fun NoSupportedPaymentMethodTypeAccountsErrorContent(
    exception: AccountNoneEligibleForPaymentMethodError,
    onSelectAnotherBank: () -> Unit
) {
    ErrorContent(
        iconContent = {
            InstitutionIcon(institutionIcon = exception.institution.icon?.default ?: "")
        },
        title = stringResource(
            R.string.stripe_account_picker_error_no_payment_method_title
        ),
        content = pluralStringResource(
            singular = R.string.stripe_account_picker_error_no_payment_method_desc_singular,
            plural = R.string.stripe_account_picker_error_no_payment_method_desc_plural,
            count = exception.accountsCount,
            exception.accountsCount.toString(),
            exception.institution.name,
            exception.merchantName
        ),
        primaryCta = Pair(
            stringResource(R.string.stripe_error_cta_select_another_bank),
            onSelectAnotherBank
        ),
        secondaryCta = null
    )
}

@Composable
internal fun NoAccountsAvailableErrorContent(
    exception: AccountLoadError,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onTryAgain: () -> Unit
) {
    // primary and (optional) secondary button to show.
    val (primaryCta: Pair<Int, () -> Unit>, secondaryCta: Pair<Int, () -> Unit>?) = remember(
        exception.showManualEntry,
        exception.canRetry
    ) {
        when {
            exception.canRetry -> Pair(
                first = R.string.stripe_error_cta_retry to onTryAgain,
                second = R.string.stripe_error_cta_select_another_bank to onSelectAnotherBank,
            )

            exception.showManualEntry -> Pair(
                first = R.string.stripe_error_cta_manual_entry to onEnterDetailsManually,
                second = R.string.stripe_error_cta_select_another_bank to onSelectAnotherBank,
            )

            else -> Pair(
                first = R.string.stripe_error_cta_select_another_bank to onSelectAnotherBank,
                second = null
            )
        }
    }
    // description to show.
    val description = remember(exception.showManualEntry, exception.canRetry) {
        when {
            exception.canRetry -> R.string.stripe_accounts_error_desc_retry
            exception.showManualEntry -> R.string.stripe_accounts_error_desc_manualentry
            else -> R.string.stripe_accounts_error_desc_no_retry
        }
    }

    ErrorContent(
        iconContent = {
            InstitutionIcon(institutionIcon = exception.institution.icon?.default ?: "")
        },
        title = stringResource(
            R.string.stripe_account_picker_error_no_account_available_title,
            exception.institution.name
        ),
        content = stringResource(description),
        primaryCta = stringResource(primaryCta.first) to primaryCta.second,
        secondaryCta = secondaryCta?.let { stringResource(it.first) to it.second }
    )
}

@Composable
internal fun AccountNumberRetrievalErrorContent(
    exception: AccountNumberRetrievalError,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit
) {
    ErrorContent(
        iconContent = {
            InstitutionIcon(institutionIcon = exception.institution.icon?.default ?: "")
        },
        title = stringResource(
            R.string.stripe_attachlinkedpaymentaccount_error_title
        ),
        content = stringResource(
            when (exception.showManualEntry) {
                true -> R.string.stripe_attachlinkedpaymentaccount_error_desc_manual_entry
                false -> R.string.stripe_attachlinkedpaymentaccount_error_desc
            }
        ),
        primaryCta = Pair(
            stringResource(R.string.stripe_error_cta_select_another_bank),
            onSelectAnotherBank
        ),
        secondaryCta = if (exception.showManualEntry) {
            Pair(
                stringResource(R.string.stripe_error_cta_manual_entry),
                onEnterDetailsManually
            )
        } else {
            null
        }
    )
}

@Composable
internal fun ErrorContent(
    iconContent: @Composable (() -> Unit)?,
    title: String,
    content: String,
    primaryCta: Pair<String, () -> Unit>? = null,
    secondaryCta: Pair<String, () -> Unit>? = null
) {
    val view = LocalView.current
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(REJECT)
        }
    }
    LazyLayout(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        body = {
            iconContent?.let {
                item { Box(modifier = Modifier.padding(top = 16.dp)) { it() } }
            }
            item {
                Text(
                    text = title,
                    style = typography.headingXLarge,
                    color = colors.textDefault,
                )
            }
            item {
                Text(
                    text = content,
                    style = typography.bodyMedium,
                    color = colors.textDefault,
                )
            }
        },
        footer = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secondaryCta?.let { (text, onClick) ->
                    FinancialConnectionsButton(
                        type = FinancialConnectionsButton.Type.Secondary,
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = text)
                    }
                }
                primaryCta?.let { (text, onClick) ->
                    FinancialConnectionsButton(
                        type = FinancialConnectionsButton.Type.Primary,
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = text)
                    }
                }
            }
        }
    )
}

@Preview(group = "Errors", name = "no accounts available error")
@Composable
internal fun NoAccountsAvailableErrorContentPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsScaffold(
            topBar = {
                FinancialConnectionsTopAppBar(
                    state = TopAppBarState(hideStripeLogo = false),
                    onCloseClick = {},
                )
            }
        ) {
            NoAccountsAvailableErrorContent(
                exception = AccountLoadError(
                    institution = FinancialConnectionsInstitution(
                        id = "3",
                        name = "Random Institution",
                        url = "Random Institution url",
                        featured = false,
                        featuredOrder = null,
                        icon = null,
                        logo = null,
                        mobileHandoffCapable = false
                    ),
                    showManualEntry = true,
                    stripeException = APIException(),
                    canRetry = true
                ),
                onEnterDetailsManually = {},
                onSelectAnotherBank = {},
                onTryAgain = {}
            )
        }
    }
}
