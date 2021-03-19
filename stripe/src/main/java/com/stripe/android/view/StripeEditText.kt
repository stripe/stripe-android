package com.stripe.android.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.stripe.android.R

/**
 * Extension of [TextInputEditText] that listens for users pressing the delete key when
 * there is no text present. Google has actually made this
 * [somewhat difficult](https://code.google.com/p/android/issues/detail?id=42904),
 * but we listen here for hardware key presses, older Android soft keyboard delete presses,
 * and modern Google Keyboard delete key presses.
 */
open class StripeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {
    protected var isLastKeyDelete: Boolean = false

    private var afterTextChangedListener: AfterTextChangedListener? = null
    private var deleteEmptyListener: DeleteEmptyListener? = null

    var defaultColorState: ColorStateList
        private set
    private var externalColorState: ColorStateList? = null
    @ColorInt
    private var defaultErrorColor: Int = 0
    @ColorInt
    private var externalErrorColor: Int? = null

    /**
     * Gets whether or not the text should be displayed in error mode.
     *
     * Sets whether or not the text should be put into "error mode," which displays
     * the text in an error color determined by the original text color.
     */
    var shouldShowError: Boolean = false
        set(shouldShowError) {
            errorMessage?.let {
                errorMessageListener?.displayErrorMessage(it.takeIf { shouldShowError })
            }

            if (field != shouldShowError) {
                // only update the view's UI if the property's value is changing
                if (shouldShowError) {
                    super.setTextColor(externalErrorColor ?: defaultErrorColor)
                } else {
                    super.setTextColor(externalColorState ?: defaultColorState)
                }
                refreshDrawableState()
            }

            field = shouldShowError
        }

    internal var errorMessage: String? = null

    internal val fieldText: String
        get() {
            return text?.toString().orEmpty()
        }

    private var errorMessageListener: ErrorMessageListener? = null

    /**
     * The color used for error text.
     */
    // It's possible that we need to verify this value again
    // in case the user programmatically changes the text color.
    val defaultErrorColorInt: Int
        @ColorInt
        get() {
            determineDefaultErrorColor()
            return defaultErrorColor
        }

    init {
        maxLines = 1
        listenForTextChanges()
        listenForDeleteEmpty()
        defaultColorState = textColors
        determineDefaultErrorColor()
    }

    protected open val accessibilityText: String? = null

    override fun setTextColor(colors: ColorStateList?) {
        super.setTextColor(colors)

        // This will only use textColors and not colors because textColor is never null
        externalColorState = textColors
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)

        // This will only use textColors and not colors because textColor is never null
        externalColorState = ColorStateList.valueOf(color)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(outAttrs)
        return inputConnection?.let {
            SoftDeleteInputConnection(it, true, deleteEmptyListener)
        }
    }

    /**
     * Sets a listener that can react to changes in text, but only by reflecting the new
     * text in the field.
     *
     * @param afterTextChangedListener the [AfterTextChangedListener] to attach to this view
     */
    fun setAfterTextChangedListener(afterTextChangedListener: AfterTextChangedListener?) {
        this.afterTextChangedListener = afterTextChangedListener
    }

    /**
     * Sets a listener that can react to the user attempting to delete the empty string.
     *
     * @param deleteEmptyListener the [DeleteEmptyListener] to attach to this view
     */
    fun setDeleteEmptyListener(deleteEmptyListener: DeleteEmptyListener?) {
        this.deleteEmptyListener = deleteEmptyListener
    }

    fun setErrorMessageListener(errorMessageListener: ErrorMessageListener?) {
        this.errorMessageListener = errorMessageListener
    }

    fun setErrorMessage(errorMessage: String?) {
        this.errorMessage = errorMessage
    }

    /**
     * Sets the error text color on this [StripeEditText].
     *
     * @param errorColor a [ColorInt]
     */
    fun setErrorColor(@ColorInt errorColor: Int) {
        this.externalErrorColor = errorColor
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.isContentInvalid = shouldShowError
        accessibilityText?.let { info.text = it }
        info.error = errorMessage.takeIf { shouldShowError }
    }

    private fun determineDefaultErrorColor() {
        defaultErrorColor = ContextCompat.getColor(
            context,
            if (StripeColorUtils.isColorDark(defaultColorState.defaultColor)) {
                // Note: if the _text_ color is dark, then this is a
                // light theme, and vice-versa.
                R.color.stripe_error_text_light_theme
            } else {
                R.color.stripe_error_text_dark_theme
            }
        )
    }

    private fun listenForTextChanges() {
        doAfterTextChanged { editable ->
            afterTextChangedListener?.onTextChanged(editable?.toString().orEmpty())
        }
    }

    private fun listenForDeleteEmpty() {
        // This method works for hard keyboards and older phones.
        setOnKeyListener { _, keyCode, event ->
            isLastKeyDelete = isDeleteKey(keyCode, event)
            if (isLastKeyDelete && length() == 0) {
                deleteEmptyListener?.onDeleteEmpty()
            }
            false
        }
    }

    private fun isDeleteKey(keyCode: Int, event: KeyEvent): Boolean {
        return keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN
    }

    fun interface DeleteEmptyListener {
        fun onDeleteEmpty()
    }

    fun interface AfterTextChangedListener {
        fun onTextChanged(text: String)
    }

    fun interface ErrorMessageListener {
        fun displayErrorMessage(message: String?)
    }

    private class SoftDeleteInputConnection constructor(
        target: InputConnection,
        mutable: Boolean,
        private val deleteEmptyListener: DeleteEmptyListener?
    ) : InputConnectionWrapper(target, mutable) {
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            // This method works on modern versions of Android with soft keyboard delete.
            if (getTextBeforeCursor(1, 0).isEmpty()) {
                deleteEmptyListener?.onDeleteEmpty()
            }
            return super.deleteSurroundingText(beforeLength, afterLength)
        }
    }
}
