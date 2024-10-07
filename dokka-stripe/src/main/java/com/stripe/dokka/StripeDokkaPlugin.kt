package com.stripe.dokka

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DParameter
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.DTypeAlias
import org.jetbrains.dokka.model.DTypeParameter
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

class StripeDokkaPlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }

    @Suppress("unused") // This delegated property has a desired side effect.
    val filterInternalApis by extending {
        dokkaBase.preMergeDocumentableTransformer providing ::FilterInternalApis
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement {
        return PluginApiPreviewAcknowledgement
    }
}

private class FilterInternalApis(context: DokkaContext) : SuppressedByConditionDocumentableFilterTransformer(context) {
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        return when (d) {
            is DTypeAlias -> d.isInternalSdk()
            is DPackage -> d.isInternalSdk()
            is DEnumEntry -> d.isInternalSdk()
            is DModule -> d.isInternalSdk()
            is DTypeParameter -> d.isInternalSdk()
            is DParameter -> d.isInternalSdk()
            is DFunction -> d.isInternalSdk()
            is DClass -> d.isInternalSdk()
            is DObject -> d.isInternalSdk()
            is DAnnotation -> d.isInternalSdk()
            is DEnum -> d.isInternalSdk()
            is DInterface -> d.isInternalSdk()
            is DProperty -> d.isInternalSdk()
            else -> false
        }
    }
}

private fun <T> T.isInternalSdk() where T : WithExtraProperties<out Documentable> =
    internalAnnotation != null

@Suppress("TYPE_MISMATCH_WARNING_FOR_INCORRECT_CAPTURE_APPROXIMATION")
private val <T> T.internalAnnotation where T : WithExtraProperties<out Documentable>
    get() = extra[Annotations]?.let { annotations ->
        annotations.directAnnotations.values.flatten().firstOrNull {
            it.dri.toString() == "androidx.annotation/RestrictTo///PointingToDeclaration/"
        }
    }
