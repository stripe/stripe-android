package com.stripe.android.link.ui.verification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.paymentsheet.R

@Composable
internal fun VerificationSandboxBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(VERIFICATION_SANDBOX_BANNER_TAG)
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(24.dp),
        color = LinkTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, LinkTheme.colors.surfaceTertiary),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(LinkTheme.colors.surfaceSecondary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.stripe_ic_sail_warning_circle),
                    contentDescription = null,
                    tint = LinkTheme.colors.iconPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.stripe_link_verification_sandbox_title),
                    style = LinkTheme.typography.detailEmphasized,
                    color = LinkTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.stripe_link_verification_sandbox_message),
                    style = LinkTheme.typography.caption,
                    color = if (LinkTheme.colors.isDark) {
                        LinkTheme.colors.textSecondary
                    } else {
                        LinkTheme.colors.textTertiary
                    },
                )
            }
        }
    }
}

internal const val VERIFICATION_SANDBOX_BANNER_TAG = "verification_sandbox_banner"
