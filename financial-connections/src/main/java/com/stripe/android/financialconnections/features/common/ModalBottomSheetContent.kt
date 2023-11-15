@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.ui.ImageResource
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.Brand50
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.uicore.image.StripeImage

@Composable
internal fun DataAccessBottomSheetContent(
    dataDialog: DataAccessNotice,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit
) {
    val title = remember(dataDialog.title) {
        TextResource.Text(fromHtml(dataDialog.title))
    }
    val subtitle = remember(dataDialog.subtitle) {
        dataDialog.subtitle?.let { TextResource.Text(fromHtml(it)) }
    }
    val disclaimer = remember(dataDialog.disclaimer) {
        dataDialog.disclaimer?.let { TextResource.Text(fromHtml(it)) }
    }
    val connectedAccountNotice = remember(dataDialog.connectedAccountNotice) {
        dataDialog.connectedAccountNotice?.let { TextResource.Text(fromHtml(it)) }
    }
    val bullets = remember(dataDialog.body.bullets) {
        dataDialog.body.bullets.map { BulletUI.from(it) }
    }
    ModalBottomSheetContent(
        icon = dataDialog.icon,
        title = title,
        subtitle = subtitle,
        onClickableTextClick = onClickableTextClick,
        connectedAccountNotice = connectedAccountNotice,
        cta = dataDialog.cta,
        disclaimer = disclaimer,
        onConfirmModalClick = onConfirmModalClick,
        content = {
            bullets.forEachIndexed { index, bullet ->
                BulletItem(
                    bullet = bullet,
                    onClickableTextClick = onClickableTextClick
                )
                if (index < bullets.size - 1) {
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
        }
    )
}

@Composable
internal fun LegalDetailsBottomSheetContent(
    legalDetails: LegalDetailsNotice,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit
) {
    val title = remember(legalDetails.title) {
        TextResource.Text(fromHtml(legalDetails.title))
    }
    val learnMore = remember(legalDetails.disclaimer) {
        legalDetails.disclaimer?.let { TextResource.Text(fromHtml(it)) }
    }
    val links = remember(legalDetails.body.links) {
        legalDetails.body.links.map { TextResource.Text(fromHtml(it.title)) }
    }
    ModalBottomSheetContent(
        icon = legalDetails.icon,
        title = title,
        subtitle = null,
        onClickableTextClick = onClickableTextClick,
        connectedAccountNotice = null,
        cta = legalDetails.cta,
        disclaimer = learnMore,
        onConfirmModalClick = onConfirmModalClick
    ) {
        links.forEachIndexed { index, link ->
            Divider(color = v3Colors.border, modifier = Modifier.padding(bottom = 16.dp))
            AnnotatedText(
                modifier = Modifier.padding(bottom = 16.dp),
                text = link,
                defaultStyle = v3Typography.labelLargeEmphasized.copy(
                    color = v3Colors.textBrand
                ),
                // remove annotation styles to avoid link underline (the default)
                annotationStyles = emptyMap(),
                onClickableTextClick = onClickableTextClick,
            )
            if (links.lastIndex == index) {
                Divider(color = v3Colors.border)
            }
        }
    }
}

@Composable
private fun ModalBottomSheetContent(
    icon: Image?,
    title: TextResource,
    subtitle: TextResource?,
    onClickableTextClick: (String) -> Unit,
    connectedAccountNotice: TextResource?,
    cta: String,
    disclaimer: TextResource?,
    onConfirmModalClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Column {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            icon?.default?.let {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .background(color = Brand50, shape = CircleShape)
                ) {
                    StripeImage(
                        url = it,
                        imageLoader = LocalImageLoader.current,
                        contentDescription = "Web Icon",
                        modifier = Modifier
                            .size(20.dp),
                        contentScale = ContentScale.Crop // Adjust the scaling if needed
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
            }
            AnnotatedText(
                text = title,
                defaultStyle = v3Typography.headingMedium.copy(
                    color = v3Colors.textDefault
                ),
                onClickableTextClick = onClickableTextClick
            )
            subtitle?.let {
                Spacer(modifier = Modifier.size(16.dp))
                AnnotatedText(
                    text = it,
                    defaultStyle = v3Typography.bodyMedium.copy(
                        color = v3Colors.textDefault
                    ),
                    onClickableTextClick = onClickableTextClick
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
            content()
        }
        Column(
            Modifier.padding(
                bottom = 24.dp,
                start = 24.dp,
                end = 24.dp
            )
        ) {
            if (connectedAccountNotice != null) {
                AnnotatedText(
                    text = connectedAccountNotice,
                    onClickableTextClick = onClickableTextClick,
                    defaultStyle = v3Typography.labelSmall.copy(
                        color = v3Colors.textDefault
                    ),
                )
                Spacer(modifier = Modifier.size(12.dp))
            }
            if (disclaimer != null) {
                AnnotatedText(
                    text = disclaimer,
                    onClickableTextClick = onClickableTextClick,
                    defaultStyle = v3Typography.labelSmall.copy(
                        color = v3Colors.textDefault
                    ),
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            FinancialConnectionsButton(
                onClick = { onConfirmModalClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = cta)
            }
        }
    }
}

@Composable
internal fun BulletItem(
    bullet: BulletUI,
    onClickableTextClick: (String) -> Unit
) {
    Row {
        BulletIcon(icon = bullet.imageResource)
        Spacer(modifier = Modifier.size(16.dp))
        val shouldEmphasize = remember(bullet) { bullet.title != null && bullet.content != null }
        Column {
            if (bullet.title != null) {
                val titleStyle = if (shouldEmphasize) v3Typography.bodyMediumEmphasized else v3Typography.bodyMedium
                AnnotatedText(
                    text = bullet.title,
                    defaultStyle = titleStyle.copy(color = v3Colors.textDefault),
                    onClickableTextClick = onClickableTextClick
                )
            }
            if (bullet.content != null) {
                AnnotatedText(
                    text = bullet.content,
                    defaultStyle = v3Typography.bodySmall.copy(
                        color = v3Colors.textSubdued
                    ),
                    onClickableTextClick = onClickableTextClick
                )
            }
        }
    }
}

@Composable
private fun BulletIcon(icon: ImageResource?) {
    val modifier = Modifier
        .size(20.dp)
        .offset(y = 2.dp)
    if (icon == null) {
        val color = v3Colors.iconDefault
        Canvas(
            modifier = Modifier
                .size(20.dp)
                .padding(6.dp)
                .offset(y = 2.dp),
            onDraw = { drawCircle(color = color) }
        )
    } else {
        when (icon) {
            is ImageResource.Local -> Image(
                modifier = modifier,
                painter = painterResource(id = icon.resId),
                contentDescription = null,
            )

            is ImageResource.Network -> StripeImage(
                url = icon.url,
                errorContent = {
                    val color = v3Colors.iconDefault
                    Canvas(
                        modifier = Modifier
                            .size(6.dp)
                            .align(Alignment.Center),
                        onDraw = { drawCircle(color = color) }
                    )
                },
                imageLoader = LocalImageLoader.current,
                contentDescription = null,
                modifier = modifier
            )
        }
    }
}
