package com.stripe.android.view

internal interface Bank {
    val id: String
    val code: String
    val displayName: String
    val brandIconResId: Int?
}
