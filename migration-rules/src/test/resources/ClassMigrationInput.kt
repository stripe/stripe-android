package com.stripe.android.test

import com.stripe.android.paymentsheet.PaymentSheet

// Test case 1: Simple class reference
fun testSimpleClass() {
    val cardBrand = PaymentSheet.CardBrandAcceptance.All
    println(cardBrand)
}

// Test case 2: Class in type annotation
fun testTypeAnnotation(): PaymentSheet.CardBrandAcceptance {
    return PaymentSheet.CardBrandAcceptance.All
}

// Test case 3: Class in parameter
fun testParameter(acceptance: PaymentSheet.CardBrandAcceptance) {
    // Do something
}

// Test case 4: Class in variable declaration
fun testVariableDeclaration() {
    val acceptance: PaymentSheet.CardBrandAcceptance = PaymentSheet.CardBrandAcceptance.All
}

// Test case 5: Nested class usage
fun testNestedClass() {
    val rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio
    val another = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark
}