package com.stripe.android.link.onramp.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState

@Composable
@Suppress("LongMethod")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CrsCarfDeclarationScreen(
    appearance: LinkAppearance.State?,
    attestationText: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    BackHandler(onBack = onClose)

    DefaultLinkTheme(appearance = appearance) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinkTheme.colors.surfaceBackdrop.copy(alpha = 0.32f))
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = LinkTheme.colors.surfacePrimary,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                elevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tax declaration",
                            style = LinkTheme.typography.bodyEmphasized,
                            color = LinkTheme.colors.textPrimary
                        )
                        TextButton(onClick = onClose) {
                            Text(
                                text = "Cancel",
                                style = LinkTheme.typography.detailEmphasized,
                                color = LinkTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = attestationText,
                        style = LinkTheme.typography.body,
                        color = LinkTheme.colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    )

                    Spacer(Modifier.height(24.dp))

                    PrimaryButton(
                        label = "Accept",
                        state = PrimaryButtonState.Enabled,
                        onButtonClick = onConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
