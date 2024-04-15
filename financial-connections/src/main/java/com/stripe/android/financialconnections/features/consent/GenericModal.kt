package com.stripe.android.financialconnections.features.consent

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.features.common.ListItem
import com.stripe.android.financialconnections.features.common.ShapedIcon
import com.stripe.android.financialconnections.features.common.Subtitle
import com.stripe.android.financialconnections.features.common.Title
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.serializer.MarkdownToHtmlSerializer
import com.stripe.android.financialconnections.repository.PersistingRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.sdui.BulletUI
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Layout
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val json = Json {
    ignoreUnknownKeys = true
}

private val modalPayload = """
    {
      "modal": {
          "icon": {
            "default": "https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--platform-stripeBrand-3x.png"
          },
        "title": "Data sharing",
        "subtitle": "Airbnb only accesses the following data to make transfers, ensure you have enough funds to complete payments, and to help prevent fraud.",
        "body": {
            "bullets": [
              {
                "icon": {
                  "default": "https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--bank-primary-3x.png"
                },
                "title": "Account details"
              }
            ]
        },
        "cta": "OK",
        "disclaimer": "Learn about [data shared with Stripe](https://support.stripe.com/user/questions/what-data-does-stripe-access-from-my-linked-financial-account?eventName=click.data_access.learn_more) and [how to disconnect](https://support.stripe.com/user/how-do-i-disconnect-my-linked-financial-account?eventName=click.disconnect_link)"
      }
    }
""".trimIndent()


@Preview
@Composable
internal fun GenericModalPreview() {
    val payload = remember {
        json.decodeFromString<ModalPayload>(modalPayload)
    }

    FinancialConnectionsPreview {
        Surface(color = FinancialConnectionsTheme.colors.backgroundSurface) {
            GenericModal(payload = payload.modal)
        }
    }
}

@Composable
internal fun GenericModal(
    payload: Modal,
    modifier: Modifier = Modifier
) {
    Layout(
        inModal = true,
        footer = {
            ModalFooter(payload = payload)
        },
    ) {
        payload.icon?.default?.let {
            ShapedIcon(it, contentDescription = null)
            Spacer(modifier = Modifier.size(16.dp))
        }

        Title(
            title = TextResource.Text(payload.title),
            onClickableTextClick = {},
        )

        Spacer(modifier = Modifier.size(16.dp))

        Subtitle(
            text = TextResource.Text(fromHtml(payload.subtitle)),
            onClickableTextClick = {},
        )

        Spacer(modifier = Modifier.size(24.dp))

        payload.body.bullets.forEachIndexed { index, bullet ->
            ListItem(
                bullet = BulletUI.from(bullet),
                onClickableTextClick = {}
            )

            if (index != payload.body.bullets.lastIndex) {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ModalFooter(
    payload: Modal,
    modifier: Modifier = Modifier,
) {
    Column {
        payload.disclaimer?.let { disclaimer ->
            AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = TextResource.Text(fromHtml(disclaimer)),
                onClickableTextClick = {},
                defaultStyle = FinancialConnectionsTheme.typography.labelSmall.copy(
                    color = FinancialConnectionsTheme.colors.textDefault,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.size(16.dp))
        }

        FinancialConnectionsButton(
            loading = false,
            enabled = true,
            type = FinancialConnectionsButton.Type.Primary,
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = payload.cta)
        }
    }
}

@Singleton
internal class GenericModalContentRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : PersistingRepository<Modal>(
    savedStateHandle = savedStateHandle,
)

@Serializable
internal data class ModalPayload(
    val modal: Modal,
)

@Serializable
@Parcelize
internal data class Modal(
    @SerialName("icon")
    val icon: Image?,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val subtitle: String,
    @SerialName("body")
    val body: Body,
    @SerialName("disclaimer")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val disclaimer: String? = null,
    @SerialName("cta")
    val cta: String,
) : Parcelable {

    @Serializable
    @Parcelize
    internal data class Body(
        @SerialName("bullets") val bullets: List<Bullet>,
    ) : Parcelable
}
