package com.stripe.android.view

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcelable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.stripe.android.R
import kotlinx.parcelize.Parcelize

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

    internal var defaultColorStateList: ColorStateList
        @VisibleForTesting
        internal set

    private var externalColorStateList: ColorStateList? = null

    @ColorInt
    private var defaultErrorColor: Int = 0

    @ColorInt
    private var externalErrorColor: Int? = null

    private var textWatchers: MutableList<TextWatcher>? = null

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
                    super.setTextColor(externalColorStateList ?: defaultColorStateList)
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
        textWatchers = mutableListOf()
        maxLines = 1

        listenForTextChanges()
        listenForDeleteEmpty()
        defaultColorStateList = textColors
        determineDefaultErrorColor()

        // This will initialize a listener that calls the internal listeners then the external one
        onFocusChangeListener = null
    }

    internal val internalFocusChangeListeners = mutableListOf<OnFocusChangeListener>()
    private var externalFocusChangeListener: OnFocusChangeListener? = null

    protected open val accessibilityText: String? = null

    override fun setTextColor(colors: ColorStateList?) {
        super.setTextColor(colors)

        // This will only use textColors and not colors because textColor is never null
        externalColorStateList = textColors
    }

    override fun setTextColor(color: Int) = setTextColor(ColorStateList.valueOf(color))

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
            if (StripeColorUtils.isColorDark(defaultColorStateList.defaultColor)) {
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

    override fun onSaveInstanceState(): Parcelable {
        return StripeEditTextState(
            super.onSaveInstanceState(),
            errorMessage,
            shouldShowError
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as StripeEditTextState).let {
            super.onRestoreInstanceState(it.superState)
            errorMessage = it.errorMessage
            shouldShowError = it.shouldShowError
        }
    }

    private class SoftDeleteInputConnection constructor(
        target: InputConnection,
        mutable: Boolean,
        private val deleteEmptyListener: DeleteEmptyListener?
    ) : InputConnectionWrapper(target, mutable) {
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            // This method works on modern versions of Android with soft keyboard delete.
            if (getTextBeforeCursor(1, 0)?.isEmpty() == true) {
                deleteEmptyListener?.onDeleteEmpty()
            }
            return super.deleteSurroundingText(beforeLength, afterLength)
        }
    }

    final override fun setOnFocusChangeListener(listener: OnFocusChangeListener?) {
        super.setOnFocusChangeListener { view, hasFocus ->
            internalFocusChangeListeners.forEach {
                it.onFocusChange(view, hasFocus)
            }

            externalFocusChangeListener?.onFocusChange(view, hasFocus)
        }

        externalFocusChangeListener = listener
    }

    override fun getOnFocusChangeListener() = externalFocusChangeListener

    @VisibleForTesting
    internal fun getParentOnFocusChangeListener() = super.getOnFocusChangeListener()

    /**
     * Note: [addTextChangedListener] will potentially be called by a superclass constructor
     */
    override fun addTextChangedListener(watcher: TextWatcher?) {
        super.addTextChangedListener(watcher)

        watcher?.let {
            textWatchers?.add(it)
        }
    }

    override fun removeTextChangedListener(watcher: TextWatcher?) {
        super.removeTextChangedListener(watcher)

        watcher?.let {
            textWatchers?.remove(it)
        }
    }

    /**
     * Set text without notifying its corresponding text watchers.
     */
    internal fun setTextSilent(text: CharSequence?) {
        textWatchers?.forEach {
            super.removeTextChangedListener(it)
        }
        setText(text)
        textWatchers?.forEach {
            super.addTextChangedListener(it)
        }
    }

    @Parcelize
    internal data class StripeEditTextState(
        val superState: Parcelable?,
        val errorMessage: String?,
        val shouldShowError: Boolean
    ) : Parcelable
}
