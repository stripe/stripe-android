package com.stripe.android.ui.core.elements

/**
 * Configuration interface specifically for card number text fields.
 * This extends [CardDetailsTextFieldConfig] to provide domain-specific constraints
 * for card number input fields while allowing for testability with fake implementations.
 */
internal interface CardNumberTextFieldConfig : CardDetailsTextFieldConfig
