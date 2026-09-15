package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlaceholderVerticalAlign
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.text.EmbeddableImage
import com.stripe.android.uicore.text.Html
import com.stripe.example.R

class StripeImageActivity : AppCompatActivity() {
    private val LocalStripeImageLoader = staticCompositionLocalOf<StripeImageLoader> {
        error("No ImageLoader provided")
    }

    private val imageLoader by lazy {
        DefaultStripeImageLoader(
            context = this,
            memoryCache = null,
            diskCache = null
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalStripeImageLoader provides imageLoader) {
                Column {
                    Html(
                        imageLoader = mapOf(
                            "card" to EmbeddableImage.Drawable(
                                R.drawable.ic_credit_card,
                                R.string.create_card_payment_method
                            )
                        ),
                        html = """
                            HTML with single local image
                            <br/>
                            Local image <img src="card"/>.
                            <br/>
                        """.trimIndent(),
                        color = MaterialTheme.colors.onSurface,
                        style = MaterialTheme.typography.body1
                    )
                    Html(
                        imageLoader = mapOf(
                            "card" to EmbeddableImage.Drawable(
                                R.drawable.ic_credit_card,
                                R.string.create_card_payment_method
                            )
                        ),
                        html = """
                            HTML with local and remote images
                            <br/>
                            Local image <img src="card"/>.
                            <br/>
                            Unknown remote image <img src="https://qa-b.stripecdn.com/unknown_image.png"/>
                            <br/>
                            Remote images 
                            <img src="https://qa-b.stripecdn.com/payment-method-messaging-statics-srv/assets/afterpay_logo_black.png"/>
                            <img src="https://qa-b.stripecdn.com/payment-method-messaging-statics-srv/assets/klarna_logo_black.png"/>
                            <br/>
                            Paragraph text <b>bold text</b>. ⓘ
                        """.trimIndent(),
                        color = MaterialTheme.colors.onSurface,
                        style = MaterialTheme.typography.body1,
                        imageAlign = PlaceholderVerticalAlign.TextCenter
                    )
                }
            }
        }
    }
}
