@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.financialconnections.ui.theme.Layout

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
            itemsIndexed(bullets) { index, bullet ->
                ListItem(
                    bullet = bullet,
                    onClickableTextClick = onClickableTextClick
                )
                if (index < bullets.lastIndex) {
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
        itemsIndexed(links) { index, link ->
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
    content: LazyListScope.() -> Unit,
) {
    Layout(
        inModal = true,
        body = {
            item {
                icon?.default?.let {
                    Spacer(modifier = Modifier.size(24.dp))
                    ShapedIcon(url = it, contentDescription = title.toText().toString())
                    Spacer(modifier = Modifier.size(24.dp))
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
            }
            content()
        },
        footer = {
            ModalBottomSheetFooter(
                connectedAccountNotice = connectedAccountNotice,
                onClickableTextClick = onClickableTextClick,
                disclaimer = disclaimer,
                onConfirmModalClick = onConfirmModalClick,
                cta = cta
            )
        }
    )
}

@Composable
private fun ModalBottomSheetFooter(
    connectedAccountNotice: TextResource?,
    onClickableTextClick: (String) -> Unit,
    disclaimer: TextResource?,
    onConfirmModalClick: () -> Unit,
    cta: String
) = Column {
    connectedAccountNotice?.let {
        Spacer(modifier = Modifier.size(16.dp))
        AnnotatedText(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = it,
            onClickableTextClick = onClickableTextClick,
            defaultStyle = v3Typography.labelSmall.copy(
                color = v3Colors.textDefault,
                textAlign = TextAlign.Center
            ),
        )
    }
    disclaimer?.let {
        Spacer(modifier = Modifier.size(16.dp))
        AnnotatedText(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = it,
            onClickableTextClick = onClickableTextClick,
            defaultStyle = v3Typography.labelSmall.copy(
                color = v3Colors.textDefault,
                textAlign = TextAlign.Center
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
