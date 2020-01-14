package com.stripe.android

import android.content.Context
import java.io.File

internal class FileFactory(private val context: Context) {
    fun create(): File {
        val imageResource = context.classLoader.getResource("example.png")
        return File(imageResource.path)
    }
}
