package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.text.method.TextKeyListener
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import java.util.regex.Pattern
import kotlin.properties.Delegates

class PostalCodeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    internal var config: Config by Delegates.observable(
        Config.Global
    ) { _, oldValue, newValue ->
        when (newValue) {
            Config.Global -> configureForGlobal()
            Config.US -> configureForUs()
        }
    }

    internal val postalCode: String?
        get() {
            return if (config == Config.US) {
                fieldText.takeIf {
                    ZIP_CODE_PATTERN.matcher(fieldText).matches()
                }
            } else {
                fieldText
            }
        }

    init {
        setErrorMessage(resources.getString(R.string.invalid_zip))
        maxLines = 1

        addTextChangedListener(object : StripeTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                shouldShowError = false
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_POSTAL_CODE)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        configureForGlobal()
    }

    /**
     * Configure the field for United States users
     */
    private fun configureForUs() {
        updateHint(R.string.address_label_zip_code)
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH_US))
        keyListener = DigitsKeyListener.getInstance(false, true)
        inputType = InputType.TYPE_CLASS_NUMBER
    }

    /**
     * Configure the field for global users
     */
    private fun configureForGlobal() {
        updateHint(R.string.address_label_postal_code)
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH_GLOBAL))
        keyListener = TextKeyListener.getInstance()
        inputType = InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
    }

    /**
     * If a `TextInputLayout` is an ancestor of this view, set the hint on it. Otherwise, set
     * the hint on this view.
     */
    private fun updateHint(@StringRes hintRes: Int) {
        getTextInputLayout()?.let {
            if (it.isHintEnabled) {
                it.hint = resources.getString(hintRes)
            } else {
                setHint(hintRes)
            }
        }
    }

    /**
     * Copied from `TextInputEditText`
     */
    private fun getTextInputLayout(): TextInputLayout? {
        var parent = parent
        while (parent is View) {
            if (parent is TextInputLayout) {
                return parent
            }
            parent = parent.getParent()
        }
        return null
    }

    internal enum class Config {
        Global,
        US
    }

    private companion object {
        private const val MAX_LENGTH_US = 5
        private const val MAX_LENGTH_GLOBAL = 13

        private val ZIP_CODE_PATTERN = Pattern.compile("^[0-9]{5}$")
    }
}
