package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.InstitutionPlannedException
import com.stripe.android.financialconnections.exception.InstitutionUnplannedException
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import java.text.SimpleDateFormat

@Composable
internal fun UnclassifiedErrorContent() {
    ErrorContent(
        painterResource(id = R.drawable.stripe_ic_brandicon_institution),
        title = stringResource(R.string.stripe_error_generic_title),
        content = stringResource(R.string.stripe_error_generic_desc)
    )
}

@Composable
internal fun InstitutionUnplannedDowntimeErrorContent(
    exception: InstitutionUnplannedException,
    onSelectAnotherBank: () -> Unit
) {
    ErrorContent(
        iconPainter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
        title = stringResource(
            R.string.stripe_error_unplanned_downtime_title,
            exception.institution.name
        ),
        content = stringResource(R.string.stripe_error_unplanned_downtime_desc),
        ctaText = stringResource(R.string.stripe_error_cta_select_another_bank),
        onCtaClick = onSelectAnotherBank
    )
}

@Composable
internal fun InstitutionPlannedDowntimeErrorContent(
    exception: InstitutionPlannedException,
    onSelectAnotherBank: () -> Unit
) {
    val readableDate = remember(exception.backUpAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm").format(exception.backUpAt)
    }
    ErrorContent(
        iconPainter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
        title = stringResource(
            R.string.stripe_error_planned_downtime_title,
            exception.institution.name
        ),
        content = stringResource(
            R.string.stripe_error_planned_downtime_desc,
            readableDate,
        ),
        ctaText = stringResource(R.string.stripe_error_cta_select_another_bank),
        onCtaClick = onSelectAnotherBank
    )
}

@Composable
internal fun ErrorContent(
    iconPainter: Painter,
    title: String,
    content: String,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null
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
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
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
        if (ctaText != null && onCtaClick != null) {
            FinancialConnectionsButton(
                onClick = onCtaClick,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = ctaText)
            }
        }
    }
}

@Composable
@Preview(group = "Errors", name = "unclassified error")
internal fun ErrorContentPreview() {
    FinancialConnectionsTheme {
        UnclassifiedErrorContent()
    }
}
