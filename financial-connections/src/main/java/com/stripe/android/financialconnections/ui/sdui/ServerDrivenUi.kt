@file:Suppress("MatchingDeclarationName")

package com.stripe.android.financialconnections.ui.sdui

import android.os.Build
import android.text.Html
import android.text.Spanned
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.ui.ImageResource
import com.stripe.android.financialconnections.ui.TextResource

/**
 * UI model of [Bullet] where text content has been transformed to [Spanned] Text resources.
 */
internal data class BulletUI(
    val title: TextResource?,
    val content: TextResource?,
    val imageResource: ImageResource?
) {
    companion object {
        fun from(bullet: Bullet): BulletUI = BulletUI(
            imageResource = bullet.icon?.default?.let { ImageResource.Network(it) },
            title = bullet.title?.let { TextResource.Text(fromHtml(it)) },
            content = bullet.content?.let { TextResource.Text(fromHtml(it)) },
        )
    }
}

@SuppressWarnings("deprecation")
internal fun fromHtml(source: String): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(source)
    }
}
