package com.stripe.android.financialconnections.features.consent

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.core.model.serializers.EnumIgnoreUnknownSerializer
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.features.common.PrepaneImage
import com.stripe.android.financialconnections.features.consent.ContentAlignment.Center
import com.stripe.android.financialconnections.features.consent.ContentAlignment.End
import com.stripe.android.financialconnections.features.consent.ContentAlignment.Start
import com.stripe.android.financialconnections.features.consent.VerticalAlignment.Centered
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.Entry
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.serializer.MarkdownToHtmlSerializer
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.uicore.image.StripeImage
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Preview
@Composable
internal fun GenericScreenPreview() {
    val presenter = remember { GenericScreenPresenter() }
    val screenState by presenter.screenState.collectAsState()

    LaunchedEffect(Unit) {
        val payload = Json.parseToJsonElement(streamlinedConsentPayload)
        presenter.initialize(
            payload = payload,
            scope = this,
        )
    }

    FinancialConnectionsPreview {
        Surface(color = colors.backgroundSurface) {
            screenState?.let { state ->
                GenericScreen(
                    state = state,
                    onClickableTextClick = {},
                    onPrimaryButtonClick = {
                        Log.d("GenericScreenPreview", "Primary button clicked")
                    },
                )
            } ?: run {
                LoadingSpinner()
            }
        }
    }
}

@Composable
internal fun GenericScreen(
    state: ScreenState,
    modifier: Modifier = Modifier,
    onPrimaryButtonClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    val density = LocalDensity.current

    var containerHeight by remember { mutableStateOf(0.dp) }
    var footerHeight by remember { mutableStateOf(0.dp) }
    var contentHeight by remember { mutableStateOf(0.dp) }

    val spacing by remember {
        derivedStateOf {
            ((containerHeight - footerHeight - contentHeight) / 2).takeIf {
                state.screen.options?.verticalAlignment == Centered && it > 0.dp
            }
        }
    }

    Layout(
        bodyPadding = PaddingValues(vertical = 24.dp),
        enableScroll = spacing == null,
        footer = state.screen.footer?.let {
            {
                GenericFooter(
                    payload = it,
                    onPrimaryButtonClick = onPrimaryButtonClick,
                    onClickableTextClick = onClickableTextClick,
                    modifier = Modifier.onGloballyPositioned {
                        with(density) {
                            footerHeight = it.size.height.toDp()
                        }
                    },
                )
            }
        },
        modifier = Modifier.onGloballyPositioned {
            with(density) {
                containerHeight = it.size.height.toDp()
            }
        }
    ) {
        spacing?.let {
            Spacer(modifier = Modifier.height(it))
        }

        Column(
            modifier = Modifier.onGloballyPositioned {
                with(density) {
                    contentHeight = it.size.height.toDp()
                }
            }
        ) {
            GenericHeader(
                payload = state.screen.header,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            GenericBody(
                payload = state.screen.body,
                onClickableTextClick = onClickableTextClick,
            )
        }

        spacing?.let {
            Spacer(modifier = Modifier.height(it))
        }
    }
}

@Composable
internal fun GenericBody(
    payload: Screen.Body,
    modifier: Modifier = Modifier,
    onClickableTextClick: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        payload.entries.forEachIndexed { index, entry ->
            Box {
                when (entry) {
                    is Entry.Image -> {
                        PrepaneImage(entry)
                    }
                    is Entry.Text -> {
                        val font = when (entry.size) {
                            ContentSize.XSmall -> typography.bodyXSmall
                            ContentSize.Small -> typography.bodySmall
                            ContentSize.Medium -> typography.bodyMedium
                        }

                        AnnotatedText(
                            text = TextResource.Text(fromHtml(entry.content)),
                            onClickableTextClick = onClickableTextClick,
                            defaultStyle = font.copy(
                                textAlign = when (entry.alignment) {
                                    Start -> TextAlign.Start
                                    Center -> TextAlign.Center
                                    End -> TextAlign.End
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
            }

            if (index != payload.entries.lastIndex) {
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
internal fun GenericHeader(
    payload: Screen.Header,
    modifier: Modifier = Modifier,
) {
    val textAlign = when (payload.alignment) {
        Start -> TextAlign.Start
        Center -> TextAlign.Center
        End -> TextAlign.End
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        payload.icon?.default?.let { iconUrl ->
            StripeImage(
                url = iconUrl,
                contentDescription = null,
                imageLoader = LocalImageLoader.current,
                errorContent = { },
                loadingContent = {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.LightGray, RoundedCornerShape(16.dp))
                    )
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Spacer(modifier = Modifier.size(32.dp))
        }

        AnnotatedText(
            text = TextResource.Text(payload.title),
            onClickableTextClick = {
                Log.d("TILL123", it)
            },
            defaultStyle = typography.headingXLarge.copy(
                textAlign = textAlign,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (payload.subtitle != null) {
            Spacer(modifier = Modifier.height(16.dp))

            AnnotatedText(
                text = TextResource.Text(fromHtml(payload.subtitle)),
                onClickableTextClick = { },
                defaultStyle = typography.bodyMedium.copy(
                    textAlign = textAlign,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun GenericFooter(
    payload: Screen.Footer,
    modifier: Modifier = Modifier,
    onPrimaryButtonClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    val textAlign = when (payload.alignment) {
        Start -> TextAlign.Start
        Center -> TextAlign.Center
        End -> TextAlign.End
    }

    Column(modifier) {
        payload.disclaimer?.let { disclaimer ->
            AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = TextResource.Text(fromHtml(disclaimer)),
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.labelSmall.copy(
                    color = colors.textDefault,
                    textAlign = textAlign,
                ),
            )

            Spacer(modifier = Modifier.size(16.dp))
        }

        FinancialConnectionsButton(
            loading = false,
            type = FinancialConnectionsButton.Type.Primary,
            onClick = onPrimaryButtonClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = payload.primaryCta.label)

            payload.primaryCta.icon?.default?.let {
                Spacer(modifier = Modifier.size(12.dp))
                StripeImage(
                    url = it,
                    contentDescription = null,
                    imageLoader = LocalImageLoader.current,
                    errorContent = { },
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        payload.secondaryCta?.let { secondaryCta ->
            Spacer(modifier = Modifier.size(8.dp))

            FinancialConnectionsButton(
                loading = false,
                enabled = true,
                type = FinancialConnectionsButton.Type.Secondary,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = secondaryCta.label)
            }
        }

        payload.belowCta?.let { belowCta ->
            Spacer(modifier = Modifier.size(12.dp))

            AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = TextResource.Text(fromHtml(belowCta)),
                onClickableTextClick = onClickableTextClick,
                defaultStyle = typography.labelSmall.copy(
                    color = colors.textDefault,
                    textAlign = textAlign,
                ),
            )
        }
    }
}

@Serializable
internal data class ScreenPayload(
    val screen: Screen,
)

internal data class ScreenState(
    val screen: Screen,
)

@Serializable
@Parcelize
internal data class Screen(
    @SerialName("id") val id: String,
    @SerialName("header") val header: Header,
    @SerialName("body") val body: Body,
    @SerialName("footer") val footer: Footer?,
    @SerialName("options") val options: Options?,
) : Parcelable {

    @Serializable
    @Parcelize
    internal data class Header(
        @SerialName("alignment")
        val alignment: ContentAlignment = Center,
        @SerialName("icon")
        val icon: Image?,
        @SerialName("title")
        val title: String,
        @SerialName("subtitle")
        @Serializable(with = MarkdownToHtmlSerializer::class)
        val subtitle: String? = null,
    ) : Parcelable

    @Serializable
    @Parcelize
    internal data class Body(
//        @SerialName("items") val items: List<Item>,
        @SerialName("entries") val entries: List<Entry>,
    ) : Parcelable {

        @Serializable(with = Item.Serializer::class)
        sealed interface Item {

            @Serializable
            data class Input(
                val key: String,
                val value: String,
                @SerialName("input_type") val type: String,
            ) : Item

            @Serializable
            data class Bulleted(val bullet: Bullet) : Item

            @Serializable
            data class BodyEntry(val entry: Entry) : Item

            object Serializer : JsonContentPolymorphicSerializer<Item>(Item::class) {

                override fun selectDeserializer(element: JsonElement): KSerializer<out Item> {
                    return when (element.typeValue) {
                        TYPE_BULLET -> Bulleted.serializer()
                        TYPE_ENTRY -> BodyEntry.serializer()
                        TYPE_INPUT -> Input.serializer()
                        else -> throw IllegalArgumentException("Unknown type: ${element.typeValue}")
                    }
                }

                /**
                 * gets the `type` value from the given [JsonElement]
                 */
                private val JsonElement.typeValue: String?
                    get() = jsonObject["type"]?.jsonPrimitive?.content
            }

            companion object {
                internal const val TYPE_BULLET = "bullet"
                internal const val TYPE_ENTRY = "entry"
                internal const val TYPE_INPUT = "input"
            }
        }
    }

    @Serializable
    @Parcelize
    internal data class Footer(
        @SerialName("alignment")
        val alignment: ContentAlignment = Center,
        @SerialName("primary_cta")
        val primaryCta: PrimaryButton,
        @SerialName("secondary_cta")
        val secondaryCta: SecondaryButton? = null,
        @SerialName("disclaimer")
        @Serializable(with = MarkdownToHtmlSerializer::class)
        val disclaimer: String? = null,
        @SerialName("below_cta")
        @Serializable(with = MarkdownToHtmlSerializer::class)
        val belowCta: String? = null,
    ) : Parcelable

    @Serializable
    @Parcelize
    internal data class Options(
        @SerialName("vertical_alignment")
        val verticalAlignment: VerticalAlignment = Centered,
    ) : Parcelable
}

@Serializable(with = ContentAlignment.Serializer::class)
internal enum class ContentAlignment {

    @SerialName("left")
    Start,

    @SerialName("center")
    Center,

    @SerialName("right")
    End;

    internal object Serializer : EnumIgnoreUnknownSerializer<ContentAlignment>(entries.toTypedArray(), Center)
}

@Serializable(with = VerticalAlignment.Serializer::class)
internal enum class VerticalAlignment {

    @SerialName("default")
    Default,

    @SerialName("centered")
    Centered;

    internal object Serializer : EnumIgnoreUnknownSerializer<VerticalAlignment>(entries.toTypedArray(), Centered)
}

@Serializable(with = ContentSize.Serializer::class)
internal enum class ContentSize {

    @SerialName("xsmall")
    XSmall,

    @SerialName("small")
    Small,

    @SerialName("medium")
    Medium;

    internal object Serializer : EnumIgnoreUnknownSerializer<ContentSize>(entries.toTypedArray(), Small)
}

@Serializable
@Parcelize
internal data class PrimaryButton(
    @SerialName("icon") val icon: Image? = null,
    @SerialName("label") val label: String,
) : Parcelable

@Serializable
@Parcelize
internal data class SecondaryButton(
    @SerialName("label") val label: String,
) : Parcelable
