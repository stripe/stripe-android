package com.stripe.android.connect.example.ui.features.notificationbanner

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.NotificationBannerListener
import com.stripe.android.connect.NotificationBannerView
import com.stripe.android.connect.example.BaseActivity
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.appearance.AppearanceInfo
import com.stripe.android.connect.example.ui.appearance.AppearanceView
import com.stripe.android.connect.example.ui.appearance.AppearanceViewModel
import com.stripe.android.connect.example.ui.common.BackIconButton
import com.stripe.android.connect.example.ui.common.ConnectExampleScaffold
import com.stripe.android.connect.example.ui.common.ConnectSdkExampleTheme
import com.stripe.android.connect.example.ui.common.CustomizeAppearanceIconButton
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentManagerLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApi")
@AndroidEntryPoint
class NotificationBannerExampleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConnectSdkExampleTheme {
                NotificationBannerExample()
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun NotificationBannerExample() {
        val managerState by loaderViewModel.state.collectAsState()
        val appearanceViewModel = hiltViewModel<AppearanceViewModel>()
        val appearanceState by appearanceViewModel.state.collectAsState()
        val sheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
        )
        val coroutineScope = rememberCoroutineScope()

        ModalBottomSheetLayout(
            modifier = Modifier.fillMaxSize(),
            sheetState = sheetState,
            sheetContent = {
                AppearanceView(
                    viewModel = appearanceViewModel,
                    onDismiss = { coroutineScope.launch { sheetState.hide() } },
                )
            },
        ) {
            ConnectExampleScaffold(
                title = getString(R.string.notification_banner),
                navigationIcon = { BackIconButton(onClick = ::finish) },
                actions = {
                    CustomizeAppearanceIconButton(
                        onClick = { coroutineScope.launch { sheetState.show() } }
                    )
                },
            ) {
                EmbeddedComponentManagerLoader(
                    embeddedComponentAsync = managerState.embeddedComponentManagerAsync,
                    reload = loaderViewModel::reload,
                    openSettings = {},
                ) { manager ->
                    NotificationBannerContent(
                        manager = manager,
                        appearanceId = appearanceState.selectedAppearance,
                    )
                }
            }
        }
    }

    @Composable
    @Suppress("LongMethod")
    private fun NotificationBannerContent(
        manager: EmbeddedComponentManager,
        appearanceId: AppearanceInfo.AppearanceId,
    ) {
        val hostAppearance = hostAppearance(appearanceId)
        var initialLoadState by remember(manager) {
            mutableStateOf(NotificationBannerView.InitialLoadState.LOADING)
        }
        var notificationTotal by remember(manager) { mutableIntStateOf(0) }
        var actionRequiredCount by remember(manager) { mutableIntStateOf(0) }
        var contentHeight by remember(manager) { mutableIntStateOf(0) }
        val bannerAlpha by animateFloatAsState(
            targetValue = if (initialLoadState == NotificationBannerView.InitialLoadState.LOADED) 1f else 0f,
            animationSpec = tween(BANNER_TRANSITION_DURATION_MILLIS),
            label = "notification banner alpha",
        )
        val listener = remember(manager) {
            object : NotificationBannerListener {
                override fun onNotificationsChanged(total: Int, actionRequired: Int) {
                    notificationTotal = total
                    this@NotificationBannerExampleActivity.logNotificationCount(total, actionRequired)
                    actionRequiredCount = actionRequired
                }

                override fun onContentHeightChanged(height: Int) {
                    contentHeight = height
                }

                override fun onInitialLoadStateChanged(state: NotificationBannerView.InitialLoadState) {
                    initialLoadState = state
                }

                override fun onLoadError(error: Throwable) {
                    Log.e(LOG_TAG, "Notification banner failed to load", error)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(hostAppearance.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(bannerAlpha),
                    factory = { context ->
                        val bannerView = manager.createNotificationBannerView(
                            context = context,
                            listener = listener,
                            cacheKey = "NotificationBannerExampleActivity",
                        )
                        bannerView.taskTitle = "Update information"
                        initialLoadState = bannerView.initialLoadState
                        bannerView
                    },
                )
                LoadingSkeleton(
                    visible = initialLoadState == NotificationBannerView.InitialLoadState.LOADING,
                    appearance = hostAppearance,
                )
            }

            Text(
                text = "loadState → $initialLoadState\n" +
                    "notifications → total: $notificationTotal, actionRequired: $actionRequiredCount\n" +
                    "height → ${contentHeight}px",
                color = hostAppearance.secondaryText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Text(
                text = "YOUR APP CONTENT",
                color = hostAppearance.secondaryText,
                fontFamily = hostAppearance.fontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            HostCard(hostAppearance) {
                Text("Available balance", color = hostAppearance.secondaryText)
                Text(
                    text = "\$2,438.19",
                    color = hostAppearance.text,
                    fontFamily = hostAppearance.fontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            SAMPLE_PAYMENTS.forEach { payment ->
                HostCard(hostAppearance) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(payment.first, color = hostAppearance.text)
                            Text(payment.second, color = hostAppearance.secondaryText, fontSize = 12.sp)
                        }
                        Text(payment.third, color = hostAppearance.text)
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingSkeleton(visible: Boolean, appearance: HostAppearance) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(BANNER_TRANSITION_DURATION_MILLIS)),
            exit = fadeOut(tween(BANNER_TRANSITION_DURATION_MILLIS)),
        ) {
            NotificationBannerSkeleton(appearance)
        }
    }

    @Composable
    @Suppress("MagicNumber")
    private fun NotificationBannerSkeleton(appearance: HostAppearance) {
        val transition = rememberInfiniteTransition(label = "notification banner skeleton")
        val alpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "skeleton alpha",
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp),
            color = appearance.surface,
            shape = RoundedCornerShape(appearance.cornerRadius.dp),
            border = BorderStroke(1.dp, appearance.border),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SkeletonBar(156, 16, appearance.secondaryText.copy(alpha = 0.18f * alpha))
                    SkeletonBar(220, 12, appearance.secondaryText.copy(alpha = 0.18f * alpha))
                }
                Spacer(Modifier.width(16.dp))
                SkeletonBar(72, 36, appearance.secondaryText.copy(alpha = 0.18f * alpha))
            }
        }
    }

    @Composable
    private fun SkeletonBar(width: Int, height: Int, color: Color) {
        Spacer(
            Modifier
                .width(width.dp)
                .height(height.dp)
                .background(color, RoundedCornerShape((height / 2).dp))
        )
    }

    @Composable
    private fun HostCard(appearance: HostAppearance, content: @Composable ColumnScope.() -> Unit) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = appearance.surface,
            shape = RoundedCornerShape(appearance.cornerRadius.dp),
            border = BorderStroke(1.dp, appearance.border),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }

    @Composable
    @Suppress("LongMethod", "MagicNumber")
    private fun hostAppearance(id: AppearanceInfo.AppearanceId): HostAppearance {
        val context = LocalContext.current
        val defaults = HostAppearance(
            background = MaterialTheme.colors.background,
            surface = MaterialTheme.colors.surface,
            text = MaterialTheme.colors.onSurface,
            secondaryText = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
            border = MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
            cornerRadius = 8f,
            fontFamily = FontFamily.Default,
        )
        fun color(resource: Int) = Color(ContextCompat.getColor(context, resource))

        return when (id) {
            AppearanceInfo.AppearanceId.Default,
            AppearanceInfo.AppearanceId.CustomFont -> defaults
            AppearanceInfo.AppearanceId.Ogre -> defaults.copy(
                background = color(R.color.ogre_background),
                surface = color(R.color.ogre_background),
                text = color(R.color.ogre_text),
            )
            AppearanceInfo.AppearanceId.HotDog -> defaults.copy(
                background = color(R.color.hot_dog_background),
                surface = color(R.color.hot_dog_offset_background),
                text = color(R.color.hot_dog_text),
                secondaryText = color(R.color.hot_dog_secondary_text),
                cornerRadius = 0f,
            )
            AppearanceInfo.AppearanceId.OceanBreeze -> defaults.copy(
                background = color(R.color.ocean_breeze_background),
                surface = color(R.color.ocean_breeze_background),
                cornerRadius = 23f,
            )
            AppearanceInfo.AppearanceId.Link -> defaults.copy(
                background = color(R.color.link_background),
                surface = color(R.color.link_background),
                text = color(R.color.link_text),
                secondaryText = color(R.color.link_secondary_text),
                cornerRadius = 5f,
            )
            AppearanceInfo.AppearanceId.Dynamic -> defaults.copy(
                background = color(R.color.dynamic_colors_background),
                surface = color(R.color.dynamic_colors_offset_background),
                text = color(R.color.dynamic_colors_text),
                secondaryText = color(R.color.dynamic_colors_secondary_text),
                border = color(R.color.dynamic_colors_border),
            )
            AppearanceInfo.AppearanceId.Retro -> defaults.copy(
                background = color(R.color.retro_background),
                surface = color(R.color.retro_offset_background),
                text = color(R.color.retro_text),
                secondaryText = color(R.color.retro_secondary_text),
                border = color(R.color.retro_border),
                cornerRadius = 0f,
                fontFamily = FontFamily.Monospace,
            )
            AppearanceInfo.AppearanceId.Forest -> defaults.copy(
                background = color(R.color.forest_background),
                surface = color(R.color.forest_offset_background),
                secondaryText = color(R.color.forest_secondary_text),
                border = color(R.color.forest_border),
                cornerRadius = 24f,
            )
            AppearanceInfo.AppearanceId.DarkMode -> defaults.copy(
                background = color(R.color.dark_mode_background),
                surface = color(R.color.dark_mode_offset_background),
                text = color(R.color.dark_mode_text),
                secondaryText = color(R.color.dark_mode_secondary_text),
                border = color(R.color.dark_mode_border),
            )
        }
    }

    private fun logNotificationCount(total: Int, actionRequired: Int) {
        Log.d(LOG_TAG, "Notifications changed: total=$total, actionRequired=$actionRequired")
    }

    private data class HostAppearance(
        val background: Color,
        val surface: Color,
        val text: Color,
        val secondaryText: Color,
        val border: Color,
        val cornerRadius: Float,
        val fontFamily: FontFamily,
    )

    private companion object {
        private const val BANNER_TRANSITION_DURATION_MILLIS = 200
        private const val LOG_TAG = "NotificationBanner"
        private val SAMPLE_PAYMENTS = listOf(
            Triple("Acme Coffee Co.", "Today, 9:41 AM", "\$4.50"),
            Triple("Blue Bottle", "Today, 8:12 AM", "\$6.25"),
            Triple("Corner Bakery", "Yesterday", "\$12.80"),
            Triple("Downtown Deli", "Yesterday", "\$18.40"),
            Triple("Elm St. Grocers", "Mar 12", "\$54.10"),
        )
    }
}
