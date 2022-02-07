package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo

/**
 * This represents a field in a section.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class SectionFieldSpec(open val identifier: IdentifierSpec) : Parcelable
