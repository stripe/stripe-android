@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.link.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.R as StripeR

private val LinkButtonVerticalPadding = 6.dp
private val LinkButtonHorizontalPadding = 10.dp
private val LinkButtonShape: RoundedCornerShape
    get() = RoundedCornerShape(
        StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
    )
private val LinkButtonEmailShape: RoundedCornerShape
    get() = RoundedCornerShape(
        StripeTheme.primaryButtonStyle.shape.cornerRadius.dp / 2
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val LinkButtonTestTag = "LinkButtonTestTag"

@Preview
@Composable
private fun LinkButton() {
    LinkButton(
        enabled = true,
        email = "example@stripe.com",
        onClick = {}
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun LinkButton(
    email: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled
    ) {
        DefaultLinkTheme {
            Button(
                onClick = onClick,
                modifier = modifier
                    .clip(LinkButtonShape)
                    .testTag(LinkButtonTestTag),
                enabled = enabled,
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                shape = LinkButtonShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    disabledBackgroundColor = MaterialTheme.colors.primary
                ),
                contentPadding = PaddingValues(
                    start = LinkButtonHorizontalPadding,
                    top = LinkButtonVerticalPadding,
                    end = LinkButtonHorizontalPadding,
                    bottom = LinkButtonVerticalPadding
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.stripe_link_logo),
                    contentDescription = stringResource(StripeR.string.stripe_link),
                    modifier = Modifier
                        .height(22.dp)
                        .padding(
                            horizontal = 5.dp,
                            vertical = 3.dp
                        ),
                    tint = MaterialTheme.linkColors.buttonLabel
                        .copy(alpha = LocalContentAlpha.current)
                )
                email?.let {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.05f),
                                shape = LinkButtonEmailShape
                            )
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier
                                .padding(6.dp),
                            color = MaterialTheme.linkColors.buttonLabel,
                            fontSize = 14.sp,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
