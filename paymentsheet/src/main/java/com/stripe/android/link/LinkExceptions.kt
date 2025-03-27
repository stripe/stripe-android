package com.stripe.android.link

internal class NoLinkAccountFoundException : IllegalStateException("No link account found")

internal class LinkSignupDisabledException : IllegalStateException("Link signup is disabled")
