package com.stripe.android.stripecardscan.framework.exception

import java.lang.Exception

internal class ImageTypeNotSupportedException(val imageType: Int) : Exception()
