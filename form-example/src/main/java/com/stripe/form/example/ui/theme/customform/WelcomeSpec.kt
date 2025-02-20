package com.stripe.form.example.ui.theme.customform

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stripe.form.ContentSpec
import kotlinx.collections.immutable.ImmutableList

data class WelcomeSpec(
    val names: ImmutableList<String>
) : ContentSpec {
    @Composable
    override fun Content(modifier: Modifier) {
        val infiniteTransition = rememberInfiniteTransition()

        val color by infiniteTransition.animateColor(
            initialValue = Color.Cyan,
            targetValue = Color.Green,
            animationSpec = infiniteRepeatable(
                animation = tween(5000),
                repeatMode = RepeatMode.Restart
            ),
            label = "color"
        )

        val currentNameIndex by infiniteTransition.animateValue(
            initialValue = 0,
            targetValue = names.size,
            typeConverter = Int.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = tween(5000),
                repeatMode = RepeatMode.Restart
            ),
            label = "nameIndex"
        )

        Text(
            text = "Welcome ${names[currentNameIndex]}",
            style = MaterialTheme.typography.h4,
            color = color
        )
    }

}
