package com.stripe.android.connect.example.ui.features.notificationbanner

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.NotificationBannerListener
import com.stripe.android.connect.PrivatePreviewConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.connect.example.ui.appearance.AppearanceInfo
import com.stripe.android.connect.example.ui.common.BasicExampleComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * A sample home screen with the notification banner above the app's own content, which is placeholder data.
 * The screen takes its colors and corner radius from the selected appearance, so the banner blends in.
 */
@AndroidEntryPoint
@OptIn(PrivatePreviewConnectSDK::class)
@Suppress("MagicNumber")
class NotificationBannerExampleActivity : BasicExampleComponentActivity() {
    override val titleRes: Int = R.string.notification_banner_home

    @Inject
    lateinit var settingsService: SettingsService

    override fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = context.dp(16)
            setPadding(padding, padding, padding, context.dp(24))
        }
        val placeholder = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        lateinit var banner: View
        banner = embeddedComponentManager.createNotificationBannerView(
            context = context,
            listener = object : NotificationBannerListener {
                override fun onContentHeightChange(height: Float) {
                    TransitionManager.beginDelayedTransition(content)
                    // No gap while the banner has nothing to show
                    placeholder.updateLayoutParams<LinearLayout.LayoutParams> {
                        topMargin = if (height > 0) context.dp(16) else 0
                    }
                }

                override fun onLoadError(error: Throwable) {
                    banner.isVisible = false
                    Toast.makeText(context, R.string.notification_banner_load_error, Toast.LENGTH_LONG).show()
                }
            },
            cacheKey = "NotificationBannerExampleActivity",
        )
        content.addView(banner, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        content.addView(placeholder, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        val scrollView = ScrollView(context).apply { addView(content, MATCH_PARENT, WRAP_CONTENT) }

        lifecycleScope.launch {
            settingsService.getAppearanceIdFlow().collect { appearanceId ->
                val style = SampleScreenStyle.from(appearanceId ?: AppearanceInfo.AppearanceId.Default, context)
                scrollView.setBackgroundColor(style.background)
                placeholder.removeAllViews()
                PlaceholderContent(context, style).addTo(placeholder)
            }
        }
        return scrollView
    }
}

@Suppress("MagicNumber")
private class PlaceholderContent(private val context: Context, private val style: SampleScreenStyle) {
    private data class Activity(val title: String, val detail: String, val amount: String)

    fun addTo(container: LinearLayout) {
        container.addView(balanceCard(), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        container.addView(
            label("Recent activity", TEXT_SIZE_HEADLINE, bold = true),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = context.dp(24)
                bottomMargin = context.dp(8)
            },
        )
        container.addView(activityCard(), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun balanceCard(): View {
        return card(verticalPadding = 16).apply {
            addView(label("Available balance", TEXT_SIZE_CAPTION, color = style.secondaryText))
            addView(label("$12,480.55", TEXT_SIZE_BALANCE, bold = true))
            addView(label("Next payout of $3,210.00 on Dec 2", TEXT_SIZE_CAPTION, color = style.secondaryText))
        }
    }

    private fun activityCard(): View {
        return card(verticalPadding = 4).apply {
            ACTIVITIES.forEachIndexed { index, activity ->
                if (index > 0) {
                    addView(
                        View(context).apply { setBackgroundColor(style.border) },
                        LinearLayout.LayoutParams(MATCH_PARENT, 1),
                    )
                }
                addView(activityRow(activity))
            }
        }
    }

    private fun activityRow(activity: Activity): View {
        val text = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(activity.title, TEXT_SIZE_BODY))
            addView(label(activity.detail, TEXT_SIZE_CAPTION, color = style.secondaryText))
        }
        return LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(12), 0, context.dp(12))
            addView(text, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(label(activity.amount, TEXT_SIZE_BODY, bold = true))
        }
    }

    private fun card(verticalPadding: Int): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(verticalPadding), context.dp(16), context.dp(verticalPadding))
            background = GradientDrawable().apply {
                cornerRadius = context.dp(style.cornerRadiusDp).toFloat()
                setStroke(context.dp(1), style.border)
            }
        }
    }

    private fun label(text: String, sizeSp: Float, bold: Boolean = false, color: Int = style.text): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    }

    private companion object {
        const val TEXT_SIZE_CAPTION = 13f
        const val TEXT_SIZE_BODY = 16f
        const val TEXT_SIZE_HEADLINE = 17f
        const val TEXT_SIZE_BALANCE = 32f

        val ACTIVITIES = listOf(
            Activity("Jordan Lee", "Payment · Today, 9:41 AM", "+$84.00"),
            Activity("Bank account ••6789", "Payout · Yesterday", "−$3,000.00"),
            Activity("Sam Park", "Payment · Yesterday", "+$126.50"),
            Activity("Alex Kim", "Refund · Nov 20", "−$24.50"),
            Activity("Riley Chen", "Payment · Nov 19", "+$310.00"),
        )
    }
}

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
