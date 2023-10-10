package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.VerificationPageIconType
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetLineContent
import com.stripe.android.identity.networking.models.getContentDescriptionId
import com.stripe.android.identity.networking.models.getResourceId
import com.stripe.android.identity.viewmodel.BottomSheetViewModel
import com.stripe.android.uicore.text.Html

@Composable
@ExperimentalMaterialApi
internal fun BottomSheet() {
    val viewModel = viewModel<BottomSheetViewModel>()
    val state by viewModel.bottomSheetState.collectAsState()
    state.content?.let { bottomSheetContent ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                    horizontal = dimensionResource(
                        id = R.dimen.stripe_page_horizontal_margin
                    )
                )
                .testTag(BOTTOM_SHEET_CONTENT_TAG)
        ) {
            bottomSheetContent.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.h4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BOTTOM_SHEET_TITLE_TAG)
                )
            }
            Column(
                Modifier
                    .heightIn(max = 400.dp)
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    for (line in bottomSheetContent.lines) {
                        BottomSheetLine(line)
                    }
                }
            }
            Button(
                onClick = {
                    viewModel.dismissBottomSheet()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(BOTTOM_SHEET_BUTTON_TAG)
                    .padding(vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
            ) {
                Text(stringResource(id = R.string.stripe_close_button_text).uppercase())
            }
        }
    }
}

@Composable
private fun BottomSheetLine(line: VerificationPageStaticContentBottomSheetLineContent) {
    Row(
        modifier = Modifier
            .testTag(BOTTOM_SHEET_LINE_TAG)
            .padding(top = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
    ) {
        line.icon?.let {
            Image(
                painter = painterResource(id = it.getResourceId()),
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 4.dp),
                contentDescription = stringResource(id = it.getContentDescriptionId())
            )
        }

        Column(
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Text(
                text = line.title,
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(
                    bottom = 4.dp
                )
            )
            Html(
                html = line.content,
                color = MaterialTheme.colors.onSurface.copy(
                    alpha = 0.6f
                ),
                urlSpanStyle = SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colors.secondary
                )
            )
        }
    }
}

@Preview
@Composable
@ExperimentalMaterialApi
internal fun ButtonSheetPreview() {
    IdentityPreview {
        val mockViewModel = viewModel<BottomSheetViewModel>()
        mockViewModel.showBottomSheet(
            VerificationPageStaticContentBottomSheetContent(
                bottomSheetId = "testId",
                title = "BottomSheet title",
                lines = listOf(
                    VerificationPageStaticContentBottomSheetLineContent(
                        icon = VerificationPageIconType.CAMERA,
                        title = "camera line",
                        content = "camera line content"
                    ),
                    VerificationPageStaticContentBottomSheetLineContent(
                        title = "no icon line",
                        content = "no line content with a <a href='https://stripe.com'>link</a>"
                    ),
                    VerificationPageStaticContentBottomSheetLineContent(
                        icon = VerificationPageIconType.CLOUD,
                        title = "cloud line",
                        content = "cloud line content with another " +
                            "<a href='https://stripe.com'>link</a>, with multiline content"
                    )
                )
            )
        )
        BottomSheet()
    }
}

internal const val BOTTOM_SHEET_CONTENT_TAG = "BottomSheetContent"
internal const val BOTTOM_SHEET_TITLE_TAG = "BottomSheetTitle"
internal const val BOTTOM_SHEET_LINE_TAG = "BottomSheetLine"
internal const val BOTTOM_SHEET_BUTTON_TAG = "BottomSheetButton"
