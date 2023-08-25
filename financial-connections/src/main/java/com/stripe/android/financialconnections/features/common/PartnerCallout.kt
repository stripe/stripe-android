package com.stripe.android.financialconnections.features.common

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
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.model.PartnerNotice
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImage

@Composable
internal fun PartnerCallout(
    modifier: Modifier = Modifier,
    partnerNotice: PartnerNotice,
    onClickableTextClick: (String) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(8.dp))
            .background(color = FinancialConnectionsTheme.colors.backgroundContainer)
            .padding(12.dp)
    ) {
        StripeImage(
            url = partnerNotice.partnerIcon.default ?: "",
            imageLoader = LocalImageLoader.current,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.size(16.dp))
        AnnotatedText(
            TextResource.Text(
                fromHtml(partnerNotice.text)
            ),
            defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
                color = FinancialConnectionsTheme.colors.textSecondary
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.captionEmphasized
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textBrand),
                StringAnnotation.BOLD to FinancialConnectionsTheme.typography.captionEmphasized
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textSecondary)
            ),
            onClickableTextClick = onClickableTextClick
        )
    }
}
