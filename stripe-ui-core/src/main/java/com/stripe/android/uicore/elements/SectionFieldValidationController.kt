package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import kotlinx.coroutines.flow.StateFlow

/**
 * Any element in a section must have a controller that provides
 * an error and have a type.  This is used for a single field in a section
 * or a section field that has other fields in it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SectionFieldValidationController : Controller {
    val validationMessage: StateFlow<FieldValidationMessage?>

    fun onValidationStateChanged(isValidating: Boolean) {}
}

/**
 * Encapsulates an error message including the string resource and the variable arguments
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
sealed interface FieldValidationMessage {
    @get:StringRes
    val message: Int
    val formatArgs: List<Any>?

    @Suppress("SpreadOperator")
    val resolvable: ResolvableString
        get() {
            val args = formatArgs?.toTypedArray() ?: arrayOf()
            return resolvableString(message, *args)
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Immutable
    data class Error(
        override val message: Int,
        override val formatArgs: List<Any>? = null
    ) : FieldValidationMessage

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Immutable
    data class Warning(
        override val message: Int,
        override val formatArgs: List<Any>? = null
    ) : FieldValidationMessage
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FieldValidationMessageComparator : Comparator<FieldValidationMessage?>

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DefaultFieldValidationMessageComparator : FieldValidationMessageComparator {
    override fun compare(
        a: FieldValidationMessage?,
        b: FieldValidationMessage?
    ): Int {
        return index(a) - index(b)
    }

    private fun index(message: FieldValidationMessage?): Int {
        return when (message) {
            is FieldValidationMessage.Error -> 0
            is FieldValidationMessage.Warning -> 1
            null -> 2
        }
    }
}
