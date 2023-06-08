package com.stripe.android.link.ui.inline

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.link.LinkConfigurationInteractor
import com.stripe.android.link.R
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColors

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun LinkInlineSignedIn(
    linkConfigurationInteractor: LinkConfigurationInteractor,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    linkConfigurationInteractor.component?.let { component ->
        val viewModel: InlineSignupViewModel = viewModel(
            factory = InlineSignupViewModel.Factory(component.injector)
        )

        val accountEmail = viewModel.accountEmail.collectAsState(initial = "")

        StripeTheme {
            Box(
                modifier = modifier
                    .border(
                        border = MaterialTheme.getBorderStroke(isSelected = false),
                        shape = MaterialTheme.linkShapes.small
                    )
                    .background(
                        color = MaterialTheme.stripeColors.component,
                        shape = MaterialTheme.linkShapes.small
                    )
                    .semantics {
                        testTag = "SignedInBox"
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.stripe_this_card_will_be_saved),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Divider(
                        color = MaterialTheme.stripeColors.componentBorder.copy(alpha = 0.1f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = accountEmail.value ?: "",
                            color = MaterialTheme.stripeColors.subtitle
                        )
                        ClickableText(
                            text = AnnotatedString(text = stringResource(id = R.string.stripe_logout)),
                            style = TextStyle.Default.copy(color = MaterialTheme.colors.primary),
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            }
                        )
                    }
                }
            }
        }
    }
}
