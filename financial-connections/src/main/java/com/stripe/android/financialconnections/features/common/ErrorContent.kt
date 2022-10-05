@file:Suppress("TooManyFunctions")

package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.AccountNumberRetrievalException
import com.stripe.android.financialconnections.exception.InstitutionPlannedException
import com.stripe.android.financialconnections.exception.InstitutionUnplannedException
import com.stripe.android.financialconnections.exception.NoAccountsAvailableException
import com.stripe.android.financialconnections.exception.NoSupportedPaymentMethodTypeAccountsException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import java.text.SimpleDateFormat

@Composable
internal fun UnclassifiedErrorContent(
    error: Throwable,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    ErrorContent(
        painterResource(id = R.drawable.stripe_ic_brandicon_institution),
        title = stringResource(R.string.stripe_error_generic_title),
        content = stringResource(R.string.stripe_error_generic_desc),
        primaryCta = stringResource(R.string.stripe_error_cta_close) to {
            onCloseFromErrorClick(error)
        }
    )
}

@Composable
internal fun InstitutionUnknownErrorContent(
    onSelectAnotherBank: () -> Unit
) {
    ErrorContent(
        iconPainter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
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
    exception: InstitutionUnplannedException,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit
) {
    ErrorContent(
        iconPainter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
        title = stringResource(
            R.string.stripe_error_unplanned_downtime_title,
            exception.institution.name
        ),
        content = stringResource(R.string.stripe_error_unplanned_downtime_desc),
        primaryCta = Pair(
            stringResource(R.string.stripe_error_cta_select_another_bank),
            onSelectAnotherBank
        ),
        secondaryCta = if (exception.allowManualEntry) {
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
    exception: InstitutionPlannedException,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit
) {
    val javaLocale: java.util.Locale = remember { java.util.Locale(Locale.current.language) }
    val readableDate = remember(exception.backUpAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", javaLocale).format(exception.backUpAt)
    }
    ErrorContent(
        iconPainter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
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
        secondaryCta = if (exception.allowManualEntry) {
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
    exception: NoSupportedPaymentMethodTypeAccountsException,
    onSelectAnotherBank: () -> Unit
) {
    ErrorContent(
        painterResource(id = R.drawable.stripe_ic_brandicon_institution),
        title = stringResource(
            R.string.stripe_account_picker_error_no_payment_method_title
        ),
        content = stringResource(
            R.string.stripe_account_picker_error_no_payment_method_desc,
            exception.accountsCount.toString(),
            exception.institution.name,
            exception.merchantName
        ),
        primaryCta = Pair(
            stringResource(R.string.stripe_error_cta_select_another_bank),
            onSelectAnotherBank
        )
    )
}

@Composable
internal fun NoAccountsAvailableErrorContent(
    exception: NoAccountsAvailableException,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onTryAgain: () -> Unit
) {
    // primary and (optional) secondary button to show.
    val (primaryCta: Pair<Int, () -> Unit>, secondaryCta: Pair<Int, () -> Unit>?) = remember(
        exception.allowManualEntry,
        exception.canRetry
    ) {
        when {
            exception.canRetry -> Pair(
                first = R.string.stripe_error_cta_retry to onTryAgain,
                second = R.string.stripe_error_cta_select_another_bank to onSelectAnotherBank,
            )

            exception.allowManualEntry -> Pair(
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
    val description = remember(exception.allowManualEntry, exception.canRetry) {
        when {
            exception.canRetry -> R.string.stripe_accounts_error_desc_retry
            exception.allowManualEntry -> R.string.stripe_accounts_error_desc_manualentry
            else -> R.string.stripe_accounts_error_desc_no_retry
        }
    }

    ErrorContent(
        painterResource(id = R.drawable.stripe_ic_brandicon_institution),
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
    exception: AccountNumberRetrievalException,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit
) {
    ErrorContent(
        painterResource(id = R.drawable.stripe_ic_brandicon_institution),
        title = stringResource(
            R.string.stripe_attachlinkedpaymentaccount_error_title
        ),
        content = stringResource(
            when (exception.allowManualEntry) {
                true -> R.string.stripe_attachlinkedpaymentaccount_error_desc_manual_entry
                false -> R.string.stripe_attachlinkedpaymentaccount_error_desc
            }
        ),
        primaryCta = Pair(
            stringResource(R.string.stripe_error_cta_select_another_bank),
            onSelectAnotherBank
        ),
        secondaryCta = if (exception.allowManualEntry) {
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
    iconPainter: Painter,
    badge: Pair<Painter, Shape> = Pair(
        painterResource(id = R.drawable.stripe_ic_warning_circle),
        CircleShape
    ),
    title: String,
    content: String,
    primaryCta: Pair<String, () -> Unit>? = null,
    secondaryCta: Pair<String, () -> Unit>? = null
) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            BadgedImage(iconPainter, badge)
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = title,
                style = FinancialConnectionsTheme.typography.subtitle
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = content,
                style = FinancialConnectionsTheme.typography.body
            )
        }
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
        if (primaryCta != null && secondaryCta != null) Spacer(Modifier.size(8.dp))
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

@Composable
private fun BadgedImage(
    iconPainter: Painter,
    badge: Pair<Painter, Shape>
) {
    Box(
        modifier = Modifier
            .size(40.dp)
    ) {
        Image(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(6.dp))
        )
        Icon(
            painter = badge.first,
            contentDescription = "",
            tint = FinancialConnectionsTheme.colors.textCritical,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(12.dp)
                .clip(badge.second)
                // draws a background with padding around the badge to simulate a border.
                .background(FinancialConnectionsTheme.colors.textWhite)
                .padding(1.dp)
        )
    }
}

@Composable
@Preview(group = "Errors", name = "unclassified error")
internal fun UnclassifiedErrorContentPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsScaffold(
            topBar = { FinancialConnectionsTopAppBar(onCloseClick = { }) }
        ) {
            UnclassifiedErrorContent(APIException()) {}
        }
    }
}

@Composable
@Preview(group = "Errors", name = "institution down planned error")
internal fun InstitutionPlannedDowntimeErrorContentPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsScaffold(
            topBar = { FinancialConnectionsTopAppBar(onCloseClick = { }) }
        ) {
            InstitutionPlannedDowntimeErrorContent(
                exception = InstitutionPlannedException(
                    institution = FinancialConnectionsInstitution(
                        id = "3",
                        name = "RandomInstitution",
                        url = "RandomInstitution url",
                        featured = false,
                        featuredOrder = null,
                        mobileHandoffCapable = false
                    ),
                    allowManualEntry = true,
                    isToday = true,
                    backUpAt = 10000L,
                    stripeException = APIException()
                ),
                onEnterDetailsManually = {},
                onSelectAnotherBank = {}
            )
        }
    }
}

@Composable
@Preview(group = "Errors", name = "no accounts available error")
internal fun NoAccountsAvailableErrorContentPreview() {
    FinancialConnectionsTheme {
        FinancialConnectionsScaffold(
            topBar = { FinancialConnectionsTopAppBar(onCloseClick = { }) }
        ) {
            NoAccountsAvailableErrorContent(
                exception = NoAccountsAvailableException(
                    institution = FinancialConnectionsInstitution(
                        id = "3",
                        name = "RandomInstitution",
                        url = "RandomInstitution url",
                        featured = false,
                        featuredOrder = null,
                        mobileHandoffCapable = false
                    ),
                    allowManualEntry = true,
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
