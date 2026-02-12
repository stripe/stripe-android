package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.LocalSectionStyle
import com.stripe.android.uicore.SectionStyle
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColorScheme

/**
 * This is a simple section that holds content in a card view.  It has a label, content specified
 * by the caller, and an error string.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Section(
    title: ResolvableString?,
    validationMessage: FieldValidationMessage?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(modifier) {
        SectionTitle(title)
        SectionCard(
            isSelected = isSelected,
            content = content,
        )

        if (validationMessage != null) {
            SectionValidationMessage(validationMessage)
        }
    }
}

/**
 * This is the layout for the section title
 */
@Composable
internal fun SectionTitle(titleText: ResolvableString?) {
    titleText?.let {
        H6Text(
            text = it.resolve(),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .semantics(mergeDescendants = true) { // Need to prevent form as focusable accessibility
                    heading()
                }
        )
    }
}

/**
 * This is the layout for the section card.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SectionCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    isSelected: Boolean = false,
    backgroundColor: Color = MaterialTheme.stripeColorScheme.component,
    border: BorderStroke = MaterialTheme.getBorderStroke(isSelected),
    content: @Composable () -> Unit
) {
    val sectionStyle = LocalSectionStyle.current

    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 1.5.dp else 0.dp
        ),
        border = when (sectionStyle) {
            SectionStyle.Borderless -> null
            SectionStyle.Bordered -> border
        },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        modifier = modifier,
        shape = shape
    ) {
        Column(
            modifier = when (sectionStyle) {
                SectionStyle.Borderless -> Modifier
                SectionStyle.Bordered ->
                    Modifier
                        .padding(border.width)
                        .clip(shape)
            }
        ) {
            content()
        }
    }
}

/**
 * This is how error string for the section are displayed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun SectionValidationMessage(
    validationMessage: FieldValidationMessage,
    modifier: Modifier = Modifier
) {
    Text(
        text = validationMessage.resolvable.resolve(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .padding(top = 2.dp)
            .semantics(mergeDescendants = true) { }
    )
}
