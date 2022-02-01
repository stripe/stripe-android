package com.stripe.android.stripe3ds2.views

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.FragmentActivity
import com.stripe.android.stripe3ds2.R
import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization
import com.stripe.android.stripe3ds2.utils.CustomizeUtils

/**
 * Customizes the activity's action bar as the header zone.
 */
internal class HeaderZoneCustomizer(
    private val activity: FragmentActivity
) {

    /**
     * Customizes the activity's action bar as the header zone
     *
     * @param toolbarCustomization customization for the header zone, if null use default
     * system styles and text labels
     * @param cancelButtonCustomization customization for the header zone cancel button, if null
     * uses default system button styles
     * @return The header zone cancel button
     */
    fun customize(
        toolbarCustomization: ToolbarCustomization? = null,
        cancelButtonCustomization: ButtonCustomization? = null
    ): ThreeDS2Button? {
        val appCompatActivity = (activity as? AppCompatActivity)
        val actionBar = appCompatActivity?.supportActionBar ?: return null
        val buttonContext = ContextThemeWrapper(activity, R.style.Stripe3DS2ActionBarButton)
        val cancelButton = ThreeDS2Button(buttonContext)
        cancelButton.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        cancelButton.setButtonCustomization(cancelButtonCustomization)

        val layoutParams = ActionBar.LayoutParams(
            ActionBar.LayoutParams.WRAP_CONTENT,
            ActionBar.LayoutParams.WRAP_CONTENT,
            Gravity.END or Gravity.CENTER_VERTICAL
        )
        actionBar.setCustomView(cancelButton, layoutParams)
        actionBar.setDisplayShowCustomEnabled(true)

        if (toolbarCustomization != null) {
            if (!toolbarCustomization.buttonText.isNullOrBlank()) {
                cancelButton.text = toolbarCustomization.buttonText
            } else {
                cancelButton.setText(R.string.stripe_3ds2_hzv_cancel_label)
            }

            toolbarCustomization.backgroundColor?.let { backgroundColor ->
                actionBar.setBackgroundDrawable(ColorDrawable(Color.parseColor(backgroundColor)))
                customizeStatusBar(appCompatActivity, toolbarCustomization)
            }

            val headerText: String = if (!toolbarCustomization.headerText.isNullOrBlank()) {
                toolbarCustomization.headerText
            } else {
                activity.getString(R.string.stripe_3ds2_hzv_header_label)
            }

            actionBar.title = CustomizeUtils.buildStyledText(
                activity,
                headerText,
                toolbarCustomization
            )
        } else {
            actionBar.setTitle(R.string.stripe_3ds2_hzv_header_label)
            cancelButton.setText(R.string.stripe_3ds2_hzv_cancel_label)
        }

        return cancelButton
    }

    companion object {
        /**
         * If a status bar color is provided, utilize it for the status bar, otherwise, darken the
         * toolbar background color and use that for the status bar.
         *
         * @param activity the activity to set the status bar color of
         * @param toolbarCustomization the toolbar customization data for customization
         */
        fun customizeStatusBar(
            activity: AppCompatActivity,
            toolbarCustomization: ToolbarCustomization
        ) {
            if (toolbarCustomization.statusBarColor != null) {
                CustomizeUtils.setStatusBarColor(
                    activity,
                    Color.parseColor(toolbarCustomization.statusBarColor)
                )
            } else if (toolbarCustomization.backgroundColor != null) {
                val backgroundColor = Color.parseColor(toolbarCustomization.backgroundColor)
                CustomizeUtils.setStatusBarColor(activity, CustomizeUtils.darken(backgroundColor))
            }
        }
    }
}
