package com.stripe.android.connect.example.ui.features.notificationbanner

import android.content.Context
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.NotificationBannerListener
import com.stripe.android.connect.PrivatePreviewConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.BasicExampleComponentActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@OptIn(PrivatePreviewConnectSDK::class)
class NotificationBannerExampleActivity : BasicExampleComponentActivity() {
    override val titleRes: Int = R.string.notification_banner

    override fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View {
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val summary = TextView(context).apply {
            setPadding(PADDING, PADDING, PADDING, PADDING)
        }
        val banner = embeddedComponentManager.createNotificationBannerView(
            context = context,
            listener = object : NotificationBannerListener {
                override fun onNotificationsChange(total: Int, actionRequired: Int) {
                    summary.text = getString(R.string.notification_banner_summary, total, actionRequired)
                }

                override fun onContentHeightChange(height: Float) {
                    TransitionManager.beginDelayedTransition(content)
                }

                override fun onLoadError(error: Throwable) {
                    Toast.makeText(this@NotificationBannerExampleActivity, error.message, Toast.LENGTH_LONG).show()
                }
            },
            cacheKey = "NotificationBannerExampleActivity",
        )
        content.addView(banner, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        content.addView(summary, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return ScrollView(context).apply { addView(content, MATCH_PARENT, WRAP_CONTENT) }
    }

    private companion object {
        const val PADDING = 48
    }
}
