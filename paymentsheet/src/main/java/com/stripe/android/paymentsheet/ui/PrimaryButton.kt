package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
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
import com.stripe.android.uicore.strings.resolve

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
    private var originalLabel: ResolvableString? = null

    private var defaultLabelColor: Int? = null

    @VisibleForTesting
    internal var externalLabel: ResolvableString? = null

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
            setLabel(it.toString().resolvableString)
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

    private fun setLabel(text: ResolvableString?) {
        externalLabel = text
        text?.let {
            if (state !is State.StartProcessing) {
                originalLabel = text
            }
            viewBinding.label.setContent {
                LabelUI(
                    label = text.resolve(),
                    color = defaultLabelColor,
                )
            }
        }
    }

    private fun onReadyState() {
        updateLockVisibility(canShow = true)
        isClickable = true
        originalLabel?.let {
            ViewCompat.setStateDescription(this, it.resolve(context))
            setLabel(it)
        }
        defaultTintList?.let {
            backgroundTintList = it
        }
        viewBinding.lockIcon.isVisible = lockVisible
        viewBinding.confirmingIcon.isVisible = false
    }

    private fun onStartProcessing() {
        updateLockVisibility(canShow = false)
        viewBinding.confirmingIcon.isVisible = true
        isClickable = false

        val processingLabel = R.string.stripe_paymentsheet_primary_button_processing.resolvableString

        ViewCompat.setStateDescription(this, processingLabel.resolve(context))
        setLabel(processingLabel)
    }

    private fun onFinishProcessing(onAnimationEnd: () -> Unit) {
        updateLockVisibility(canShow = false)
        isClickable = false
        ViewCompat.setStateDescription(
            this,
            R.string.stripe_successful_transaction_description.resolvableString.resolve(context)
        )
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

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        // Indicate this custom view is a button, so TalkBack can announce it as such.
        info?.className = Button::class.java.name
        info?.isEnabled = isEnabled
    }

    fun updateUiState(uiState: UIState?) {
        isVisible = uiState != null

        if (uiState != null) {
            lockVisible = uiState.lockVisible

            if (state !is State.StartProcessing && state !is State.FinishProcessing) {
                // If we're processing or finishing, we're not overriding the label
                setLabel(uiState.label)
                ViewCompat.setStateDescription(this, uiState.label.resolve(context))
                updateLockVisibility(canShow = true)
            }

            isEnabled = uiState.enabled

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

    private fun updateLockVisibility(canShow: Boolean) {
        viewBinding.lockIcon.isVisible = lockVisible && canShow
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
        data object Ready : State(
            isProcessing = false
        )
        data object StartProcessing : State(
            isProcessing = true
        )
        data class FinishProcessing(
            val onComplete: () -> Unit
        ) : State(
            isProcessing = true
        )
    }

    internal data class UIState(
        val label: ResolvableString,
        val onClick: () -> Unit,
        val enabled: Boolean,
        val lockVisible: Boolean,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
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
                .semantics {
                    // This shouldn't be visible for accessibility purposes
                    // due to the content description and the click listener
                    // being defined outside of compose, in PrimaryButton.
                    invisibleToUser()
                }
        )
    }
}
