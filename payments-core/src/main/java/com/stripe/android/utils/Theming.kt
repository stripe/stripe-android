package com.stripe.android.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.use
import com.google.accompanist.themeadapter.appcompat.AppCompatTheme
import com.google.accompanist.themeadapter.material.MdcTheme
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.google.accompanist.themeadapter.material.R as MaterialR
import com.google.accompanist.themeadapter.material3.R as Material3R

@Composable
internal fun AppCompatOrMdcTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val isMaterialTheme = remember {
        context.obtainStyledAttributes(MaterialR.styleable.ThemeAdapterMaterialTheme).use { ta ->
            ta.hasValue(MaterialR.styleable.ThemeAdapterMaterialTheme_isMaterialTheme)
        }
    }

    val isMaterial3Theme = remember {
        context.obtainStyledAttributes(Material3R.styleable.ThemeAdapterMaterial3Theme).use { ta ->
            ta.hasValue(Material3R.styleable.ThemeAdapterMaterial3Theme_isMaterial3Theme)
        }
    }

    if (isMaterialTheme) {
        MdcTheme(content = content)
    } else if (isMaterial3Theme) {
        Mdc3Theme(content = content)
    } else {
        AppCompatTheme(content = content)
    }
}
