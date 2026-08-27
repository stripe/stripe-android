package com.stripe.android.gradle

import com.android.tools.firebase.testlab.gradle.TestLabGradlePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Repository-local entry point for the Firebase Test Lab compatibility plugin. */
class FirebaseTestLabPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        TestLabGradlePlugin().apply(target)
    }
}
