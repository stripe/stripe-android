@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.core.strings

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.core.strings.transformations.TransformOperation

internal fun resolveArgs(context: Context, args: List<Any?>): Array<Any?> {
    return args.map { arg ->
        when (arg) {
            is ResolvableString -> arg.resolve(context)
            else -> arg
        }
    }.toTypedArray()
}

/**
 * Creates a [ResolvableString] from a given identifier.
 *
 * @param id a string resource identifier
 * @param formatArgs a variable amount of arguments to format the string with.
 *
 * @return a [ResolvableString] instance when resolved returns the string that matches the identifier.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun resolvableString(
    @StringRes id: Int,
    vararg formatArgs: Any?,
    transformations: List<TransformOperation> = listOf()
): ResolvableString {
    return IdentifierResolvableString(id, formatArgs.toList(), transformations)
}

/**
 * Creates a [ResolvableString] from a given string value.
 *
 * @param value a string value
 * @param formatArgs a variable amount of arguments to format the string with.
 *
 * @return a [ResolvableString] instance when resolved returns the string value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun resolvableString(value: String, vararg formatArgs: Any?): ResolvableString {
    return StaticResolvableString(value, formatArgs.toList())
}
