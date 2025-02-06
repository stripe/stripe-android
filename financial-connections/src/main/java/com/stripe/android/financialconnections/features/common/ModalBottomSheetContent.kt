package com.stripe.android.financialconnections.features.common

import FinancialConnectionsGenericInfoScreen
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.features.generic.GenericScreen
import com.stripe.android.financialconnections.features.generic.GenericScreenState
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.sdui.rememberHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.Layout

@Composable
internal fun DataAccessBottomSheetContent(
    dataDialog: DataAccessNotice,
    onClickableTextClick: (String) -> Unit,
    onConfirmModalClick: () -> Unit
) {
    val title = rememberHtml(dataDialog.title)
    val subtitle = dataDialog.subtitle?.let { rememberHtml(it) }
    val disclaimer = dataDialog.disclaimer?.let { rememberHtml(it) }
    val bullets = remember(dataDialog.body.bullets) {
        dataDialog.body.bullets.map { BulletUI.from(it) }
    }
    ModalBottomSheetContent(
        onClickableTextClick = onClickableTextClick,
        cta = dataDialog.cta,
        disclaimer = disclaimer,
        onConfirmModalClick = onConfirmModalClick,
        content = {
            dataDialog.icon?.default?.let {
                ShapedIcon(url = it, contentDescription = "Icon")
                Spacer(modifier = Modifier.size(16.dp))
            }
            Title(title = title, onClickableTextClick = onClickableTextClick)
            // FOR CONNECTED ACCOUNTS: Permissions granted to Stripe by the connected account
            dataDialog.connectedAccountNotice?.let {
                Spacer(modifier = Modifier.size(16.dp))
                Subtitle(
                    text = rememberHtml(it.subtitle),
                    onClickableTextClick = onClickableTextClick
                )
                Spacer(modifier = Modifier.size(24.dp))
                it.body.bullets.forEach { bullet ->
                    ListItem(
                        bullet = BulletUI.from(bullet),
                        onClickableTextClick = onClickableTextClick
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
            // FOR ALL MERCHANTS: Permissions granted to Stripe by the merchant
            subtitle?.let {
                Spacer(modifier = Modifier.size(16.dp))
                Subtitle(it, onClickableTextClick)
            }
            Spacer(modifier = Modifier.size(24.dp))
            bullets.forEach { bullet ->
                ListItem(
                    bullet = bullet,
                    onClickableTextClick = onClickableTextClick
                )
                Spacer(modifier = Modifier.size(16.dp))
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
    val title = rememberHtml(legalDetails.title)
    val subtitle = legalDetails.subtitle?.let { rememberHtml(it) }
    val learnMore = legalDetails.disclaimer?.let { rememberHtml(it) }
    val links = remember(legalDetails.body.links) {
        legalDetails.body.links.map { TextResource.Text(fromHtml(it.title)) }
    }
    ModalBottomSheetContent(
        onClickableTextClick = onClickableTextClick,
        cta = legalDetails.cta,
        disclaimer = learnMore,
        onConfirmModalClick = onConfirmModalClick
    ) {
        legalDetails.icon?.default?.let {
            ShapedIcon(it, contentDescription = "legal details icon")
            Spacer(modifier = Modifier.size(16.dp))
        }

        Title(title = title, onClickableTextClick = onClickableTextClick)

        subtitle?.let {
            Spacer(modifier = Modifier.size(16.dp))
            Subtitle(text = it, onClickableTextClick = onClickableTextClick)
        }

        Spacer(modifier = Modifier.size(24.dp))

        Links(links, onClickableTextClick)
    }
}

@Composable
internal fun GenericBottomSheetContent(
    screen: FinancialConnectionsGenericInfoScreen,
    onClickableTextClick: (String) -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
) {
    GenericScreen(
        state = GenericScreenState(screen, inModal = true),
        onPrimaryButtonClick = onPrimaryButtonClick,
        onSecondaryButtonClick = onSecondaryButtonClick,
        onClickableTextClick = onClickableTextClick,
    )
}

@Composable
private fun Links(
    links: List<TextResource.Text>,
    onClickableTextClick: (String) -> Unit,
) {
    Column {
        val linkStyle = typography.labelLargeEmphasized.copy(color = colors.textAction)
        links.forEachIndexed { index, link ->
            Divider(color = colors.borderNeutral, thickness = 0.5.dp)
            AnnotatedText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                text = link,
                defaultStyle = linkStyle,
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to linkStyle.toSpanStyle()
                ),
                onClickableTextClick = onClickableTextClick,
            )
            if (links.lastIndex == index) {
                Divider(color = colors.borderNeutral, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun Title(
    title: TextResource.Text,
    onClickableTextClick: (String) -> Unit
) {
    AnnotatedText(
        text = title,
        defaultStyle = typography.headingLarge.copy(
            color = colors.textDefault
        ),
        onClickableTextClick = onClickableTextClick
    )
}

@Composable
private fun ModalBottomSheetContent(
    onClickableTextClick: (String) -> Unit,
    cta: String,
    disclaimer: TextResource?,
    onConfirmModalClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Layout(
        modifier = modifier,
        inModal = true,
        body = { content() },
        footer = {
            ModalBottomSheetFooter(
                onClickableTextClick = onClickableTextClick,
                disclaimer = disclaimer,
                onConfirmModalClick = onConfirmModalClick,
                cta = cta
            )
        }
    )
}

@Composable
private fun Subtitle(
    text: TextResource,
    onClickableTextClick: (String) -> Unit
) {
    AnnotatedText(
        text = text,
        defaultStyle = typography.bodyMedium.copy(
            color = colors.textDefault
        ),
        onClickableTextClick = onClickableTextClick
    )
}

@Composable
private fun ModalBottomSheetFooter(
    onClickableTextClick: (String) -> Unit,
    disclaimer: TextResource?,
    onConfirmModalClick: () -> Unit,
    cta: String
) = Column {
    disclaimer?.let {
        AnnotatedText(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = it,
            onClickableTextClick = onClickableTextClick,
            defaultStyle = typography.labelSmall.copy(
                color = colors.textDefault,
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
