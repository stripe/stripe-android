package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.utils.DateUtils
import com.stripe.android.R
import com.stripe.android.core.utils.DateUtils
import com.stripe.android.model.ExpirationDate
import kotlin.math.min
import kotlin.properties.Delegates
import androidx.appcompat.R as AppCompatR
import com.stripe.android.uicore.R as UiCoreR

/**
 * An [EditText] that handles putting numbers around a central divider character.
 */
class ExpiryDateEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = AppCompatR.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

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
     * The return value is a [ExpirationDate.Validated], where the first entry is the two-digit
     * month (from 01-12) and the second entry is the four-digit year (2017, not 17).
     */
    val validatedDate: ExpirationDate.Validated?
        get() {
            return when (isDateValid) {
                true -> ExpirationDate.Unvalidated.create(fieldText).validate()
                false -> null
            }
        }

    override val accessibilityText: String
        get() {
            return resources.getString(R.string.stripe_acc_label_expiry_date_node, text)
        }

    internal var includeSeparatorGaps: Boolean by Delegates.observable(
        INCLUDE_SEPARATOR_GAPS_DEFAULT
    ) { _, _, newValue ->
        updateSeparatorUi(newValue)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun setIncludeSeparatorGaps(include: Boolean) {
        includeSeparatorGaps = include
    }

    private val dateDigitsLength = context.resources.getInteger(R.integer.stripe_date_digits_length)

    private var separator = if (INCLUDE_SEPARATOR_GAPS_DEFAULT) {
        SEPARATOR_WITH_GAPS
    } else {
        SEPARATOR_WITHOUT_GAPS
    }

    internal fun setText(expiryMonth: Int?, expiryYear: Int?) {
        if (expiryMonth != null && expiryYear != null) {
            setText(
                listOf(
                    expiryMonth.toString().padStart(2, '0'),
                    expiryYear.toString().takeLast(2).padStart(2, '0')
                ).joinToString(separator = separator)
            )
        }
    }

    private fun updateSeparatorUi(
        includeSeparatorGaps: Boolean = INCLUDE_SEPARATOR_GAPS_DEFAULT
    ) {
        separator = if (includeSeparatorGaps) {
            SEPARATOR_WITH_GAPS
        } else {
            SEPARATOR_WITHOUT_GAPS
        }

        filters = listOf(
            InputFilter.LengthFilter(dateDigitsLength + separator.length)
        ).toTypedArray()
    }

    private fun listenForTextChanges() {
        addTextChangedListener(
            object : StripeTextWatcher() {
                private var latestChangeStart: Int = 0
                private var latestInsertionSize: Int = 0
                private var expirationDate = ExpirationDate.Unvalidated.EMPTY

                private var newCursorPosition: Int? = null
                private var formattedDate: String? = null

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    latestChangeStart = start
                    latestInsertionSize = after
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    var inErrorState = false

                    val inputText = s?.toString().orEmpty()
                    var rawNumericInput = inputText.filter { it.isDigit() }

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

                    val expirationDate = ExpirationDate.Unvalidated.create(
                        rawNumericInput
                    ).also {
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
                        formattedDateBuilder.append(separator)
                    }

                    formattedDateBuilder.append(expirationDate.year)

                    val formattedDate = formattedDateBuilder.toString()
                    this.newCursorPosition = updateSelectionIndex(
                        formattedDate.length,
                        latestChangeStart,
                        latestInsertionSize,
                        dateDigitsLength + separator.length
                    )
                    this.formattedDate = formattedDate
                }

                override fun afterTextChanged(s: Editable?) {
                    if (formattedDate != null) {
                        setTextSilent(formattedDate)
                        newCursorPosition?.let {
                            setSelection(it.coerceIn(0, fieldText.length))
                        }
                    }

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

                    setErrorMessage(
                        resources.getString(
                            if (expirationDate.isPartialEntry) {
                                UiCoreR.string.stripe_incomplete_expiry_date
                            } else if (!expirationDate.isMonthValid) {
                                UiCoreR.string.stripe_invalid_expiry_month
                            } else {
                                UiCoreR.string.stripe_invalid_expiry_year
                            }
                        )
                    )

                    this@ExpiryDateEditText.shouldShowError =
                        shouldShowError && (expirationDate.isPartialEntry || expirationDate.isComplete)

                    formattedDate = null
                    newCursorPosition = null
                }
            }
        )
    }

    init {
        setNumberOnlyInputType()

        updateSeparatorUi()

        listenForTextChanges()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
        }

        internalFocusChangeListeners.add { _, hasFocus ->
            if (!hasFocus && !text.isNullOrEmpty() && !isDateValid) {
                shouldShowError = true
            }
        }

        layoutDirection = LAYOUT_DIRECTION_LTR
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
            separator.length
        } else {
            0
        }

        // `shouldRemoveSeparator` will be true when deleting the character immediately after the
        // separator. For example, if the input text is currently "01/2" and a character is
        // deleted, both the '2' and the separator should be deleted.
        val isDelete = editActionAddition == 0
        val shouldRemoveSeparator = isDelete && editActionStart == (2 + separator.length)

        val newPosition = (editActionStart + editActionAddition + gapsJumped).let { newPosition ->
            newPosition - if (shouldRemoveSeparator && newPosition > 0) {
                separator.length
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
        private const val INVALID_INPUT = -1

        private const val SEPARATOR_WITHOUT_GAPS = "/"
        private const val SEPARATOR_WITH_GAPS = " / "

        private const val INCLUDE_SEPARATOR_GAPS_DEFAULT = false
    }
}
