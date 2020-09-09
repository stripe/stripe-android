package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import com.stripe.android.R
import kotlin.properties.Delegates

/**
 * An [IconTextInputLayout] that can show a loading indicator.
 */
internal class CardNumberTextInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.textInputStyle
) : IconTextInputLayout(context, attrs, defStyleAttr) {
    private val progressView = CardWidgetProgressView(context, attrs, defStyleAttr)

    internal var isLoading: Boolean by Delegates.observable(
        false
    ) { _, wasLoading, isLoading ->
        if (SHOULD_SHOW_PROGRESS && wasLoading != isLoading) {
            if (isLoading) {
                progressView.show()
            } else {
                progressView.hide()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // add the progress view to the `TextInputLayout`'s `FrameLayout` container
        (children.first() as FrameLayout).addView(progressView)

        // absolutely position the progress view over the brand icon
        progressView.updateLayoutParams<FrameLayout.LayoutParams> {
            leftMargin = resources.getDimensionPixelSize(
                R.dimen.stripe_card_number_text_input_layout_progress_left_margin
            )
            topMargin = resources.getDimensionPixelSize(
                R.dimen.stripe_card_number_text_input_layout_progress_top_margin
            )
        }
    }

    private companion object {
        private const val SHOULD_SHOW_PROGRESS = false
    }
}
