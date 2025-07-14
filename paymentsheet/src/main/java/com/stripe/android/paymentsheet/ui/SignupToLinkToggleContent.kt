package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState

@Immutable
internal class SignupToLinkToggleContent(
    internal val interactor: SignupToLinkToggleInteractor,
) {
    @Composable
    fun Content() {
        val state by interactor.state.collectAsState()

        if (state.shouldDisplay) {
            StripeTheme {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Save my info for faster checkout with Link",
                            fontSize = 14.sp * StripeThemeDefaults.typography.fontSizeMultiplier,
                            color = MaterialTheme.stripeColors.onComponent,
                            textAlign = TextAlign.Start
                        )
                        
                        Text(
                            text = "Pay faster everywhere Link is accepted.",
                            fontSize = 12.sp * StripeThemeDefaults.typography.fontSizeMultiplier,
                            color = MaterialTheme.stripeColors.subtitle,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    Switch(
                        checked = state.isChecked,
                        onCheckedChange = { checked ->
                            interactor.handleToggleChange(checked)
                        },
                        enabled = state.isEnabled,
                        modifier = Modifier.padding(start = 8.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(MaterialTheme.stripeColors.materialColors.primary.value),
                            checkedTrackColor = Color(MaterialTheme.stripeColors.materialColors.primary.value).copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
} 