package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.stripe.android.uicore.stripeColors

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val MANDATE_TEST_TAG = "mandate_test_tag"

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun MandateTextUI(
    element: MandateTextElement,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(element.stringResId, *element.args.toTypedArray()),
        style = MaterialTheme.typography.caption.copy(
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Normal,
        ),
        color = MaterialTheme.stripeColors.placeholderText,
        modifier = modifier
            .semantics(mergeDescendants = true) {} // makes it a separate accessibile item
            .testTag(MANDATE_TEST_TAG)
    )
}
