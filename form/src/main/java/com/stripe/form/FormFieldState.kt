package com.stripe.form

import android.os.Parcelable

interface FormFieldState<T> {
    val key: Parcelable
    val onValueChange: (ValueChange<T>) -> Unit
    val validator: (T) -> ValidationResult
}
