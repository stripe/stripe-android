package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.uicore.PrimaryButtonStyle
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.convertDpToPx
import com.stripe.android.uicore.getBackgroundColor
import com.stripe.android.uicore.getBorderStrokeColor
import com.stripe.android.uicore.getComposeTextStyle
import com.stripe.android.uicore.getOnBackgroundColor

@Composable
internal fun PrimaryButton(
    uiState: PrimaryButton.UIState,
    modifier: Modifier = Modifier,
) {
    val height = dimensionResource(R.dimen.stripe_paymentsheet_primary_button_height)
    val topPadding = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing)
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    AndroidView(
        factory = { context ->
            PrimaryButton(context).apply {
                setAppearanceConfiguration(
                    primaryButtonStyle = StripeTheme.primaryButtonStyle,
                    tintList = uiState.color ?: ColorStateList.valueOf(
                        StripeTheme.primaryButtonStyle.getBackgroundColor(context)
                    )
                )
            }
        },
        update = { button -> button.updateUiState(uiState) },
        modifier = modifier
            .testTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)
            .padding(top = topPadding)
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth()
            .requiredHeight(height),
    )
}

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

    @VisibleForTesting
    internal val viewBinding = PrimaryButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val confirmedIcon = viewBinding.confirmedIcon

    private var cornerRadius = context.convertDpToPx(
        StripeThemeDefaults.primaryButtonStyle.shape.cornerRadius.dp
    )
    private var borderStrokeWidth = context.convertDpToPx(
        StripeThemeDefaults.primaryButtonStyle.shape.borderStrokeWidth.dp
    )
    private var borderStrokeColor =
        StripeThemeDefaults.primaryButtonStyle.getBorderStrokeColor(context)

    internal var finishedBackgroundColor =
        ContextCompat.getColor(
            context,
            R.color.stripe_paymentsheet_primary_button_success_background
        )

    init {
        // This is only needed if the button is inside a fragment
        viewBinding.label.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        getTextAttributeValue(attrs)?.let {
            setLabel(it.toString())
        }
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
        if (text != null) {
            viewBinding.label.setContent {
                LabelUI(label = text)
            }
        }
    }

    private fun onReadyState() {
        isClickable = true
        defaultTintList?.let {
            backgroundTintList = it
        }
        viewBinding.confirmingIcon.isVisible = false
    }

    private fun onStartProcessing() {
        viewBinding.confirmingIcon.isVisible = true
        isClickable = false
        setLabel(
            resources.getString(R.string.stripe_paymentsheet_primary_button_processing)
        )
    }

    private fun onFinishProcessing(onAnimationEnd: () -> Unit) {
        isClickable = false
        backgroundTintList = ColorStateList.valueOf(finishedBackgroundColor)

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
            setLabel(uiState.label)
            isEnabled = uiState.enabled
            viewBinding.lockIcon.isVisible = uiState.lockVisible
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

    internal sealed class State {
        /**
         * The label will be applied if the value is not null.
         */
        object Ready : State()
        object StartProcessing : State()
        data class FinishProcessing(val onComplete: () -> Unit) : State()
    }

    internal data class UIState(
        val processingState: State,
        val label: String,
        val onClick: () -> Unit,
        val enabled: Boolean,
        val lockVisible: Boolean,
        val color: ColorStateList?,
    )
}

@Composable
private fun LabelUI(label: String) {
    StripeTheme {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = StripeTheme.primaryButtonStyle.getComposeTextStyle(),
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 5.dp)
        )
    }
}
