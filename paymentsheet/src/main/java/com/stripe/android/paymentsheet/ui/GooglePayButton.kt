package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import com.stripe.android.paymentsheet.databinding.StripeGooglePayButtonBinding

@Composable
internal fun GooglePayButton(
    state: PrimaryButton.State?,
    isEnabled: Boolean,
    onPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context -> GooglePayButton(context) },
        update = { googlePayButton ->
            googlePayButton.isEnabled = isEnabled
            googlePayButton.updateState(state)
            googlePayButton.setOnClickListener { onPressed() }
        },
        modifier = modifier.testTag(GooglePayButton.TEST_TAG),
    )
}

internal class GooglePayButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    internal val viewBinding = StripeGooglePayButtonBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private var state: PrimaryButton.State? = null

    init {
        // Call super so we don't inadvertently effect the primary button as well.
        super.setClickable(true)
        super.setEnabled(true)
        viewBinding.googlePayPrimaryButton.backgroundTintList = null
        viewBinding.googlePayPrimaryButton.finishedBackgroundColor = Color.TRANSPARENT
    }

    private fun onReadyState() {
        viewBinding.googlePayPrimaryButton.isVisible = false
        viewBinding.googlePayButtonContent.isVisible = true
    }

    private fun onStartProcessing() {
        viewBinding.googlePayPrimaryButton.isVisible = true
        viewBinding.googlePayButtonContent.isVisible = false
    }

    private fun onFinishProcessing() {
        viewBinding.googlePayPrimaryButton.isVisible = true
        viewBinding.googlePayButtonContent.isVisible = false
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.googlePayPrimaryButton.isEnabled = enabled
        updateAlpha()
    }

    private fun updateAlpha() {
        viewBinding.googlePayButtonLayout.alpha =
            if ((state == null || state is PrimaryButton.State.Ready) && !isEnabled) {
                0.5f
            } else {
                1.0f
            }
    }

    fun updateState(state: PrimaryButton.State?) {
        viewBinding.googlePayPrimaryButton.updateState(state)
        this.state = state
        updateAlpha()

        when (state) {
            is PrimaryButton.State.Ready -> {
                onReadyState()
            }
            PrimaryButton.State.StartProcessing -> {
                onStartProcessing()
            }
            is PrimaryButton.State.FinishProcessing -> {
                onFinishProcessing()
            }
            null -> {}
        }
    }

    internal companion object {
        const val TEST_TAG = "google-pay-button"
    }
}
