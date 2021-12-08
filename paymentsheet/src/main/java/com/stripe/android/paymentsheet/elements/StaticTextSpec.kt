package com.stripe.android.paymentsheet.elements

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * This is for elements that do not receive user input
 */
@Parcelize
internal data class StaticTextSpec(
    override val identifier: IdentifierSpec,
    @StringRes val stringResId: Int,
    @ColorRes val color: Int? = null,
    val fontSizeSp: Int = 10,
    val letterSpacingSp: Double = .7
) : FormItemSpec(), RequiredItemSpec, Parcelable
