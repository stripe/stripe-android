package com.stripe.android.link.ui

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.linkColors

@Preview
@Composable
private fun LinkButton() {
    LinkButton(
        enabled = true,
        onClick = {}
    )
}

@Composable
private fun LinkButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    CompositionLocalProvider(
        LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled,
    ) {
        DefaultLinkTheme {
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    disabledBackgroundColor = MaterialTheme.colors.primary
                )
            ) {
                println(LocalContentAlpha.current)
                Icon(
                    painter = painterResource(R.drawable.ic_link_logo),
                    contentDescription = stringResource(R.string.link),
                    modifier = Modifier
                        .height(22.dp)
                        .padding(
                            start = 5.dp,
                            top = 4.dp,
                            bottom = 4.dp
                        ),
                    tint = MaterialTheme.linkColors.buttonLabel
                        .copy(alpha = LocalContentAlpha.current)
                )
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
class LinkViewButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    var onClick by mutableStateOf({})
    var isButtonEnabled by mutableStateOf(isEnabled)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        isButtonEnabled = enabled
    }

    @Composable
    override fun Content() {
        LinkButton(
            isButtonEnabled,
            onClick
        )
    }
}
