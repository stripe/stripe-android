package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripeGooglePayButtonBinding
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.convertDpToPx
import com.stripe.android.uicore.isSystemDarkTheme
import org.json.JSONArray

@Composable
internal fun GooglePayButton(
    state: PrimaryButton.State?,
    allowCreditCards: Boolean,
    billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters?,
    isEnabled: Boolean,
    onPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cornerRadius = LocalContext.current.convertDpToPx(
        StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
    ).toInt()

    val isInspectionMode = LocalInspectionMode.current

    AndroidView(
        factory = { context -> GooglePayButton(context) },
        update = { googlePayButton ->
            if (!isInspectionMode) {
                googlePayButton.initialize(
                    cornerRadius = cornerRadius,
                    allowCreditCards = allowCreditCards,
                    billingAddressParameters = billingAddressParameters
                )
            }
            googlePayButton.isEnabled = isEnabled
            googlePayButton.updateState(state)
            googlePayButton.viewBinding.googlePayPaymentButton.setOnClickListener { onPressed() }
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
    }

    fun initialize(
        cornerRadius: Int,
        allowCreditCards: Boolean,
        billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters?
    ) {
        initializePrimaryButton()
        val isDark = context.isSystemDarkTheme()
        val allowedPaymentMethods = JSONArray().put(
            GooglePayJsonFactory(context).createCardPaymentMethod(
                billingAddressParameters = billingAddressParameters,
                allowCreditCards = allowCreditCards
            )
        ).toString()

        val options = with(ButtonOptions.newBuilder()) {
            setButtonTheme(
                if (isDark) {
                    ButtonConstants.ButtonTheme.LIGHT
                } else {
                    ButtonConstants.ButtonTheme.DARK
                }
            )
            setButtonType(ButtonConstants.ButtonType.PAY)
            setAllowedPaymentMethods(allowedPaymentMethods)
            setCornerRadius(
                // Corner radius of 0 is undefined in Google Pay
                if (cornerRadius <= 0) {
                    1
                } else {
                    cornerRadius
                }
            )
            build()
        }

        viewBinding.googlePayPaymentButton.initialize(options)
    }

    private fun initializePrimaryButton() {
        with(viewBinding.googlePayPrimaryButton) {
            setAppearanceConfiguration(
                StripeTheme.primaryButtonStyle,
                null
            )
            val backgroundColor = ContextCompat.getColor(
                context,
                R.color.stripe_paymentsheet_googlepay_primary_button_background_color
            )
            finishedBackgroundColor = backgroundColor
            backgroundTintList = ColorStateList.valueOf(backgroundColor)
            setLockIconDrawable(
                R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_lock
            )
            setIndicatorColor(
                ContextCompat.getColor(
                    context,
                    R.color.stripe_paymentsheet_googlepay_primary_button_tint_color,
                )
            )
            setConfirmedIconDrawable(
                R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_checkmark
            )
            setDefaultLabelColor(
                ContextCompat.getColor(
                    context,
                    R.color.stripe_paymentsheet_googlepay_primary_button_tint_color
                )
            )
        }
    }

    private fun onReadyState() {
        viewBinding.googlePayPrimaryButton.isVisible = false
        viewBinding.googlePayPaymentButton.isVisible = true
    }

    private fun onStartProcessing() {
        viewBinding.googlePayPrimaryButton.isVisible = true
        viewBinding.googlePayPaymentButton.isVisible = false
    }

    private fun onFinishProcessing() {
        viewBinding.googlePayPrimaryButton.isVisible = true
        viewBinding.googlePayPaymentButton.isVisible = false
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.googlePayPrimaryButton.isEnabled = enabled
        viewBinding.googlePayPaymentButton.isEnabled = enabled
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
