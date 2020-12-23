package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.annotation.VisibleForTesting
import com.stripe.android.R
import com.stripe.android.model.ExpirationDate
import kotlin.math.min

/**
 * An [EditText] that handles putting numbers around a central divider character.
 */
class ExpiryDateEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    init {
        inputType = InputType.TYPE_CLASS_NUMBER
        filters = listOf(
            InputFilter.LengthFilter(
                context.resources.getInteger(R.integer.stripe_date_length)
            )
        ).toTypedArray()

        setErrorMessage(resources.getString(R.string.invalid_expiry_year))
        listenForTextChanges()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
        }
    }

    // invoked when a valid date has been entered
    @JvmSynthetic
    internal var completionCallback: () -> Unit = {}

    /**
     * Is `true` if the text entered represents a valid expiry date that has not
     * yet passed, and `false` if not.
     */
    var isDateValid: Boolean = false
        private set

    /**
     * Gets the expiry date displayed on this control if it is valid, or `null` if it is not.
     * The return value is given as a [Pair], where the first entry is the two-digit month
     * (from 01-12) and the second entry is the four-digit year (2017, not 17).
     */
    @Deprecated("Use validatedDate")
    val validDateFields: Pair<Int, Int>?
        get() = validatedDate?.let {
            Pair(it.month, it.year)
        }

    val validatedDate: ExpirationDate.Validated?
        get() {
            return when (isDateValid) {
                true -> ExpirationDate.Unvalidated.create(fieldText).validate()
                false -> null
            }
        }

    override val accessibilityText: String?
        get() {
            return resources.getString(R.string.acc_label_expiry_date_node, text)
        }

    private fun listenForTextChanges() {
        addTextChangedListener(
            object : StripeTextWatcher() {
                private var ignoreChanges = false
                private var latestChangeStart: Int = 0
                private var latestInsertionSize: Int = 0
                private var expirationDate = ExpirationDate.Unvalidated(
                    month = "",
                    year = ""
                )

                private var newCursorPosition: Int? = null
                private var formattedDate: String? = null

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    if (ignoreChanges) {
                        return
                    }
                    latestChangeStart = start
                    latestInsertionSize = after
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (ignoreChanges) {
                        return
                    }

                    var inErrorState = false

                    val inputText = s?.toString().orEmpty()
                    var rawNumericInput = inputText.replace(SEPARATOR.toRegex(), "")

                    if (rawNumericInput.length == 1 && latestChangeStart == 0 &&
                        latestInsertionSize == 1
                    ) {
                        val first = rawNumericInput[0]
                        if (!(first == '0' || first == '1')) {
                            // If the first digit typed isn't 0 or 1, then it can't be a valid
                            // two-digit month. Hence, we assume the user is inputting a one-digit
                            // month. We bump it to the preferred input, so "4" becomes "04", which
                            // later in this method goes to "04/".
                            rawNumericInput = "0$rawNumericInput"
                            latestInsertionSize++
                        }
                    } else if (rawNumericInput.length == 2 &&
                        latestChangeStart == 2 &&
                        latestInsertionSize == 0
                    ) {
                        // This allows us to delete past the separator, so that if a user presses
                        // delete when the current string is "12/", the resulting string is "1," since
                        // we pretend that the "/" isn't really there. The case that we also want,
                        // where "12/3" + DEL => "12" is handled elsewhere.
                        rawNumericInput = rawNumericInput.substring(0, 1)
                    }

                    val expirationDate =
                        ExpirationDate.Unvalidated.create(rawNumericInput).also {
                            this.expirationDate = it
                        }

                    if (!expirationDate.isMonthValid) {
                        inErrorState = true
                    }

                    val formattedDateBuilder = StringBuilder()
                        .append(expirationDate.month)

                    if (expirationDate.month.length == 2 && latestInsertionSize > 0 &&
                        !inErrorState || rawNumericInput.length > 2
                    ) {
                        formattedDateBuilder.append(SEPARATOR)
                    }

                    formattedDateBuilder.append(expirationDate.year)

                    val formattedDate = formattedDateBuilder.toString()
                    this.newCursorPosition = updateSelectionIndex(
                        formattedDate.length,
                        latestChangeStart,
                        latestInsertionSize,
                        MAX_INPUT_LENGTH
                    )
                    this.formattedDate = formattedDate
                }

                override fun afterTextChanged(s: Editable?) {
                    if (ignoreChanges) {
                        return
                    }

                    ignoreChanges = true
                    if (!isLastKeyDelete && formattedDate != null) {
                        setText(formattedDate)
                        newCursorPosition?.let {
                            setSelection(it.coerceIn(0, fieldText.length))
                        }
                    }

                    ignoreChanges = false

                    // Note: we want to show an error state if the month is invalid or the
                    // final, complete date is in the past. We don't want to show an error state for
                    // incomplete entries.

                    // This covers the case where the user has entered a month of 15, for instance.
                    val month = expirationDate.month
                    val year = expirationDate.year
                    var shouldShowError = month.length == 2 && !expirationDate.isMonthValid

                    // Note that we have to check the parts array because afterTextChanged has odd
                    // behavior when it comes to pasting, where a paste of "1212" triggers this
                    // function for the strings "12/12" (what it actually becomes) and "1212",
                    // so we might not be properly catching an error state.
                    if (month.length == 2 && year.length == 2) {
                        val wasComplete = isDateValid
                        isDateValid = isDateValid(month, year)
                        // Here, we have a complete date, so if we've made an invalid one, we want
                        // to show an error.
                        shouldShowError = !isDateValid
                        if (!wasComplete && isDateValid) {
                            completionCallback()
                        }
                    } else {
                        isDateValid = false
                    }

                    this@ExpiryDateEditText.shouldShowError = shouldShowError

                    formattedDate = null
                    newCursorPosition = null
                }
            }
        )
    }

    /**
     * Updates the selection index based on the current (pre-edit) index, and
     * the size change of the number being input.
     *
     * @param newLength the post-edit length of the string
     * @param editActionStart the position in the string at which the edit action starts
     * @param editActionAddition the number of new characters going into the string (zero for
     * delete)
     *
     * @return an index within the string at which to put the cursor
     */
    @VisibleForTesting
    internal fun updateSelectionIndex(
        newLength: Int,
        editActionStart: Int,
        editActionAddition: Int,
        maxInputLength: Int
    ): Int {
        val gapsJumped = if (editActionStart <= 2 && editActionStart + editActionAddition >= 2) {
            SEPARATOR.length
        } else {
            0
        }

        // `shouldRemoveSeparator` will be true when deleting the character immediately after the
        // separator. For example, if the input text is currently "01/2" and a character is
        // deleted, both the '2' and the separator should be deleted.
        val isDelete = editActionAddition == 0
        val shouldRemoveSeparator = isDelete && editActionStart == (2 + SEPARATOR.length)

        val newPosition = (editActionStart + editActionAddition + gapsJumped).let { newPosition ->
            newPosition - if (shouldRemoveSeparator && newPosition > 0) {
                SEPARATOR.length
            } else {
                0
            }
        }

        val untruncatedPosition = min(newPosition, newLength)
        return min(maxInputLength, untruncatedPosition)
    }

    private fun isDateValid(month: String, year: String): Boolean {
        val inputMonth = if (month.length != 2) {
            INVALID_INPUT
        } else {
            runCatching {
                month.toInt()
            }.getOrDefault(INVALID_INPUT)
        }

        val inputYear = if (year.length != 2) {
            INVALID_INPUT
        } else {
            runCatching {
                DateUtils.convertTwoDigitYearToFour(year.toInt())
            }.getOrDefault(INVALID_INPUT)
        }

        return DateUtils.isExpiryDataValid(inputMonth, inputYear)
    }

    private companion object {
        private const val SEPARATOR = "/"
        private const val INVALID_INPUT = -1
        private const val MAX_INPUT_LENGTH = 4 + SEPARATOR.length
    }
}
