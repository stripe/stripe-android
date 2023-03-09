package com.stripe.android.identity.utils

import android.net.Uri
import java.net.URI

internal fun Uri.isRemote() = scheme == "http" || scheme == "https"

internal fun Uri.urlWithoutQuery() = URI(scheme, authority, path, null, fragment).toString()
