package com.stripe.android.paymentsheet.elements

import android.os.Parcelable

/**
 * This represents a field in a section.
 */
internal sealed class SectionFieldSpec(open val identifier: IdentifierSpec) : Parcelable
