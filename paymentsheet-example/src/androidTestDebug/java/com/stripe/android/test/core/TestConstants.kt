package com.stripe.android.test.core

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

const val INDIVIDUAL_TEST_TIMEOUT_SECONDS = 90L
const val HOOKS_PAGE_LOAD_TIMEOUT = 60L
const val TEST_IBAN_NUMBER = "DE89370400440532013000"

val testArtifactDirectoryOnDevice by lazy {
    val pattern = "yyyy-MM-dd-HH-mm"
    val simpleDateFormat = SimpleDateFormat(pattern)
    val date = simpleDateFormat.format(Date())
    File(
        // Path is /data/user/0/com.stripe.android.paymentsheet.example/files/
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
        "$date/"
    )
}
