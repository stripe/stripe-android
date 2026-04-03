@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.core.strings

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.core.strings.transformations.TransformOperation
import kotlinx.parcelize.RawValue

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
    vararg formatArgs: @RawValue Any?,
    transformations: List<TransformOperation> = listOf(),
): ResolvableString {
    return IdentifierResolvableString(id = id, transformations = transformations, args = formatArgs.toList())
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
fun resolvableString(value: String, vararg formatArgs: @RawValue Any?): ResolvableString {
    return StaticResolvableString(value, formatArgs.toList())
}

/**
 * Creates a [ResolvableString] from a given string value.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val String.resolvableString: ResolvableString
    get() = StaticResolvableString(this, emptyList())

/**
 * Creates a [ResolvableString] from a given string identifier.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val @receiver:StringRes Int.resolvableString: ResolvableString
    get() = IdentifierResolvableString(this, emptyList(), emptyList())

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ResolvableString?.orEmpty(): ResolvableString {
    return this ?: "".resolvableString
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
operator fun ResolvableString.plus(other: ResolvableString): ResolvableString {
    return ConcatenatedResolvableString(this, other)
}
