package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
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
    val formatArgs: Array<out Any>?

    @Immutable
    data class Error(
        override val message: Int,
        override val formatArgs: Array<out Any>? = null
    ) : FieldValidationMessage {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Error

            if (message != other.message) return false
            if (!formatArgs.contentEquals(other.formatArgs)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = message
            result = 31 * result + (formatArgs?.contentHashCode() ?: 0)
            return result
        }
    }

    @Immutable
    data class Warning(
        override val message: Int,
        override val formatArgs: Array<out Any>? = null
    ) : FieldValidationMessage {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Warning

            if (message != other.message) return false
            if (!formatArgs.contentEquals(other.formatArgs)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = message
            result = 31 * result + (formatArgs?.contentHashCode() ?: 0)
            return result
        }
    }
}
