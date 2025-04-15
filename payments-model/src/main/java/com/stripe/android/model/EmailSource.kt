package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class EmailSource(val backendValue: String) {
    // describes a user-entered email
    USER_ACTION("user_action"),

    // describes an editable, merchant-passed email
    DEFAULT_VALUE("default_value"),

    // describes a read-only, merchant-passed email
    CUSTOMER_OBJECT("customer_object"),

    // describes a read-only, merchant-passed email from prop
    CUSTOMER_EMAIL("customer_email"),
}
