package com.stripe.form

interface Validator<T> {
    fun validateResult(value: T): ValidationResult
}

class NoOpValidator<T> : Validator<T> {
    override fun validateResult(value: T): ValidationResult = ValidationResult.Valid
}