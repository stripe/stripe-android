package com.stripe.android.paymentelement.embedded.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.TestModeBadge
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.uicore.stripeColors

@Composable
internal fun FormActivityUI(
    interactor: DefaultVerticalModeFormInteractor,
    eventReporter: EventReporter,
    onDismissed: () -> Unit
) {
    val scrollState = rememberScrollState()
    EventReporterProvider(eventReporter) {
        BottomSheetScaffold(
            topBar = {
                FormActivityTopBar(
                    isLiveMode = interactor.isLiveMode,
                    onDismissed = onDismissed
                )
            },
            content = {
                VerticalModeFormUI(
                    interactor = interactor,
                    showsWalletHeader = false
                )
            },
            scrollState = scrollState
        )
    }
}

@Composable
internal fun FormActivityTopBar(
    isLiveMode: Boolean,
    onDismissed: () -> Unit
) {
    val tintColor = MaterialTheme.stripeColors.appBarIcon
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        if (!isLiveMode) {
            TestModeBadge()
        }
        IconButton(
            enabled = true,
            onClick = onDismissed,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                contentDescription = stringResource(R.string.stripe_paymentsheet_close),
                tint = tintColor
            )
        }
    }
}
