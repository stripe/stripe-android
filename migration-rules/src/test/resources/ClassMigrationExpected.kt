package com.stripe.android.test

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.elements.CardBrandAcceptance
import com.stripe.android.elements.Appearance.Embedded.RowStyle

// Test case 1: Simple class reference
fun testSimpleClass() {
    val cardBrand = CardBrandAcceptance.All
    println(cardBrand)
}

// Test case 2: Class in type annotation
fun testTypeAnnotation(): CardBrandAcceptance {
    return CardBrandAcceptance.All
}

// Test case 3: Class in parameter
fun testParameter(acceptance: CardBrandAcceptance) {
    // Do something
}

// Test case 4: Class in variable declaration
fun testVariableDeclaration() {
    val acceptance: CardBrandAcceptance = CardBrandAcceptance.All
}

// Test case 5: Nested class usage
fun testNestedClass() {
    val rowStyle = Appearance.Embedded.RowStyle.FlatWithRadio
    val another = Appearance.Embedded.RowStyle.FlatWithCheckmark
}