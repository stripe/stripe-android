package com.stripe.android.link.ui

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors

private val LinkButtonVerticalPadding = 6.dp
private val LinkButtonHorizontalPadding = 10.dp

@Preview
@Composable
private fun LinkButton() {
    LinkButton(
        enabled = true,
        email = "example@stripe.com",
        onClick = {}
    )
}

@Composable
private fun LinkButton(
    linkPaymentLauncher: LinkPaymentLauncher,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val account = linkPaymentLauncher.linkAccountManager.linkAccount.collectAsState()

    LinkButton(
        enabled = enabled,
        email = account.value?.email,
        onClick = onClick
    )
}

@Composable
private fun LinkButton(
    enabled: Boolean,
    email: String?,
    onClick: () -> Unit
) {
    CompositionLocalProvider(
        LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled
    ) {
        DefaultLinkTheme {
            Button(
                onClick = onClick,
                enabled = enabled,
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
                    painter = painterResource(R.drawable.ic_link_logo),
                    contentDescription = stringResource(R.string.link),
                    modifier = Modifier
                        .height(22.dp)
                        .padding(
                            start = 5.dp,
                            top = 3.dp,
                            bottom = 3.dp
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
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier
                                .padding(6.dp),
                            color = MaterialTheme.linkColors.buttonLabel,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Wrapper view around a button for paying with Link, for use in xml layout.
 *
 * Set the `onClick` function to launch LinkPaymentLauncher.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    var linkPaymentLauncher: LinkPaymentLauncher? = null
    var onClick by mutableStateOf({})
    private var isEnabledState by mutableStateOf(isEnabled)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        isEnabledState = enabled
    }

    @Composable
    override fun Content() {
        linkPaymentLauncher?.let {
            LinkButton(
                it,
                isEnabledState,
                onClick
            )
        }
    }
}
