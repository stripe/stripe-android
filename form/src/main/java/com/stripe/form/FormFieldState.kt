package com.stripe.form

interface FormFieldState<T> {
    val key: Key<T>
    val onValueChange: (ValueChange<T>) -> Unit
    val validator: (T) -> ValidationResult
}
