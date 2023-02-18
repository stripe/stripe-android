package com.stripe.android.identity.utils

import android.net.Uri

internal fun Uri.isRemote() = scheme == "http" || scheme == "https"
