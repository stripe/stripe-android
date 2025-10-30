package com.stripe.android.crypto.onramp.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.getLinkIcon

@Composable
internal fun KYCRefreshScreen(
    appearance: LinkAppearance?,
    updatedAddress: PaymentSheet.Address? = null,
    onClose: () -> Unit = { }
) {
    var name by remember { mutableStateOf("Satoshi Nakamoto") }
    var dob by remember { mutableStateOf("10/06/1995") }
    var ssnLast4 by remember { mutableStateOf("5678") }
    var address by remember { mutableStateOf("2930 E 2nd Ave, Denver, CO, United States of America, 80206") }

    OnrampTheme(appearance) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                TopNavigationBar(
                    onClose = onClose,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "Confirm your information",
                    style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        InfoRow(title = "Name", value = name)
                        Divider()
                        InfoRow(title = "Date of Birth", value = dob)
                        Divider()
                        InfoRow(title = "Last 4 digits of SSN", value = ssnLast4)
                        Divider()
                        InfoRow(title = "Address", value = address, icon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Address"
                            )
                        }, onIconTap = { /* Handle edit action */ })
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { /* Handle confirm action */ },
                    shape = RoundedCornerShape(appearance?.primaryButton?.cornerRadiusDp?.dp ?: 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(appearance?.primaryButton?.heightDp?.dp ?: 56.dp)
                ) {
                    Text(
                        "Confirm",
                        style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun TopNavigationBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = getLinkIcon()),
            contentDescription = "Link",
            tint = Color.Unspecified,
            modifier = Modifier
                .height(72.dp)
                .width(88.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onClose,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colors.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String,
    icon: (@Composable () -> Unit)? = null,
    onIconTap: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface
            )
        }
        icon?.let {
            IconButton(onClick = onIconTap ?: {}) {
                it()
            }
        }
    }
}

@Composable
fun OnrampTheme(
    linkAppearance: LinkAppearance?,
    content: @Composable () -> Unit,
) {
    linkAppearance?.let {
        val isDark = when (it.style) {
            LinkAppearance.Style.ALWAYS_LIGHT -> false
            LinkAppearance.Style.ALWAYS_DARK -> true
            LinkAppearance.Style.AUTOMATIC -> isSystemInDarkTheme()
        }

        val baseContext = LocalContext.current
        val inspectionMode = LocalInspectionMode.current
        val styleContext = remember(baseContext, isDark, inspectionMode) {
            val uiMode =
                if (isDark) {
                    Configuration.UI_MODE_NIGHT_YES
                } else {
                    Configuration.UI_MODE_NIGHT_NO
                }
            baseContext.withUiMode(uiMode, inspectionMode)
        }

        val colors = if (isDark) darkColors() else lightColors()
        val linkColors = if (isDark) it.darkColors else it.lightColors

        val resolvedColors = colors.copy(
            primary = linkColors.primary,
            onPrimary = linkColors.contentOnPrimary,
        )

        CompositionLocalProvider(
            LocalContext provides styleContext,
        ) {
            MaterialTheme(
                colors = resolvedColors,
                content = content
            )
        }
    } ?: run {
        MaterialTheme {
            content()
        }
    }
}

private fun Context.withUiMode(uiMode: Int, inspectionMode: Boolean): Context {
    if (uiMode == this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        return this
    }
    val config = Configuration(resources.configuration).apply {
        this.uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or uiMode
    }
    return object : ContextThemeWrapper(this, theme) {
        override fun getResources(): Resources? {
            @Suppress("DEPRECATION")
            if (inspectionMode) {
                // Workaround NPE thrown in BridgeContext#createConfigurationContext() when getting resources.
                val baseResources = this@withUiMode.resources
                return Resources(
                    baseResources.assets,
                    baseResources.displayMetrics,
                    config
                )
            }
            return super.getResources()
        }
    }.apply {
        applyOverrideConfiguration(config)
    }
}
