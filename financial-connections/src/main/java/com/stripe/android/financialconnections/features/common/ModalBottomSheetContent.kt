@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.ui.ImageResource
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
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
    val learnMore = remember(dataDialog.learnMore) {
        TextResource.Text(fromHtml(dataDialog.learnMore))
    }
    val connectedAccountNotice = remember(dataDialog.connectedAccountNotice) {
        dataDialog.connectedAccountNotice?.let { TextResource.Text(fromHtml(it)) }
    }
    val bullets = remember(dataDialog.body.bullets) {
        dataDialog.body.bullets.map { BulletUI.from(it) }
    }
    ModalBottomSheetContent(
        title = title,
        subtitle = subtitle,
        onClickableTextClick = onClickableTextClick,
        bullets = bullets,
        connectedAccountNotice = connectedAccountNotice,
        cta = dataDialog.cta,
        learnMore = learnMore,
        onConfirmModalClick = onConfirmModalClick,
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
    val learnMore = remember(legalDetails.learnMore) {
        TextResource.Text(fromHtml(legalDetails.learnMore))
    }
    val bullets = remember(legalDetails.body.bullets) {
        legalDetails.body.bullets.map { BulletUI.from(it) }
    }
    ModalBottomSheetContent(
        title = title,
        subtitle = null,
        onClickableTextClick = onClickableTextClick,
        bullets = bullets,
        connectedAccountNotice = null,
        cta = legalDetails.cta,
        learnMore = learnMore,
        onConfirmModalClick = onConfirmModalClick,
    )
}

@Composable
private fun ModalBottomSheetContent(
    title: TextResource.Text,
    subtitle: TextResource.Text?,
    onClickableTextClick: (String) -> Unit,
    bullets: List<BulletUI>,
    connectedAccountNotice: TextResource?,
    cta: String,
    learnMore: TextResource,
    onConfirmModalClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            AnnotatedText(
                text = title,
                defaultStyle = typography.heading.copy(
                    color = colors.textPrimary
                ),
                annotationStyles = emptyMap(),
                onClickableTextClick = onClickableTextClick
            )
            subtitle?.let {
                Spacer(modifier = Modifier.size(4.dp))
                AnnotatedText(
                    text = it,
                    defaultStyle = typography.body.copy(
                        color = colors.textPrimary
                    ),
                    annotationStyles = mapOf(
                        StringAnnotation.CLICKABLE to typography.detail
                            .toSpanStyle()
                            .copy(color = colors.textBrand),
                        StringAnnotation.BOLD to typography.detailEmphasized
                            .toSpanStyle()
                            .copy(color = colors.textPrimary),
                    ),
                    onClickableTextClick = onClickableTextClick
                )
            }
            bullets.forEach {
                Spacer(modifier = Modifier.size(16.dp))
                BulletItem(
                    bullet = it,
                    onClickableTextClick = onClickableTextClick
                )
            }
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
                    defaultStyle = typography.caption.copy(
                        color = colors.textSecondary
                    ),
                    annotationStyles = mapOf(
                        StringAnnotation.CLICKABLE to typography.captionEmphasized
                            .toSpanStyle()
                            .copy(color = colors.textBrand),
                        StringAnnotation.BOLD to typography.captionEmphasized
                            .toSpanStyle()
                            .copy(color = colors.textSecondary),
                    )
                )
                Spacer(modifier = Modifier.size(12.dp))
            }
            AnnotatedText(
                text = learnMore,
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.caption.copy(
                    color = colors.textSecondary
                ),
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to typography.captionEmphasized
                        .toSpanStyle()
                        .copy(color = colors.textBrand),
                    StringAnnotation.BOLD to typography.captionEmphasized
                        .toSpanStyle()
                        .copy(color = colors.textSecondary),
                )
            )
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
        Spacer(modifier = Modifier.size(8.dp))
        Column {
            when {
                // title + content
                bullet.title != null && bullet.content != null -> {
                    AnnotatedText(
                        text = bullet.title,
                        defaultStyle = typography.body.copy(
                            color = colors.textPrimary
                        ),
                        annotationStyles = mapOf(
                            StringAnnotation.CLICKABLE to typography.bodyEmphasized
                                .toSpanStyle()
                                .copy(color = colors.textBrand),
                            StringAnnotation.BOLD to typography.bodyEmphasized
                                .toSpanStyle()
                                .copy(color = colors.textPrimary),
                        ),
                        onClickableTextClick = onClickableTextClick
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    AnnotatedText(
                        text = bullet.content,
                        defaultStyle = typography.detail.copy(
                            color = colors.textSecondary
                        ),
                        annotationStyles = mapOf(
                            StringAnnotation.CLICKABLE to typography.detailEmphasized
                                .toSpanStyle()
                                .copy(color = colors.textBrand),
                            StringAnnotation.BOLD to typography.detailEmphasized
                                .toSpanStyle()
                                .copy(color = colors.textSecondary),
                        ),
                        onClickableTextClick = onClickableTextClick
                    )
                }
                // only title
                bullet.title != null -> {
                    AnnotatedText(
                        text = bullet.title,
                        defaultStyle = typography.body.copy(
                            color = colors.textPrimary
                        ),
                        annotationStyles = mapOf(
                            StringAnnotation.CLICKABLE to typography.bodyEmphasized
                                .toSpanStyle()
                                .copy(color = colors.textBrand),
                            StringAnnotation.BOLD to typography.bodyEmphasized
                                .toSpanStyle()
                                .copy(color = colors.textPrimary),
                        ),
                        onClickableTextClick = onClickableTextClick
                    )
                }
                // only content
                bullet.content != null -> {
                    AnnotatedText(
                        text = bullet.content,
                        defaultStyle = typography.body.copy(
                            color = colors.textSecondary
                        ),
                        annotationStyles = mapOf(
                            StringAnnotation.CLICKABLE to typography.bodyEmphasized
                                .toSpanStyle()
                                .copy(color = colors.textBrand),
                            StringAnnotation.BOLD to typography.bodyEmphasized
                                .toSpanStyle()
                                .copy(color = colors.textSecondary),
                        ),
                        onClickableTextClick = onClickableTextClick
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletIcon(icon: ImageResource?) {
    val modifier = Modifier
        .size(16.dp)
        .offset(y = 2.dp)
    if (icon == null) {
        val color = colors.textPrimary
        Canvas(
            modifier = Modifier
                .size(16.dp)
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
                    val color = colors.textSecondary
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
