
import android.os.Parcelable
import com.stripe.android.financialconnections.model.serializer.BodyEntrySerializer
import com.stripe.android.financialconnections.model.serializer.MarkdownToHtmlSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.stripe.android.financialconnections.model.Image as ImageResponse

@Parcelize
@Serializable
internal data class FinancialConnectionsGenericInfoScreen(
    val id: String,
    val header: Header? = null,
    val body: Body? = null,
    val footer: Footer? = null,
    val options: Options? = null
) : Parcelable {

    @Parcelize
    @Serializable
    internal data class Header(
        val title: String? = null,
        @Serializable(with = MarkdownToHtmlSerializer::class)
        val subtitle: String? = null,
        val icon: ImageResponse? = null,
        val alignment: Alignment? = null
    ) : Parcelable

    @Parcelize
    @Serializable
    internal data class Body(
        val entries: List<Entry>
    ) : Parcelable {

        @Serializable(with = BodyEntrySerializer::class)
        sealed class Entry : Parcelable {
            abstract val id: String

            @Parcelize
            @Serializable
            internal data class Text(
                override val id: String,
                val text: String,
                val alignment: Alignment? = null,
                val size: Size? = null
            ) : Entry()

            @Parcelize
            @Serializable
            internal data class Image(
                override val id: String,
                val image: ImageResponse,
                val alt: String
            ) : Entry()

            @Parcelize
            @Serializable
            internal data class Bullets(
                override val id: String,
                val bullets: List<GenericBulletPoint>
            ) : Entry() {
                @Parcelize
                @Serializable
                internal data class GenericBulletPoint(
                    val id: String,
                    val icon: ImageResponse? = null,
                    val title: String? = null,
                    @Serializable(with = MarkdownToHtmlSerializer::class)
                    val content: String? = null,
                ) : Parcelable
            }

            @Serializable
            @Parcelize
            internal data class Unknown(
                override val id: String
            ) : Entry()

            // TODO@carlosmuvi: Add missing body items: prepane, forms.
        }
    }

    @Parcelize
    @Serializable
    internal data class Footer(
        @Serializable(with = MarkdownToHtmlSerializer::class)
        val disclaimer: String? = null,
        @SerialName("primary_cta")
        val primaryCta: GenericInfoAction? = null,
        @SerialName("secondary_cta")
        val secondaryCta: GenericInfoAction? = null,
        @SerialName("below_cta")
        @Serializable(with = MarkdownToHtmlSerializer::class)
        val belowCta: String? = null,
        // TODO: Confirm this is an ok change to make
    ) : Parcelable {
        @Parcelize
        @Serializable
        internal data class GenericInfoAction(
            val id: String,
            val label: String,
            val icon: ImageResponse? = null,
        ) : Parcelable
    }

    @Parcelize
    @Serializable
    internal data class Options(
        @SerialName("full_width_content")
        val fullWidthContent: Boolean? = null,
        @SerialName("vertical_alignment")
        val verticalAlignment: VerticalAlignment? = null
    ) : Parcelable
}

@Serializable
internal enum class Alignment {
    @SerialName("left")
    Left,

    @SerialName("center")
    Center,

    @SerialName("right")
    Right
}

@Serializable
internal enum class VerticalAlignment {
    @SerialName("default")
    Default,

    @SerialName("centered")
    Centered
}

@Serializable
internal enum class Size {
    @SerialName("x-small")
    XSmall,

    @SerialName("small")
    Small,

    @SerialName("medium")
    Medium
}
