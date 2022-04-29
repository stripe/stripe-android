package com.stripe.android.financialconnections.test

import java.io.BufferedReader

/**
 * Reads a file located on the given [resourcePath] and parses it as a single string.
 *
 * Mostly used to parse json files on repository tests.
 */
fun Any.readResourceAsString(resourcePath: String): String = javaClass
    .classLoader!!
    .getResourceAsStream(resourcePath)!!
    .bufferedReader()
    .use(BufferedReader::readText)
