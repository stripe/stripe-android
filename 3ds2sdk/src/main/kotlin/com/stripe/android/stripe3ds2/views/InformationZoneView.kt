package com.stripe.android.stripe3ds2.views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import com.stripe.android.stripe3ds2.databinding.StripeInformationZoneViewBinding
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization

/**
 * A view for 3DS2 information to be displayed and expanded
 */
internal class InformationZoneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewBinding = StripeInformationZoneViewBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    @VisibleForTesting
    internal val whyLabel = viewBinding.whyLabel
    @VisibleForTesting
    internal val whyText = viewBinding.whyText
    @VisibleForTesting
    internal val whyContainer = viewBinding.whyContainer
    @VisibleForTesting
    internal val whyArrow = viewBinding.whyArrow

    @VisibleForTesting
    internal val expandLabel = viewBinding.expandLabel
    @VisibleForTesting
    internal val expandText = viewBinding.expandText
    @VisibleForTesting
    internal val expandContainer = viewBinding.expandContainer
    @VisibleForTesting
    internal val expandArrow = viewBinding.expandArrow

    @ColorInt
    internal var toggleColor: Int = 0
    @ColorInt
    private var defaultColor: Int = 0

    /**
     * duration of arrow rotation animation
     */
    private val animationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

    /**
     * isEnabled for arrows and labels provides a hook for customizing colors based on toggled state
     */
    init {
        whyContainer.setOnClickListener { toggleView(whyArrow, whyLabel, whyText) }
        expandContainer.setOnClickListener { toggleView(expandArrow, expandLabel, expandText) }
    }

    /**
     * Set the why info section label and text to display when the why info section is expanded
     *
     * @param whyInfoLabel The label to display
     * @param whyInfoText The text that is initially hidden until expanded by the user
     */
    fun setWhyInfo(
        whyInfoLabel: String?,
        whyInfoText: String?,
        labelCustomization: LabelCustomization? = null
    ) {
        if (whyInfoLabel.isNullOrBlank()) {
            return
        }

        whyLabel.setText(whyInfoLabel, labelCustomization)
        whyContainer.visibility = View.VISIBLE
        whyText.setText(whyInfoText, labelCustomization)
    }

    /**
     * Set the expand info section label and text to display when the ex
     *
     * @param expandInfoLabel the label to display
     * @param expandInfoText the text that is initially hidden until expanded by the user
     */
    fun setExpandInfo(
        expandInfoLabel: String?,
        expandInfoText: String?,
        labelCustomization: LabelCustomization? = null
    ) {
        if (expandInfoLabel.isNullOrBlank()) {
            return
        }

        expandLabel.setText(expandInfoLabel, labelCustomization)
        expandContainer.visibility = View.VISIBLE
        expandText.setText(expandInfoText, labelCustomization)
    }

    private fun toggleView(arrow: View, label: TextView, detailsView: View) {
        val toggleOpen = detailsView.visibility == View.GONE

        val rotation = (if (toggleOpen) 180 else 0).toFloat()
        val arrowAnimator = ObjectAnimator.ofFloat(arrow, "rotation", rotation)
        arrowAnimator.duration = animationDuration.toLong()
        arrowAnimator.start()

        label.isEnabled = toggleOpen
        arrow.isEnabled = toggleOpen

        if (toggleColor != 0) {
            if (defaultColor == 0) {
                defaultColor = label.textColors.defaultColor
            }
            label.setTextColor(if (toggleOpen) toggleColor else defaultColor)
        }

        detailsView.visibility = if (toggleOpen) View.VISIBLE else View.GONE

        if (toggleOpen) {
            detailsView.postDelayed(
                {
                    val rect = Rect(0, 0, detailsView.width, detailsView.height)
                    detailsView.getHitRect(rect)
                    detailsView.requestRectangleOnScreen(rect, false)
                },
                animationDuration.toLong()
            )
        }
    }

    fun expandViews() {
        expandArrow.rotation = 180f
        whyArrow.rotation = 180f
        expandText.visibility = View.VISIBLE
        whyText.visibility = View.VISIBLE
    }
}
