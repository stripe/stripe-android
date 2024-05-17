package com.stripe.baselineprofile

import android.content.Intent
import android.net.Uri

internal fun financialConnectionsPlaygroundIntent(): Intent {
    val intent = Intent(Intent.ACTION_VIEW).also {

        it.data = Uri.parse(
            "financial-connections://macrobenchmark/run"
        )
    }
    return intent
}