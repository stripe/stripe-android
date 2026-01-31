package com.stripe.android.common.taptoadd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.CardBrand

internal sealed interface TapToAddScreen {
    val hasCancelButton: Boolean

    @Composable
    fun Content()

    data object Collecting : TapToAddScreen {
        override val hasCancelButton: Boolean = false

        @Composable
        override fun Content() {
            Box(Modifier.fillMaxSize()) {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    data class Error(
        val message: ResolvableString
    ) : TapToAddScreen {
        override val hasCancelButton: Boolean = true

        @Composable
        override fun Content() {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.align(Alignment.Center)) { }
            }
        }
    }

    data class Collected(
        val brand: CardBrand,
        val last4: String?,
    ) : TapToAddScreen {
        override val hasCancelButton: Boolean = false

        @Composable
        override fun Content() {
        }
    }

    class Confirmation(
        val interactor: TapToAddConfirmationInteractor,
    ) : TapToAddScreen {
        override val hasCancelButton: Boolean = true

        @Composable
        override fun Content() {
        }
    }
}
