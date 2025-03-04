package com.stripe.form.testutils

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.stripe.form.ContentSpec

/**
 * A Truth Subject for testing ContentSpec implementations.
 */
class ContentSpecSubject private constructor(
    metadata: FailureMetadata,
    private val actual: ContentSpec?
) : Subject(metadata, actual) {

    /**
     * Asserts that the ContentSpec is of the specified type T and allows testing its properties
     * via the provided lambda.
     */
    fun <T : ContentSpec> isSpecOfType(
        klass: Class<T>,
    ) {
        isNotNull()
        check("is instance of ${klass.simpleName}").that(actual).isInstanceOf(klass)
    }

    companion object {
        /**
         * Returns a Subject factory for ContentSpecSubject instances.
         */
        private fun specs() = Subject.Factory<ContentSpecSubject, ContentSpec> { metadata, actual ->
            ContentSpecSubject(metadata, actual)
        }

        /**
         * Static assertion method for creating ContentSpecSubject instances.
         */
        @JvmStatic
        fun assertThat(actual: ContentSpec?) = assertAbout(specs()).that(actual)
    }
}

inline fun <reified T : ContentSpec> ContentSpec.isSpecOfType(
    crossinline assertions: (T) -> Unit = {}
) {
    ContentSpecSubject.assertThat(this)
        .isSpecOfType(
            klass = this::class.java,
        )
    assertions(this as T)
}
