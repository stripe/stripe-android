package com.stripe.android.identity.ui

import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.composethemeadapter.MdcTheme
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.utils.setHtmlString

@Composable
internal fun ConfirmationScreen(
    verificationPageState: Resource<VerificationPage>?,
    onConfirmed: () -> Unit
) {
    MdcTheme {
        require(verificationPageState != null && verificationPageState.status == Status.SUCCESS) {
            "verificationPageState.status is not SUCCESS"
        }
        val successPage = remember { requireNotNull(verificationPageState.data).success }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = dimensionResource(id = R.dimen.page_vertical_margin),
                    horizontal = dimensionResource(id = R.dimen.page_horizontal_margin)
                )
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.Center

                ) {
                    Image(
                        painter = painterResource(id = R.drawable.clock_icon),
                        modifier = Modifier
                            .width(26.dp)
                            .height(26.dp),
                        contentDescription = stringResource(id = R.string.description_plus)
                    )
                }
                Text(
                    text = successPage.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = dimensionResource(id = R.dimen.item_vertical_margin)
                        )
                        .semantics {
                            testTag = confirmationTitleTag
                        },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                AndroidView(
                    modifier = Modifier
                        .padding(bottom = dimensionResource(id = R.dimen.item_vertical_margin))
                        .semantics {
                            testTag = BODY_TAG
                        },
                    factory = { TextView(it) },
                    update = {
                        it.setHtmlString(successPage.body)
                    }
                )
            }
            Button(
                onClick = onConfirmed,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        testTag = confirmationConfirmButtonTag
                    }
            ) {
                Text(text = successPage.buttonText)
            }
        }
    }
}

internal const val confirmationTitleTag = "ConfirmationTitle"
internal const val confirmationConfirmButtonTag = "ConfirmButton"
