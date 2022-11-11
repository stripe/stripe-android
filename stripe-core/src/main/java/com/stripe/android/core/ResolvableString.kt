package com.stripe.android.core

import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface ResolvableString : Parcelable {

    @Parcelize
    data class Value internal constructor(
        val text: String,
    ) : ResolvableString

    @Parcelize
    data class Resource internal constructor(
        @StringRes val id: Int,
        val args: List<String>,
        val transform: (String) -> String = { it },
    ) : ResolvableString
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ResolvableString.resolve(context: Context): String {
    return resolve(context.resources)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ResolvableString.resolve(resources: Resources): String {
    return when (this) {
        is ResolvableString.Resource -> {
            val text = resources.getString(id, *args.toTypedArray())
            transform(text)
        }
        is ResolvableString.Value -> {
            text
        }
    }
}

//@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
//fun resolvableString(text: String): ResolvableString {
//    return ResolvableString.Value(text)
//}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.toResolvableString(): ResolvableString {
    return ResolvableString.Value(this)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun resolvableString(
    @StringRes id: Int,
    vararg args: String,
    transform: (String) -> String = { it }
): ResolvableString {
    return ResolvableString.Resource(id, args.toList(), transform)
}
