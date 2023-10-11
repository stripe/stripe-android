package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.VerificationPageStaticConsentLineContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import com.stripe.android.identity.networking.models.getContentDescriptionId
import com.stripe.android.identity.networking.models.getResourceId

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ConsentLines(
    lines: List<VerificationPageStaticConsentLineContent>,
    bottomSheets: Map<String, VerificationPageStaticContentBottomSheetContent>?
) {
    for (line in lines) {
        Row(
            modifier = Modifier
                .testTag(CONSENT_LINE_TAG)
                .padding(top = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
        ) {
            Image(
                painter = painterResource(id = line.icon.getResourceId()),
                modifier = Modifier
                    .size(28.dp)
                    .padding(end = 8.dp),
                contentDescription = stringResource(id = line.icon.getContentDescriptionId())
            )
            BottomSheetHTML(
                html = line.content,
                color = MaterialTheme.colors.onSurface.copy(
                    alpha = 0.6f
                ),
                style = LocalTextStyle.current.merge(fontSize = 16.sp),
                bottomSheets = bottomSheets,
                urlSpanStyle = SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colors.secondary
                )
            )
        }
    }
}

internal const val CONSENT_LINE_TAG = "consentLineTag"
