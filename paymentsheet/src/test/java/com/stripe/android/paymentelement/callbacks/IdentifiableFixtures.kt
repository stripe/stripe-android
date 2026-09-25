package com.stripe.android.paymentelement.callbacks

import com.stripe.android.paymentsheet.PaymentSheet.Identifiable

internal fun createTestIdentifier(name: String): Identifiable = StringIdentifiable(name)

@JvmInline
private value class StringIdentifiable(private val key: String) : Identifiable
