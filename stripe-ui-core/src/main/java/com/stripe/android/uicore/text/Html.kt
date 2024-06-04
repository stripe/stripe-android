package com.stripe.android.uicore.text

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.image.isSupportedImageUrl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow

private const val LINK_TAG = "URL"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class EmbeddableImage {
    data class Drawable(
        @DrawableRes val id: Int,
        @StringRes val contentDescription: Int,
        val colorFilter: androidx.compose.ui.graphics.ColorFilter? = null
    ) : EmbeddableImage()

    data class Bitmap(
        val bitmap: android.graphics.Bitmap
    ) : EmbeddableImage()
}

@Composable
private fun rememberDrawableImages(
    drawableImageLoader: Map<String, EmbeddableImage.Drawable>,
    imageAlign: PlaceholderVerticalAlign
): Map<String, InlineTextContent> {
    return drawableImageLoader.entries.associate { (key, value) ->
        val painter = painterResource(value.id)
        val height = painter.intrinsicSize.height
        val width = painter.intrinsicSize.width
        val newWidth = MaterialTheme.typography.body1.fontSize * (width / height)

        key to InlineTextContent(
            Placeholder(
                newWidth,
                MaterialTheme.typography.body1.fontSize,
                imageAlign
            ),
            children = {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    painter = painter,
                    contentDescription = stringResource(
                        value.contentDescription
                    ),
                    colorFilter = value.colorFilter
                )
            }
        )
    }
}

@Composable
private fun rememberBitmapImages(
    bitmapImageLoader: Map<String, EmbeddableImage.Bitmap>,
    imageAlign: PlaceholderVerticalAlign
): Map<String, InlineTextContent> {
    return bitmapImageLoader.entries.associate { (key, image) ->
        val localDensity = LocalDensity.current
        val size = with(localDensity) {
            Size(
                image.bitmap.width.toFloat(),
                image.bitmap.height.toFloat()
            ).times(1 / density)
        }
        key to InlineTextContent(
            Placeholder(
                width = size.width.sp,
                height = size.height.sp,
                imageAlign
            ),
            children = {
                Image(
                    bitmap = image.bitmap.asImageBitmap(),
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun rememberRemoteImages(
    annotatedText: AnnotatedString,
    imageLoader: Map<String, EmbeddableImage>,
    stripeImageLoader: StripeImageLoader,
    imageAlign: PlaceholderVerticalAlign,
    onLoaded: () -> Unit
): State<Map<String, InlineTextContent>> {
    val remoteUrls = annotatedText.getStringAnnotations(
        start = 0,
        end = annotatedText.length
    ).filter {
        it.item.let { url ->
            url.isSupportedImageUrl() && !imageLoader.keys.contains(url)
        }
    }

    val remoteImages = remember { MutableStateFlow<Map<String, InlineTextContent>>(emptyMap()) }
    val localDensity = LocalDensity.current

    if (remoteUrls.isNotEmpty()) {
        LaunchedEffect(annotatedText) {
            val deferred = remoteUrls.map { url ->
                async {
                    Pair(url.item, stripeImageLoader.load(url.item).getOrNull())
                }
            }

            val bitmaps = deferred.awaitAll().mapNotNull { pair ->
                pair.second?.let { bitmap ->
                    Pair(pair.first, bitmap)
                }
            }.toMap()
            remoteImages.value = bitmaps.mapValues { entry ->
                val size = with(localDensity) {
                    Size(
                        entry.value.width.toFloat(),
                        entry.value.height.toFloat()
                    ).times(1 / density)
                }
                InlineTextContent(
                    placeholder = Placeholder(
                        width = size.width.sp,
                        height = size.height.sp,
                        placeholderVerticalAlign = imageAlign
                    ),
                    children = {
                        StripeImage(
                            url = entry.key,
                            imageLoader = stripeImageLoader,
                            contentDescription = null,
                            modifier = Modifier
                                .width(size.width.dp)
                                .height(size.height.dp)
                        )
                    }
                )
            }
            onLoaded()
        }
    }

    return remoteImages.collectAsState()
}

/**
 * This will display html annotated text in a string.  Images cannot be embedded in
 * <a> link tags.  The following tags are supported: <a>, <b>, <u>, <i>, <img>, <li>
 * Local/remote sources value in the img tab, must map to something in the imageLoader.
 *
 * When an img tag does not map to a EmbeddableImage, then this will use [StripeImageLoader] to
 * retrieve the images and only renders once the images are downloaded.
 *
 * When the text is clicked, the FIRST parsed URL from <a> is opened through [Intent.ACTION_VIEW].
 *
 * @param html The HTML to render
 * @param imageLoader data source mapping img tags with images
 * @param color The color of the text defaults to [Color.Unspecified]
 * @param enabled Whether URLs should be clickable or not, defaults to true
 * @param urlSpanStyle The style given to URLs, defaults to [TextDecoration.Underline]
 * @param imageAlign The vertical alignment for the image in relation to the text, defaults to [PlaceholderVerticalAlign.AboveBaseline]
 * @param onClick Additional callback when the URL is opened.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Html(
    html: String,
    modifier: Modifier = Modifier,
    imageLoader: Map<String, EmbeddableImage> = emptyMap(),
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    enabled: Boolean = true,
    urlSpanStyle: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
    imageAlign: PlaceholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    HtmlWithCustomOnClick(
        html,
        modifier,
        imageLoader,
        color,
        style,
        urlSpanStyle,
        imageAlign
    ) { annotatedStringRanges ->
        if (enabled) {
            onClick()
            annotatedStringRanges.firstOrNull()?.let { annotation ->
                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse(annotation.item)
                context.startActivity(openURL)
            }
        }
    }
}

/**
 * This will display html annotated text in a string.  Images cannot be embedded in
 * <a> link tags.  The following tags are supported: <a>, <b>, <u>, <i>, <img>, <li>
 * Local/remote sources value in the img tab, must map to something in the imageLoader.
 *
 * When an img tag does not map to a EmbeddableImage, then this will use [StripeImageLoader] to
 * retrieve the images and only renders once the images are downloaded.
 *
 * When the text is clicked, a callback returns ALL URLs from <a> as annotatedStrings.
 *
 * @param html The HTML to render
 * @param imageLoader data source mapping img tags with images
 * @param color The color of the text defaults to [Color.Unspecified]
 * @param urlSpanStyle The style given to URLs, defaults to [TextDecoration.Underline]
 * @param imageAlign The vertical alignment for the image in relation to the text, defaults to [PlaceholderVerticalAlign.AboveBaseline]
 * @param onClick Callback returning all annotatedStrings with URL when the text is clicked.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun HtmlWithCustomOnClick(
    html: String,
    modifier: Modifier = Modifier,
    imageLoader: Map<String, EmbeddableImage> = emptyMap(),
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    urlSpanStyle: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
    imageAlign: PlaceholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline,
    onClick: (List<AnnotatedString.Range<String>>) -> Unit
) {
    val context = LocalContext.current
    val annotatedText = annotatedStringResource(html, imageLoader, urlSpanStyle)
    val remoteImagesLoaded = remember { mutableStateOf(false) }
    val stripeImageLoader = remember {
        StripeImageLoader(
            context = context,
            diskCache = null
        )
    }

    @Suppress("UNCHECKED_CAST")
    val drawableImages = rememberDrawableImages(
        drawableImageLoader = imageLoader.filterValues {
            it is EmbeddableImage.Drawable
        } as Map<String, EmbeddableImage.Drawable>,
        imageAlign = imageAlign
    )

    @Suppress("UNCHECKED_CAST")
    val bitmapImages = rememberBitmapImages(
        bitmapImageLoader = imageLoader.filterValues {
            it is EmbeddableImage.Bitmap
        } as Map<String, EmbeddableImage.Bitmap>,
        imageAlign = imageAlign
    )

    val remoteImages = rememberRemoteImages(
        annotatedText = annotatedText,
        imageLoader = imageLoader,
        stripeImageLoader = stripeImageLoader,
        imageAlign = imageAlign
    ) {
        remoteImagesLoaded.value = true
    }.value

    val shouldRenderImmediately = remoteImages.isEmpty()

    if (shouldRenderImmediately || remoteImagesLoaded.value) {
        ClickableText(
            annotatedText,
            modifier = modifier
                .semantics(mergeDescendants = true) {}, // makes it a separate accessible item,
            inlineContent = drawableImages + bitmapImages + remoteImages,
            color = color,
            style = style,
            onClick = {
                // Position is the position of the tag in the string
                onClick(annotatedText.getStringAnnotations(LINK_TAG, it, it))
            }
        )
    }
}

/**
 * Load a styled string resource with formatting.
 *
 * @param text the html text
 * @param imageGetter the mapping of string to resource id
 * @return the string data associated with the resource
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun annotatedStringResource(
    text: String,
    imageGetter: Map<String, EmbeddableImage> = emptyMap(),
    urlSpanStyle: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
): AnnotatedString {
    val spanned = remember(text) {
        HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
    return remember(spanned) {
        buildAnnotatedString {
            var currentStart = 0
            spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                if (currentStart < spanned.toString().length &&
                    start < spanned.toString().length &&
                    start - currentStart >= 0
                ) {
                    append(spanned.toString().substring(currentStart, start))
                    currentStart = start
                    when (span) {
                        is StyleSpan -> when (span.style) {
                            Typeface.BOLD -> {
                                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                            }
                            Typeface.ITALIC -> {
                                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                            }
                            Typeface.BOLD_ITALIC -> {
                                addStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = FontStyle.Italic
                                    ),
                                    start,
                                    end
                                )
                            }
                        }
                        is UnderlineSpan -> {
                            addStyle(
                                SpanStyle(textDecoration = TextDecoration.Underline),
                                start,
                                end
                            )
                        }
                        is BulletSpan -> {
                            // append a bullet and a tab character in front
                            append("\u2022\t")
                        }
                        is ForegroundColorSpan -> {
                            addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                        }
                        is ImageSpan -> {
                            currentStart = end
                            span.source?.let {
                                if (imageGetter.isNotEmpty()) {
                                    requireNotNull(imageGetter.containsKey(span.source!!))
                                }
                                appendInlineContent(span.source!!)
                            }
                        }
                        is URLSpan -> {
                            addStyle(
                                urlSpanStyle,
                                start,
                                end
                            )
                            addStringAnnotation(
                                tag = LINK_TAG,
                                annotation = span.url,
                                start = start,
                                end = end
                            )
                        }
                    }
                }
            }
            if (currentStart != spanned.toString().length) {
                append(spanned.toString().substring(currentStart))
            }
        }
    }
}

@Composable
private fun ClickableText(
    text: AnnotatedString,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onClick: (Int) -> Unit
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val pressIndicator = Modifier.pointerInput(onClick) {
        detectTapGestures { pos ->
            var nonPlaceholderPosition = pos
            // If the position is in the bounds of a placeholder set the position to the end of the placeholder.
            layoutResult.value?.placeholderRects?.filterNotNull()?.firstOrNull {
                pos.x > it.topLeft.x && pos.x < it.topRight.x
            }?.let {
                nonPlaceholderPosition = it.topRight.copy(
                    x = it.topRight.x + 0.1f
                )
            }

            layoutResult.value?.let { layoutResult ->
                // we need to account for offset and change to an index by subtracting 1.
                onClick(layoutResult.getOffsetForPosition(nonPlaceholderPosition) - 1)
            }
        }
    }

    BasicText(
        text = text,
        modifier = modifier.then(pressIndicator),
        style = style.copy(
            color = color
        ),
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = {
            layoutResult.value = it
            onTextLayout(it)
        },
        inlineContent = inlineContent
    )
}
