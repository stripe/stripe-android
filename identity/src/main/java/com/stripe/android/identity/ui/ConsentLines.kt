package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.VerificationPageIconType
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
                .padding(
                    top = dimensionResource(id = R.dimen.stripe_item_vertical_margin),
                    start = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                    end = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
                )
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
                color = colorResource(id = R.color.stripe_html_line),
                style = LocalTextStyle.current.merge(fontSize = 16.sp),
                bottomSheets = bottomSheets,
                urlSpanStyle = SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = colorResource(id = R.color.stripe_html_line)
                )
            )
        }
    }
}

internal const val CONSENT_LINE_TAG = "consentLineTag"

@Preview
@Composable
@ExperimentalMaterialApi
internal fun ConsentLinePreview() {
    IdentityPreview {
        Column {
            ConsentLines(
                lines = listOf(
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.PHONE,
                        content = "This is the line content with phone icon"
                    ),
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.CAMERA,
                        content = "This is the line content with camera icon"
                    ),
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.CLOUD,
                        content = "This is the line content with cloud icon and a " +
                            "<a href='https://stripe.com'>web link</a>"
                    ),
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.WALLET,
                        content = "This is the line content with wallet icon and a <a " +
                            "href='stripe_bottomsheet://open/consent_verification_data'>" +
                            "bottomsheet link</a>"
                    )
                ),
                bottomSheets = mapOf()
            )
        }
    }
}
