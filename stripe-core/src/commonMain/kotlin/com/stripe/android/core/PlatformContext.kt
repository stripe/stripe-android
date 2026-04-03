package com.stripe.android.core

import androidx.annotation.RestrictTo

/**
 * Opaque platform context handle for shared code.
 *
 * Android actualizes this as `android.content.Context`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
expect abstract class PlatformContext
