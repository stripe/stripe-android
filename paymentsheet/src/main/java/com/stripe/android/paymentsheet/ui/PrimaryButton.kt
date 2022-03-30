package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.PaymentsThemeConfig
import com.stripe.android.ui.core.convertDpToPx

/**
 * The primary call-to-action for a payment sheet screen.
 */
internal class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var defaultTintList: ColorStateList? = null
    private var state: State? = null
    private val animator = PrimaryButtonAnimator(context)

    // This is the text set by the client.  The internal label text is set to this value
    // in the on ready state and it is temporarily replaced during the processing and finishing states.
    private var originalLabel: String? = null

    @VisibleForTesting
    internal var externalLabel: String? = null

    @VisibleForTesting
    internal val viewBinding = PrimaryButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    internal var lockVisible = true

    private val confirmedIcon = viewBinding.confirmedIcon

    init {
        // This is only needed if the button is inside a fragment
        viewBinding.label.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        isClickable = true
        isEnabled = false
    }

    fun setDefaultBackGroundColor(tintList: ColorStateList?) {
        backgroundTintList = tintList
        defaultTintList = tintList
    }

    override fun setBackgroundTintList(tintList: ColorStateList?) {
        val cornerRadius = context.convertDpToPx(PaymentsThemeConfig.Shapes.cornerRadius)

        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = cornerRadius
        shape.color = tintList

        background = shape
        setPadding(cornerRadius.toInt())
    }

    fun setLabel(text: String?) {
        externalLabel = text
        if (state !is State.StartProcessing) {
            originalLabel = text
        }
        text?.let {
            viewBinding.label.setContent {
                LabelUI(label = text)
            }
        }
    }

    private fun onReadyState() {
        originalLabel?.let {
            setLabel(it)
        }
        defaultTintList?.let {
            backgroundTintList = it
        }
        viewBinding.lockIcon.isVisible = lockVisible
        viewBinding.confirmingIcon.isVisible = false
    }

    private fun onStartProcessing() {
        viewBinding.lockIcon.isVisible = false
        viewBinding.confirmingIcon.isVisible = true

        setLabel(
            resources.getString(R.string.stripe_paymentsheet_primary_button_processing)
        )
    }

    private fun onFinishProcessing(onAnimationEnd: () -> Unit) {
        backgroundTintList = ColorStateList.valueOf(
            resources.getColor(R.color.stripe_paymentsheet_primary_button_success_background)
        )

        animator.fadeOut(viewBinding.label)
        animator.fadeOut(viewBinding.confirmingIcon)

        animator.fadeIn(confirmedIcon, width) {
            onAnimationEnd()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        updateAlpha()
    }

    fun updateState(state: State?) {
        this.state = state
        updateAlpha()

        when (state) {
            is State.Ready -> {
                onReadyState()
            }
            State.StartProcessing -> {
                onStartProcessing()
            }
            is State.FinishProcessing -> {
                onFinishProcessing(state.onComplete)
            }
        }
    }

    private fun updateAlpha() {
        listOf(
            viewBinding.label,
            viewBinding.lockIcon
        ).forEach { view ->
            view.alpha = if ((state == null || state is State.Ready) && !isEnabled) {
                0.5f
            } else {
                1.0f
            }
        }
    }

    internal sealed class State {
        /**
         * The label will be applied if the value is not null.
         */
        object Ready : State()
        object StartProcessing : State()
        data class FinishProcessing(val onComplete: () -> Unit) : State()
    }
}

@Composable
private fun LabelUI(label: String) {
    Text(
        text = label,
        textAlign = TextAlign.Center,
        color = colorResource(R.color.stripe_paymentsheet_primary_button_text),
        style = PaymentsTheme.typography.h5,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 5.dp)
    )
}
