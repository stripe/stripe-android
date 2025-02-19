package com.stripe.form

interface Validator<T> {
    fun validateResult(value: T): ValidationResult
}
