package com.stripe.form.fields.card

import com.stripe.android.model.CardBrand
import com.stripe.form.ValidationResult
import com.stripe.form.Validator
import com.stripe.form.text.TextSpec

class CvcValidator(
    private val cardBrand: CardBrand
): Validator<String> {
    override fun validateResult(value: String): ValidationResult {
        if (cardBrand.isValidCvc(value)) {
            return ValidationResult.Valid
        }
        return ValidationResult.Invalid(
            message = TextSpec("Invalid cvc")
        )
    }
}