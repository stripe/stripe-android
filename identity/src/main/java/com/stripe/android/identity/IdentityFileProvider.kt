package com.stripe.android.identity

import androidx.core.content.FileProvider

/**
 * Custom [FileProvider] class to avoid collision of hosting apps.
 */
internal class IdentityFileProvider : FileProvider()
