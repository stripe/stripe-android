package com.stripe.android.link.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/**
 * A Composable that is shown in the ModalBottomSheetLayout.
 */
internal typealias BottomSheetContent = @Composable ColumnScope.() -> Unit
