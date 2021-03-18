package com.stripe.android.view

interface Bank {
    var id: String
    var code: String
    var displayName: String
    var brandIconResId: Int?
}