package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.core.view.children
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import kotlin.properties.Delegates
import com.google.android.material.R as MaterialR

/**
 * An [TextInputLayout] that can show a loading indicator.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
class CardNumberTextInputLayout @JvmOverloads internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = MaterialR.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {
    private val progressView = CardWidgetProgressView(context, attrs, defStyleAttr)

    internal var isLoading: Boolean by Delegates.observable(
        false
    ) { _, wasLoading, isLoading ->
        if (wasLoading != isLoading) {
            if (isLoading) {
                progressView.show()
            } else {
                progressView.hide()
            }
        }
    }

    init {
        doOnNextLayout {
            attachProgressView()
        }

        placeholderText = resources.getString(R.string.stripe_card_number_hint)
    }

    private fun attachProgressView() {
        // remove parent from the progress view if already attached earlier
        (progressView.parent as? ViewGroup)?.removeView(progressView)

        // add the progress view to the `TextInputLayout`'s `FrameLayout` container
        val progressViewParent = children.first() as FrameLayout
        progressViewParent.addView(progressView)

        // absolutely position the progress view over the brand icon
        progressView.updateLayoutParams<FrameLayout.LayoutParams> {
            marginStart = progressViewParent.width -
                resources.getDimensionPixelSize(
                    R.dimen.stripe_card_number_text_input_layout_progress_end_margin
                )
            topMargin = resources.getDimensionPixelSize(
                R.dimen.stripe_card_number_text_input_layout_progress_top_margin
            )
        }
    }
}
