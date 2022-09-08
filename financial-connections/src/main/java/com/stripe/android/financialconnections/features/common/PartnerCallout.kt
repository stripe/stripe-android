package com.stripe.android.financialconnections.features.common

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession.Flow
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun PartnerCallout(
    flow: Flow
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color = FinancialConnectionsTheme.colors.backgroundContainer)
            .padding(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.size(16.dp))
        AnnotatedText(
            TextResource.StringId(
                R.string.stripe_prepane_partner_callout,
                listOf(stringResource(id = flow.partnerName()))
            ),
            defaultStyle = FinancialConnectionsTheme.typography.captionTightEmphasized.copy(
                color = FinancialConnectionsTheme.colors.textSecondary
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.captionTightEmphasized
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textBrand)
            ),
            onClickableTextClick = {}
        )
    }
}

@StringRes
private fun Flow.partnerName(): Int = when (this) {
    Flow.FINICITY_CONNECT_V2_FIX,
    Flow.FINICITY_CONNECT_V2_LITE,
    Flow.FINICITY_CONNECT_V2_OAUTH,
    Flow.FINICITY_CONNECT_V2_OAUTH_REDIRECT,
    Flow.FINICITY_CONNECT_V2_OAUTH_WEBVIEW -> R.string.stripe_partner_finicity
    Flow.MX_CONNECT,
    Flow.MX_OAUTH,
    Flow.MX_OAUTH_REDIRECT,
    Flow.MX_OAUTH_WEBVIEW -> R.string.stripe_partner_mx
    Flow.TESTMODE,
    Flow.TESTMODE_OAUTH,
    Flow.TESTMODE_OAUTH_WEBVIEW -> R.string.stripe_partner_testmode
    Flow.TRUELAYER_OAUTH,
    Flow.TRUELAYER_OAUTH_HANDOFF,
    Flow.TRUELAYER_OAUTH_WEBVIEW -> R.string.stripe_partner_truelayer
    Flow.WELLS_FARGO,
    Flow.WELLS_FARGO_WEBVIEW -> R.string.stripe_partner_wellsfargo
    Flow.DIRECT,
    Flow.DIRECT_WEBVIEW -> TODO()
}
