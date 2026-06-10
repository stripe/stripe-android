package com.stripe.android.common.nfcscan

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CircleSize = 160.dp
private val ScanRingColor = Color(0xFF90BEF5)
private val SuccessRingColor = Color(0xFF81C784)
private val SuccessCircleColor = Color(0xFF3A8A3A)
private val SuccessBackgroundColor = Color(0xFFEBF5EB)
private val IconTintScanning = Color(0xFF5B7FA6)
private val ScreenBackgroundIdle = Color(0xFFF7F7F9)
private val TitleColor = Color(0xFF1A1A1A)
private val SubtitleColor = Color(0xFF636366)
private val ErrorIconTint = Color(0xFFE53935)

/** Scale of the reading-state ripple after it expands from the shrink phase. */
private const val ReadingRippleExpandedScale = 1.32f

/** Minimum scale during the shrink phase when a tag is being read. */
private const val ReadingRippleShrinkScale = 0.78f

private enum class CoilCenterContent {
    Tap,
    Error,
    Success,
}

@Composable
internal fun NfcScanningScreen(
    state: NfcScanningState,
    tapZone: TapZone,
    shouldSave: Boolean,
    merchantName: String?,
    onClose: () -> Unit,
    onAddManually: () -> Unit,
    onShouldSaveChanged: (Boolean) -> Unit,
) {
    val isComplete = state is NfcScanningState.Complete

    val screenBackground by animateColorAsState(
        targetValue = when {
            isComplete -> SuccessBackgroundColor
            else -> ScreenBackgroundIdle
        },
        animationSpec = tween(600),
        label = "screenBg",
    )
    val circleBackground by animateColorAsState(
        targetValue = if (isComplete) SuccessCircleColor else Color.White,
        animationSpec = tween(500),
        label = "circleBg",
    )
    val ringColor by animateColorAsState(
        targetValue = if (isComplete) SuccessRingColor else ScanRingColor,
        animationSpec = tween(500),
        label = "ringColor",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground),
    ) {
        // NFC coil and ripples: full window + tap-zone bias so alignment matches the physical coil.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BiasAlignment(
                horizontalBias = tapZone.xBias * 2 - 1,
                verticalBias = tapZone.yBias * 2 - 1,
            ),
        ) {
            NfcScanCircle(
                state = state,
                circleBackground = circleBackground,
                ringColor = ringColor,
            )
        }

        // Copy, actions, and system bar padding — kept inside safe + IME bounds (not the coil).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                    contentDescription = stringResource(R.string.stripe_nfc_scan_close),
                    tint = Color(0xFF8A8A8F),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.stripe_nfc_scan_tap_behind_device),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                color = TitleColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.stripe_nfc_scan_instruction_subtitle),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = SubtitleColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedButton(
                onClick = onAddManually,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TitleColor,
                ),
                border = BorderStroke(1.dp, Color(0xFFD1D1D6)),
            ) {
                Text(
                    text = stringResource(R.string.stripe_nfc_scan_add_card_manually),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val saveText = if (merchantName != null) {
                stringResource(R.string.stripe_nfc_scan_save_card_for_future, merchantName)
            } else {
                stringResource(R.string.stripe_nfc_scan_save_card_generic)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = shouldSave,
                    onCheckedChange = onShouldSaveChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colors.primary,
                    ),
                )
                Text(
                    text = saveText,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF3C3C43).copy(alpha = 0.85f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NfcScanCircle(
    state: NfcScanningState,
    circleBackground: Color,
    ringColor: Color,
) {
    val isReading = state is NfcScanningState.Reading
    val isIdle = state is NfcScanningState.Scanning
    val isComplete = state is NfcScanningState.Complete

    val centerContent = when (state) {
        is NfcScanningState.Complete -> CoilCenterContent.Success
        is NfcScanningState.Failed -> CoilCenterContent.Error
        else -> CoilCenterContent.Tap
    }

    Box(contentAlignment = Alignment.Center) {
        if (isComplete) {
            Box(
                modifier = Modifier
                    .size(CircleSize + 26.dp)
                    .background(ringColor.copy(alpha = 0.38f), CircleShape),
            )
        }

        if (isIdle) {
            IdlePulsingRings(circleSize = CircleSize, color = ringColor)
        }

        if (isReading) {
            ReadingRippleRing(circleSize = CircleSize, color = ringColor)
        }

        Box(
            modifier = Modifier
                .size(CircleSize)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(circleBackground),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = centerContent,
                animationSpec = tween(400),
                label = "coilCenter",
            ) { content ->
                when (content) {
                    CoilCenterContent.Success -> {
                        Icon(
                            painter = painterResource(R.drawable.stripe_ic_nfc_scan_check),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                    CoilCenterContent.Error -> {
                        Icon(
                            painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                            contentDescription = stringResource(R.string.stripe_nfc_scan_error),
                            tint = ErrorIconTint,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    CoilCenterContent.Tap -> {
                        Icon(
                            painter = painterResource(R.drawable.stripe_ic_nfc_tap),
                            contentDescription = null,
                            tint = IconTintScanning,
                            modifier = Modifier.size(80.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdlePulsingRings(circleSize: Dp, color: Color) {
    val rings = remember { List(3) { Animatable(0f) } }

    LaunchedEffect(Unit) {
        rings.forEachIndexed { index, anim ->
            launch {
                delay(index * 600L)
                while (true) {
                    anim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 1800, easing = LinearEasing),
                    )
                    anim.snapTo(0f)
                }
            }
        }
    }

    rings.forEach { ring ->
        val progress = ring.value
        Box(
            modifier = Modifier
                .size(circleSize)
                .graphicsLayer {
                    scaleX = 1f + progress * 1.6f
                    scaleY = 1f + progress * 1.6f
                    alpha = (1f - progress) * 0.45f
                }
                .background(color, CircleShape),
        )
    }
}

@Composable
private fun ReadingRippleRing(circleSize: Dp, color: Color) {
    val scale = remember { Animatable(1f) }
    val rippleAlpha = remember { Animatable(0.42f) }

    LaunchedEffect(Unit) {
        // Shrink quickly, then expand to a capped size and stay until state leaves [Reading].
        scale.snapTo(1f)
        rippleAlpha.snapTo(0.42f)
        scale.animateTo(
            targetValue = ReadingRippleShrinkScale,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        )
        scale.animateTo(
            targetValue = ReadingRippleExpandedScale,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = Modifier
            .size(circleSize)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = rippleAlpha.value
            }
            .background(color, CircleShape),
    )
}
