package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize

internal object FakeLayoutCoordinatesFixtures {

    val FULLY_VISIBLE_COORDINATES = FakeLayoutCoordinates.create(
        size = IntSize(100, 50),
        bounds = Rect(0f, 0f, 100f, 50f)
    )

    val FULLY_HIDDEN_COORDINATES =  FakeLayoutCoordinates.create(
        size = IntSize(100, 50),
        bounds = Rect(0f, 0f, 0f, 0f)
    )

    val PARTIALLY_HIDDEN_COORDINATES =  FakeLayoutCoordinates.create(
        size = IntSize(100, 50),
        position = Offset(0f, 75f),
        bounds = Rect(0f, 75f, 100f, 100f),
    )
}