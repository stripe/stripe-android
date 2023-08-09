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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripePrimaryButtonBinding
import com.stripe.android.uicore.PrimaryButtonStyle
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.convertDpToPx
import com.stripe.android.uicore.getBorderStrokeColor
import kotlinx.coroutines.flow.MutableStateFlow

private inline fun <T1, T2, T3> safeLet(
    value1: T1?,
    value2: T2?,
    value3: T3?,
    block: (T1, T2, T3) -> Unit,
) {
    if (value1 != null && value2 != null && value3 != null) {
        block(value1, value2, value3)
    }
}

private inline fun <T1, T2, T3, T4> safeLet(
    value1: T1?,
    value2: T2?,
    value3: T3?,
    value4: T4?,
    block: (T1, T2, T3, T4) -> Unit,
) {
    if (value1 != null && value2 != null && value3 != null && value4 != null) {
        block(value1, value2, value3, value4)
    }
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
//    private var state: State? = null
//    private val animator = PrimaryButtonAnimator(context)

    // This is the text set by the client.  The internal label text is set to this value
    // in the on ready state and it is temporarily replaced during the processing and finishing states.
//    private var originalLabel: String? = null

//    private var defaultLabelColor: Int? = null

    @VisibleForTesting
    internal var externalLabel: String? = null

    @VisibleForTesting
    internal val viewBinding = StripePrimaryButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    internal var lockVisible = true

//    private val confirmedIcon = viewBinding.confirmedIcon

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

    private val uiStateFlow = MutableStateFlow<UIState?>(null)
    private val stateFlow = MutableStateFlow<State?>(null)
    private val styleFlow = MutableStateFlow<PrimaryButtonStyle?>(null)
    private val backgroundFlow = MutableStateFlow<Int?>(null)
    private val defaultLabelColorFlow = MutableStateFlow<Int?>(null)
    private val lockIconDrawableFlow = MutableStateFlow<Int?>(null)
    private val confirmedIconDrawableFlow = MutableStateFlow<Int?>(null)
    private val indicatorColorFlow = MutableStateFlow<Int?>(null)

    init {
        // This is only needed if the button is inside a fragment
        viewBinding.content.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        viewBinding.content.setContent {
            val currentUiState by uiStateFlow.collectAsState()
            val currentState by stateFlow.collectAsState()
            val currentStyle by styleFlow.collectAsState()
            val currentBackground by backgroundFlow.collectAsState()
            val currentLabelColor by defaultLabelColorFlow.collectAsState()

            val lockIconDrawable by lockIconDrawableFlow.collectAsState()
            val confirmedIconDrawable by lockIconDrawableFlow.collectAsState()
            val indicatorColor by indicatorColorFlow.collectAsState()

//            safeLet(
//                currentUiState,
//                currentState,
//                currentStyle,
//                currentBackground,
//            ) { uiState, state, style, background ->
//                PrimaryButton(
//                    uiState = uiState,
//                    state = state,
//                    style = style,
//                    background = background,
//                    defaultLabelColor = currentLabelColor,
//                    lockIconDrawable = lockIconDrawable,
//                    confirmedIconDrawable = confirmedIconDrawable,
//                    indicatorColor = indicatorColor,
//                    modifier = Modifier.padding(
//                        top = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing),
//                        start = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal),
//                        end = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal),
//                    ),
//                )
//            }
        }

        isClickable = true
        isEnabled = false
    }

    fun setAppearanceConfiguration(
        primaryButtonStyle: PrimaryButtonStyle,
        tintList: ColorStateList?
    ) {
        styleFlow.value = primaryButtonStyle
        backgroundFlow.value = tintList?.defaultColor

//        cornerRadius = context.convertDpToPx(primaryButtonStyle.shape.cornerRadius.dp)
//        borderStrokeWidth = context.convertDpToPx(primaryButtonStyle.shape.borderStrokeWidth.dp)
//        borderStrokeColor = primaryButtonStyle.getBorderStrokeColor(context)
//        viewBinding.lockIcon.imageTintList = ColorStateList.valueOf(
//            primaryButtonStyle.getOnBackgroundColor(context)
//        )
//        defaultTintList = tintList
//        backgroundTintList = tintList
    }

    fun setDefaultLabelColor(@ColorInt color: Int) {
        defaultLabelColorFlow.value = color
    }

    fun setLockIconDrawable(@DrawableRes drawable: Int) {
        lockIconDrawableFlow.value = drawable
    }

    fun setIndicatorColor(@ColorInt color: Int) {
        indicatorColorFlow.value = color
    }

    fun setConfirmedIconDrawable(@DrawableRes drawable: Int) {
        confirmedIconDrawableFlow.value = drawable
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

//    private fun getTextAttributeValue(attrs: AttributeSet?): CharSequence? {
//        var text: CharSequence? = null
//        context.withStyledAttributes(
//            attrs,
//            listOf(android.R.attr.text).toIntArray()
//        ) {
//            text = getText(0)
//        }
//        return text
//    }

//    private fun setLabel(text: String?) {
//        externalLabel = text
//        text?.let {
//            if (state !is State.StartProcessing) {
//                originalLabel = text
//            }
//            viewBinding.label.setContent {
//                LabelUI(
//                    label = text,
//                    color = defaultLabelColor,
//                )
//            }
//        }
//    }

//    private fun onReadyState() {
//        isClickable = true
//        originalLabel?.let {
//            setLabel(it)
//        }
//        defaultTintList?.let {
//            backgroundTintList = it
//        }
//        viewBinding.lockIcon.isVisible = lockVisible
//        viewBinding.confirmingIcon.isVisible = false
//    }

//    private fun onStartProcessing() {
//        viewBinding.lockIcon.isVisible = false
//        viewBinding.confirmingIcon.isVisible = true
//        isClickable = false
//        setLabel(
//            resources.getString(R.string.stripe_paymentsheet_primary_button_processing)
//        )
//    }

//    private fun onFinishProcessing(onAnimationEnd: () -> Unit) {
//        isClickable = false
//        backgroundTintList = ColorStateList.valueOf(finishedBackgroundColor)
//
//        animator.fadeOut(viewBinding.label)
//        animator.fadeOut(viewBinding.confirmingIcon)
//
//        animator.fadeIn(confirmedIcon, width) {
//            onAnimationEnd()
//        }
//    }

//    override fun setEnabled(enabled: Boolean) {
//        super.setEnabled(enabled)
//        updateAlpha()
//    }

    fun updateUiState(uiState: UIState?) {
        uiStateFlow.value = uiState
//        isVisible = uiState != null
//
//        if (uiState != null) {
//            if (state !is State.StartProcessing && state !is State.FinishProcessing) {
//                // If we're processing or finishing, we're not overriding the label
//                setLabel(uiState.label)
//            }
//
//            isEnabled = uiState.enabled
//            lockVisible = uiState.lockVisible
//            setOnClickListener { uiState.onClick() }
//        }
    }

    fun updateState(state: State?) {
        stateFlow.value = state
//        this.state = state
//        updateAlpha()
//
//        when (state) {
//            is State.Ready -> {
//                onReadyState()
//            }
//            State.StartProcessing -> {
//                onStartProcessing()
//            }
//            is State.FinishProcessing -> {
//                onFinishProcessing(state.onComplete)
//            }
//            null -> {}
//        }
    }

//    private fun updateAlpha() {
//        listOf(
//            viewBinding.label,
//            viewBinding.lockIcon
//        ).forEach { view ->
//            view.alpha = if ((state == null || state is State.Ready) && !isEnabled) {
//                0.5f
//            } else {
//                1.0f
//            }
//        }
//    }

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
