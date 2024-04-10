package com.stripe.android.uicore.elements.compat

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

internal const val TextFieldId = "TextField"
internal const val PlaceholderId = "Hint"
internal const val LabelId = "Label"
internal const val LeadingId = "Leading"
internal const val TrailingId = "Trailing"
internal val ZeroConstraints = Constraints(0, 0, 0, 0)

internal const val AnimationDuration = 150

internal val TextFieldPadding = 16.dp
internal val HorizontalIconPadding = 12.dp

internal val IconDefaultSizeModifier = Modifier.defaultMinSize(48.dp, 48.dp)
