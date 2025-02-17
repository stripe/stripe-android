package com.stripe.form

sealed interface ValidationResult {
    val isValid: Boolean

    data object Valid : ValidationResult {
        override val isValid: Boolean = true
    }

    data class Invalid(val message: ContentSpec? = null) : ValidationResult {
        override val isValid: Boolean = false
    }
}
