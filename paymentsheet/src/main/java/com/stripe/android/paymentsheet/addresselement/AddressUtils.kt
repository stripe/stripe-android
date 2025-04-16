package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.min

// https://gist.github.com/ademar111190/34d3de41308389a0d0d8

internal fun CharSequence.levenshtein(other: CharSequence): Int {
    if (this == other) { return 0 }
    if (this.isEmpty()) { return other.length }
    if (other.isEmpty()) { return this.length }

    val thisLength = this.length + 1
    val otherLength = other.length + 1

    var cost = Array(thisLength) { it }
    var newCost = Array(thisLength) { 0 }

    for (i in 1 until otherLength) {
        newCost[0] = i

        for (j in 1 until thisLength) {
            val match = if (this[j - 1] == other[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = min(min(costInsert, costDelete), costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[thisLength - 1]
}

internal fun AddressDetails.editDistance(otherAddress: AddressDetails?): Int {
    var editDistance = 0
    val comparedAddress = otherAddress?.address
    editDistance += (address?.city ?: "").levenshtein(comparedAddress?.city ?: "")
    editDistance += (address?.country ?: "").levenshtein(comparedAddress?.country ?: "")
    editDistance += (address?.line1 ?: "").levenshtein(comparedAddress?.line1 ?: "")
    editDistance += (address?.line2 ?: "").levenshtein(comparedAddress?.line2 ?: "")
    editDistance += (address?.postalCode ?: "").levenshtein(comparedAddress?.postalCode ?: "")
    editDistance += (address?.state ?: "").levenshtein(comparedAddress?.state ?: "")
    return editDistance
}

@Composable
internal fun ScrollableColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}
