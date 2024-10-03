package com.stripe.android.identity.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.R
import com.stripe.android.identity.utils.isRemote
import com.stripe.android.identity.utils.urlWithoutQuery
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.image.getDrawableFromUri
import com.stripe.android.uicore.image.rememberDrawablePainter

@Composable
internal fun ConsentWelcomeHeader(
    modifier: Modifier = Modifier,
    merchantLogoUri: Uri,
    title: String?,
    showLogos: Boolean = true
) {
    if (showLogos) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (merchantLogoUri.isRemote()) {
                val localContext = LocalContext.current
                val imageLoader = remember(merchantLogoUri) {
                    StripeImageLoader(localContext)
                }
                StripeImage(
                    url = merchantLogoUri.urlWithoutQuery(),
                    imageLoader = imageLoader,
                    contentDescription = stringResource(id = R.string.stripe_description_merchant_logo),
                    modifier = Modifier
                        .width(64.dp)
                        .height(64.dp)
                )
            } else {
                Image(
                    painter = rememberDrawablePainter(
                        LocalContext.current.getDrawableFromUri(
                            merchantLogoUri
                        )
                    ),
                    modifier = Modifier
                        .width(64.dp)
                        .height(64.dp),
                    contentDescription = stringResource(id = R.string.stripe_description_merchant_logo)
                )
            }
            Image(
                painter = painterResource(id = R.drawable.stripe_ellipsis_icon),
                modifier = Modifier
                    .width(16.dp)
                    .height(16.dp),
                contentDescription = stringResource(id = R.string.stripe_description_ellipsis)
            )

            Image(
                painter = painterResource(id = R.drawable.stripe_square),
                modifier = Modifier
                    .width(64.dp)
                    .height(64.dp),
                contentDescription = stringResource(id = R.string.stripe_description_stripe_logo)
            )
        }
    }
    Text(
        text = title ?: "",
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(
                    id = R.dimen.stripe_item_vertical_margin
                )
            )
            .semantics {
                testTag = TITLE_TAG
            },
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center

    )
}

@Preview
@Composable
@ExperimentalMaterialApi
internal fun ConsentWelcomeHeaderPreview() {
    IdentityPreview {
        Column {
            ConsentWelcomeHeader(
                modifier = Modifier,
                merchantLogoUri = Uri.EMPTY,
                title = "TEST TITLE",
                showLogos = true
            )
        }
    }
}
