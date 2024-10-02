package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.DrawableRes
import androidx.core.widget.CompoundButtonCompat
import com.stripe.android.stripe3ds2.R
import com.stripe.android.stripe3ds2.databinding.StripeChallengeZoneViewBinding
import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization

@Suppress("TooManyFunctions")
internal class ChallengeZoneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    internal val infoHeader: ThreeDS2HeaderTextView
    internal val infoTextView: ThreeDS2TextView
    internal val infoLabelView: ThreeDS2TextView
    internal val submitButton: ThreeDS2Button
    internal val resendButton: ThreeDS2Button
    internal val whitelistingLabel: ThreeDS2TextView
    internal val whitelistRadioGroup: RadioGroup
    internal val challengeEntryView: FrameLayout
    internal val whitelistYesRadioButton: RadioButton
    internal val whitelistNoRadioButton: RadioButton

    /**
     * Get the whitelisting radio button selection
     *
     * @return True if the user has selected Yes, else false if the user selected No or made no
     * selection at all
     */
    internal val whitelistingSelection: Boolean
        get() = whitelistRadioGroup.checkedRadioButtonId == R.id.czv_whitelist_yes_button

    init {
        val viewBinding = StripeChallengeZoneViewBinding.inflate(
            LayoutInflater.from(context),
            this
        )
        infoHeader = viewBinding.czvHeader
        infoTextView = viewBinding.czvInfo
        infoLabelView = viewBinding.czvInfoLabel
        submitButton = viewBinding.czvSubmitButton
        resendButton = viewBinding.czvResendButton
        whitelistingLabel = viewBinding.czvWhitelistingLabel
        whitelistRadioGroup = viewBinding.czvWhitelistRadioGroup
        challengeEntryView = viewBinding.czvEntryView
        whitelistYesRadioButton = viewBinding.czvWhitelistYesButton
        whitelistNoRadioButton = viewBinding.czvWhitelistNoButton
    }

    /**
     * Sets the Challenge Zone information header and customized label. Null/empty header text will
     * hide the view.
     *
     * @param headerText challenge zone information header
     * @param labelCustomization header label customization
     */
    fun setInfoHeaderText(
        headerText: String?,
        labelCustomization: LabelCustomization? = null
    ) {
        if (headerText.isNullOrBlank()) {
            infoHeader.visibility = View.GONE
        } else {
            infoHeader.setText(headerText, labelCustomization)
        }
    }

    /**
     * Sets the challenge zone info text and label customization. Null/empty info text will hide
     * the view.
     *
     * @param infoText challenge zone info text
     * @param labelCustomization label customization for the info text
     */
    fun setInfoText(
        infoText: String?,
        labelCustomization: LabelCustomization? = null
    ) {
        if (infoText.isNullOrBlank()) {
            this.infoTextView.visibility = View.GONE
        } else {
            this.infoTextView.setText(infoText, labelCustomization)
        }
    }

    fun setInfoLabel(
        infoLabel: String?,
        labelCustomization: LabelCustomization? = null
    ) {
        if (infoLabel.isNullOrBlank()) {
            this.infoLabelView.visibility = View.GONE
        } else {
            this.infoLabelView.setText(infoLabel, labelCustomization)
        }
    }

    /**
     * Set the challenge information text indicator
     *
     * @param indicatorResId drawable resource id for the indicator to display
     */
    fun setInfoTextIndicator(@DrawableRes indicatorResId: Int) {
        infoTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(indicatorResId, 0, 0, 0)
    }

    /**
     * Set the challenge zone submit authentication button label
     *
     * @param submitButtonLabel challenge zone authentication label
     * @param buttonCustomization Button customization for the submit button
     */
    fun setSubmitButton(
        submitButtonLabel: String?,
        buttonCustomization: ButtonCustomization? = null
    ) {
        if (submitButtonLabel.isNullOrBlank()) {
            submitButton.visibility = View.GONE
        } else {
            submitButton.text = submitButtonLabel
            submitButton.setButtonCustomization(buttonCustomization)
        }
    }

    fun setSubmitButtonClickListener(listener: OnClickListener?) {
        submitButton.setOnClickListener(listener)
    }

    /**
     * Set the challenge zone resend information button label. If null, hide the resend button.
     *
     * @param resendButtonLabel challenge zone resend information label
     * @param buttonCustomization button customization info the resend button label
     */
    fun setResendButtonLabel(
        resendButtonLabel: String?,
        buttonCustomization: ButtonCustomization? = null
    ) {
        if (!resendButtonLabel.isNullOrBlank()) {
            resendButton.visibility = View.VISIBLE
            resendButton.text = resendButtonLabel
            resendButton.setButtonCustomization(buttonCustomization)
        }
    }

    fun setResendButtonClickListener(listener: OnClickListener?) {
        resendButton.setOnClickListener(listener)
    }

    /**
     * Set the whitelisting label. If not empty, also display the whitelisting entry radio buttons
     *
     * @param whitelistingLabel Challenge zone whitelisting information text
     * @param labelCustomization label customization for the whitelist label
     * @param buttonCustomization select button customization for radio button customization
     */
    fun setWhitelistingLabel(
        whitelistingLabel: String?,
        labelCustomization: LabelCustomization? = null,
        buttonCustomization: ButtonCustomization? = null
    ) {
        if (!whitelistingLabel.isNullOrBlank()) {
            this.whitelistingLabel.setText(whitelistingLabel, labelCustomization)
            if (buttonCustomization != null) {
                (0 until whitelistRadioGroup.childCount)
                    .mapNotNull {
                        whitelistRadioGroup.getChildAt(it) as? RadioButton
                    }
                    .forEach { radioButton ->
                        if (!buttonCustomization.backgroundColor.isNullOrBlank()) {
                            CompoundButtonCompat.setButtonTintList(
                                radioButton,
                                ColorStateList.valueOf(
                                    Color.parseColor(buttonCustomization.backgroundColor)
                                )
                            )
                        }
                        if (!buttonCustomization.textColor.isNullOrBlank()) {
                            radioButton.setTextColor(
                                Color.parseColor(buttonCustomization.textColor)
                            )
                        }
                    }
            }

            this.whitelistingLabel.visibility = View.VISIBLE
            whitelistRadioGroup.visibility = View.VISIBLE
        }
    }

    fun setWhitelistChecked(checked: Boolean) {
        whitelistYesRadioButton.isChecked = checked
        whitelistNoRadioButton.isChecked = !checked
    }

    /**
     * Set the challenge entry view below the challenge info text and above the buttons
     *
     * @param challengeEntryView the custom view for the challenge zone UI type
     */
    fun setChallengeEntryView(challengeEntryView: View) {
        this.challengeEntryView.addView(challengeEntryView)
    }
}
