package com.stripe.form.testutils

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.stripe.form.ContentSpec
import com.stripe.form.Form

/**
 * A Truth Subject for testing Form objects.
 */
class FormSubject private constructor(
    metadata: FailureMetadata,
    private val actual: Form?
) : Subject(metadata, actual) {

    fun hasContentSize(expected: Int) {
        isNotNull()
        actual ?: return
        check("content size").that(actual.content.size).isEqualTo(expected)
    }

    fun contentAt(index: Int, assertions: (ContentSpec) -> Unit) {
        isNotNull()
        actual ?: return
        check("has content at index $index").that(actual.content.size).isGreaterThan(index)
        assertions(actual.content[index])
    }

    fun containsExactly(vararg specs: ContentSpecAssertion) {
        isNotNull()
        actual ?: return

        check("content size").that(actual.content.size).isEqualTo(specs.size)

        specs.forEachIndexed { index, spec ->
            check("content at index $index").that(actual.content[index])
                .isInstanceOf(spec.type)

            spec.assertions?.invoke(actual.content[index])
        }
    }

    fun contains(vararg specs: ContentSpecAssertion) {
        isNotNull()
        actual ?: return

        specs.forEach { spec ->
            val matchingContent = actual.content.find { spec.type.isInstance(it) }
            check("contains ${spec.type.simpleName}")
                .that(matchingContent)
                .isNotNull()

            matchingContent?.let { spec.assertions?.invoke(it) }
        }
    }

    companion object {
        private fun forms() = Subject.Factory<FormSubject, Form> { metadata, actual ->
            FormSubject(metadata, actual)
        }

        @JvmStatic
        fun assertThat(actual: Form?) = assertAbout(forms()).that(actual)
    }
}

class ContentSpecAssertion(
    val type: Class<out ContentSpec>,
    val assertions: ((ContentSpec) -> Unit)? = null
)

// Extension function for more fluent testing
fun Form.assertThat(assertions: FormSubject.() -> Unit) {
    FormSubject.assertThat(this).apply(assertions)
}

fun Form.hasContentSize(expected: Int) {
    assertThat { hasContentSize(expected) }
}

inline fun <reified T : ContentSpec> Form.contentAt(
    index: Int,
    crossinline assertions: (T) -> Unit
) {
    assertThat {
        contentAt(index) { contentSpec ->
            contentSpec.isSpecOfType<T>(assertions)
        }
    }
}

// Extension functions
fun Form.containsExactly(vararg specs: ContentSpecAssertion) {
    assertThat { containsExactly(*specs) }
}

fun Form.contains(vararg specs: ContentSpecAssertion) {
    assertThat { contains(*specs) }
}

// Helper functions
inline fun <reified T : ContentSpec> contentOf(
    noinline assertions: ((T) -> Unit)? = null
): ContentSpecAssertion {
    return ContentSpecAssertion(
        type = T::class.java,
        assertions = assertions?.let { block ->
            { spec -> block(spec as T) }
        }
    )
}
