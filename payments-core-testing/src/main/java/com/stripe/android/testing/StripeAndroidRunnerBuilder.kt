@file:SuppressLint("RestrictedApi")

package com.stripe.android.testing

import android.annotation.SuppressLint
import androidx.test.internal.runner.junit3.AndroidJUnit3Builder
import androidx.test.internal.runner.junit3.AndroidSuiteBuilder
import androidx.test.internal.runner.junit4.AndroidAnnotatedBuilder
import androidx.test.internal.runner.junit4.AndroidJUnit4Builder
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder
import org.junit.internal.builders.AnnotatedBuilder
import org.junit.internal.builders.IgnoredBuilder
import org.junit.internal.builders.JUnit3Builder
import org.junit.internal.builders.JUnit4Builder
import org.junit.runners.model.RunnerBuilder

/**
 * Builds Android-aware JUnit runners without applying retry logic.
 */
internal class StripeAndroidRunnerBuilder(
    ignoreSuiteMethods: Boolean = false,
    perTestTimeout: Long = 0,
) : AllDefaultPossibilitiesBuilder() {
    private val androidJUnit3Builder = AndroidJUnit3Builder(perTestTimeout)
    private val androidJUnit4Builder = AndroidJUnit4Builder(perTestTimeout)
    private val androidSuiteBuilder = AndroidSuiteBuilder(ignoreSuiteMethods, perTestTimeout)
    private val androidAnnotatedBuilder = AndroidAnnotatedBuilder(this, perTestTimeout)
    private val ignoredBuilder = IgnoredBuilder()

    override fun junit4Builder(): JUnit4Builder = androidJUnit4Builder

    override fun junit3Builder(): JUnit3Builder = androidJUnit3Builder

    override fun annotatedBuilder(): AnnotatedBuilder = androidAnnotatedBuilder

    override fun ignoredBuilder(): IgnoredBuilder = ignoredBuilder

    override fun suiteMethodBuilder(): RunnerBuilder = androidSuiteBuilder
}
