package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.core.os.bundleOf
import androidx.core.widget.CompoundButtonCompat
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import com.stripe.android.stripe3ds2.R
import com.stripe.android.stripe3ds2.databinding.StripeChallengeZoneMultiSelectViewBinding
import com.stripe.android.stripe3ds2.databinding.StripeChallengeZoneSingleSelectViewBinding
import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import java.util.ArrayList

internal class ChallengeZoneSelectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val isSingleSelectMode: Boolean = false
) : FrameLayout(context, attrs, defStyleAttr), FormField {

    internal val infoLabel: ThreeDS2TextView
    internal val selectGroup: LinearLayout
    private val buttonBottomMargin: Int
    private val buttonLabelPadding: Int
    private val buttonOffsetMargin: Int
    private val buttonMinHeight: Int

    override val userEntry: String
        get() = selectedOptions.joinToString(separator = ",") { it.name }

    val checkBoxes: List<CheckBox?>?
        get() {
            if (isSingleSelectMode) {
                return null
            }
            val count = selectGroup.childCount
            return (0 until count).map { selectGroup.getChildAt(it) as CheckBox }
        }

    val selectedOptions: List<ChallengeResponseData.ChallengeSelectOption>
        get() {
            return selectedIndexes.map {
                selectGroup.getChildAt(it).tag as ChallengeResponseData.ChallengeSelectOption
            }
        }

    internal val selectedIndexes: List<Int>
        get() {
            val checkedButtons = (0 until selectGroup.childCount).mapNotNull {
                if ((selectGroup.getChildAt(it) as CompoundButton).isChecked) {
                    it
                } else {
                    null
                }
            }

            return checkedButtons
                .take(if (isSingleSelectMode) 1 else checkedButtons.size)
        }

    init {
        if (id == View.NO_ID) {
            id = R.id.stripe_3ds2_default_challenge_zone_select_view_id
        }
        buttonBottomMargin = context.resources
            .getDimensionPixelSize(R.dimen.stripe_3ds2_challenge_zone_select_button_vertical_margin)
        buttonLabelPadding = context.resources
            .getDimensionPixelSize(R.dimen.stripe_3ds2_challenge_zone_select_button_label_padding)
        buttonOffsetMargin = context.resources
            .getDimensionPixelSize(R.dimen.stripe_3ds2_challenge_zone_select_button_offset_margin)
        buttonMinHeight = context.resources
            .getDimensionPixelSize(R.dimen.stripe_3ds2_challenge_zone_select_button_min_height)

        if (isSingleSelectMode) {
            val viewBinding =
                StripeChallengeZoneSingleSelectViewBinding.inflate(
                    LayoutInflater.from(context),
                    this,
                    true
                )
            infoLabel = viewBinding.label
            selectGroup = viewBinding.selectGroup
        } else {
            val viewBinding =
                StripeChallengeZoneMultiSelectViewBinding.inflate(
                    LayoutInflater.from(context),
                    this,
                    true
                )
            infoLabel = viewBinding.label
            selectGroup = viewBinding.selectGroup
        }
    }

    internal fun buildButton(
        option: ChallengeResponseData.ChallengeSelectOption,
        buttonCustomization: ButtonCustomization?,
        lastButton: Boolean
    ): CompoundButton {
        val button: CompoundButton = if (isSingleSelectMode) {
            MaterialRadioButton(context)
        } else {
            MaterialCheckBox(context)
        }
        if (buttonCustomization != null) {
            if (!buttonCustomization.backgroundColor.isNullOrBlank()) {
                CompoundButtonCompat.setButtonTintList(
                    button,
                    ColorStateList
                        .valueOf(Color.parseColor(buttonCustomization.backgroundColor))
                )
            }
            if (!buttonCustomization.textColor.isNullOrBlank()) {
                button.setTextColor(Color.parseColor(buttonCustomization.textColor))
            }
        }
        button.id = View.generateViewId()
        button.tag = option
        button.text = option.text
        button.setPadding(
            buttonLabelPadding,
            button.paddingTop,
            button.paddingRight,
            button.paddingBottom
        )
        button.minimumHeight = buttonMinHeight
        val layoutParams = RadioGroup.LayoutParams(
            RadioGroup.LayoutParams.MATCH_PARENT,
            RadioGroup.LayoutParams.WRAP_CONTENT
        )
        if (!lastButton) {
            layoutParams.bottomMargin = buttonBottomMargin
        }
        layoutParams.leftMargin = buttonOffsetMargin
        button.layoutParams = layoutParams
        return button
    }

    fun setChallengeSelectOptions(
        options: List<ChallengeResponseData.ChallengeSelectOption>?,
        buttonCustomization: ButtonCustomization?
    ) {
        val optionsCount = options?.size ?: return
        (0 until optionsCount).forEach {
            val option = options[it]
            val last = it == optionsCount - 1
            selectGroup.addView(buildButton(option, buttonCustomization, last))
        }
    }

    fun setTextEntryLabel(label: String?, labelCustomization: LabelCustomization?) {
        if (label.isNullOrBlank()) {
            infoLabel.visibility = View.GONE
        } else {
            infoLabel.setText(label, labelCustomization)
        }
    }

    fun selectOption(index: Int) {
        val button = selectGroup.getChildAt(index) as CompoundButton
        button.isChecked = true
    }

    override fun onSaveInstanceState(): Parcelable {
        return bundleOf(
            STATE_SUPER to super.onSaveInstanceState(),
            STATE_SELECTED_INDEXED to ArrayList(selectedIndexes)
        )
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable(STATE_SUPER))
            val selectedIndexes = state.getIntegerArrayList(STATE_SELECTED_INDEXED)
            selectedIndexes?.forEach { selectOption(it) }
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private companion object {
        private const val STATE_SELECTED_INDEXED = "state_selected_indexes"
        private const val STATE_SUPER = "state_super"
    }
}
