package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripePrimaryButtonBinding
import com.stripe.android.uicore.PrimaryButtonStyle
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.convertDpToPx
import com.stripe.android.uicore.getBorderStrokeColor
import com.stripe.android.uicore.getComposeTextStyle
import com.stripe.android.uicore.getOnBackgroundColor
import com.stripe.android.uicore.getOnSuccessBackgroundColor
import com.stripe.android.uicore.getSuccessBackgroundColor

/**
 * The primary call-to-action for a payment sheet screen.
 */
internal class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    @VisibleForTesting
    internal var defaultTintList: ColorStateList? = null
    private var state: State? = null
    private val animator = PrimaryButtonAnimator(context)

    // This is the text set by the client.  The internal label text is set to this value
    // in the on ready state and it is temporarily replaced during the processing and finishing states.
    private var originalLabel: String? = null

    private var defaultLabelColor: Int? = null

    @VisibleForTesting
    internal var externalLabel: String? = null

    @VisibleForTesting
    internal val viewBinding = StripePrimaryButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    internal var lockVisible = true

    private val confirmedIcon = viewBinding.confirmedIcon

    private var cornerRadius = context.convertDpToPx(
        StripeThemeDefaults.primaryButtonStyle.shape.cornerRadius.dp
    )
    private var borderStrokeWidth = context.convertDpToPx(
        StripeThemeDefaults.primaryButtonStyle.shape.borderStrokeWidth.dp
    )
    private var borderStrokeColor =
        StripeThemeDefaults.primaryButtonStyle.getBorderStrokeColor(context)

    private var finishedBackgroundColor =
        StripeThemeDefaults.primaryButtonStyle.getSuccessBackgroundColor(context)

    private var finishedOnBackgroundColor =
        StripeThemeDefaults.primaryButtonStyle.getOnSuccessBackgroundColor(context)

    init {
        // This is only needed if the button is inside a fragment
        viewBinding.label.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        getTextAttributeValue(attrs)?.let {
            setLabel(it.toString())
        }

        isClickable = true
        isEnabled = false
    }

    fun setAppearanceConfiguration(
        primaryButtonStyle: PrimaryButtonStyle,
        tintList: ColorStateList?
    ) {
        cornerRadius = context.convertDpToPx(primaryButtonStyle.shape.cornerRadius.dp)
        borderStrokeWidth = context.convertDpToPx(primaryButtonStyle.shape.borderStrokeWidth.dp)
        borderStrokeColor = primaryButtonStyle.getBorderStrokeColor(context)
        viewBinding.lockIcon.imageTintList = ColorStateList.valueOf(
            primaryButtonStyle.getOnBackgroundColor(context)
        )
        defaultTintList = tintList
        backgroundTintList = tintList
        finishedBackgroundColor = primaryButtonStyle.getSuccessBackgroundColor(context)
        finishedOnBackgroundColor = primaryButtonStyle.getOnSuccessBackgroundColor(context)
    }

    fun setDefaultLabelColor(@ColorInt color: Int) {
        defaultLabelColor = color
    }

    fun setLockIconDrawable(@DrawableRes drawable: Int) {
        viewBinding.lockIcon.setImageResource(drawable)
    }

    fun setIndicatorColor(@ColorInt color: Int) {
        viewBinding.confirmingIcon.setIndicatorColor(color)
    }

    fun setConfirmedIconDrawable(@DrawableRes drawable: Int) {
        viewBinding.confirmedIcon.setImageResource(drawable)
    }

    override fun setBackgroundTintList(tintList: ColorStateList?) {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = cornerRadius
        shape.color = tintList
        shape.setStroke(borderStrokeWidth.toInt(), borderStrokeColor)

        background = shape
        setPadding(
            resources.getDimensionPixelSize(
                R.dimen.stripe_paymentsheet_primary_button_padding
            )
        )
    }

    private fun getTextAttributeValue(attrs: AttributeSet?): CharSequence? {
        var text: CharSequence? = null
        context.withStyledAttributes(
            attrs,
            listOf(android.R.attr.text).toIntArray()
        ) {
            text = getText(0)
        }
        return text
    }

    private fun setLabel(text: String?) {
        externalLabel = text
        text?.let {
            if (state !is State.StartProcessing) {
                originalLabel = text
            }
            viewBinding.label.setContent {
                LabelUI(
                    label = text,
                    color = defaultLabelColor,
                )
            }
        }
    }

    private fun onReadyState() {
        isClickable = true
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
        isClickable = false
        setLabel(
            resources.getString(R.string.stripe_paymentsheet_primary_button_processing)
        )
    }

    private fun onFinishProcessing(onAnimationEnd: () -> Unit) {
        isClickable = false
        backgroundTintList = ColorStateList.valueOf(finishedBackgroundColor)
        confirmedIcon.imageTintList = ColorStateList.valueOf(finishedOnBackgroundColor)

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

    fun updateUiState(uiState: UIState?) {
        isVisible = uiState != null

        if (uiState != null) {
            if (state !is State.StartProcessing && state !is State.FinishProcessing) {
                // If we're processing or finishing, we're not overriding the label
                setLabel(uiState.label)
            }

            isEnabled = uiState.enabled
            lockVisible = uiState.lockVisible
            setOnClickListener { uiState.onClick() }
        }
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
            null -> {}
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

    internal sealed class State(
        val isProcessing: Boolean
    ) {

        /**
         * The label will be applied if the value is not null.
         */
        object Ready : State(
            isProcessing = false
        )
        object StartProcessing : State(
            isProcessing = true
        )
        data class FinishProcessing(
            val onComplete: () -> Unit
        ) : State(
            isProcessing = true
        )
    }

    internal data class UIState(
        val label: String,
        val onClick: () -> Unit,
        val enabled: Boolean,
        val lockVisible: Boolean,
    )
}

@Composable
private fun LabelUI(label: String, color: Int?) {
    StripeTheme {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = color?.let { Color(it) } ?: Color.Unspecified,
            style = StripeTheme.primaryButtonStyle.getComposeTextStyle(),
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 5.dp)
        )
    }
}
