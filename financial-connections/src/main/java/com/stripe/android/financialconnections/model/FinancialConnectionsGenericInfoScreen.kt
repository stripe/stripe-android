
import android.os.Parcelable
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.serializer.BodyEntrySerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        val subtitle: String? = null,
        val icon: Image? = null,
        val alignment: Alignment? = null
    ) : Parcelable

    @Parcelize
    @Serializable
    internal data class Body(
        val entries: List<BodyEntry>
    ) : Parcelable {

        @Serializable(with = BodyEntrySerializer::class)
        sealed class BodyEntry : Parcelable {
            abstract val id: String

            @Parcelize
            @Serializable
            internal data class BodyText(
                override val id: String,
                val text: String,
                val alignment: Alignment? = null,
                val size: Size? = null
            ) : BodyEntry()

            @Parcelize
            @Serializable
            internal data class BodyImage(
                override val id: String,
                val image: Image,
                val alt: String
            ) : BodyEntry()

            @Parcelize
            @Serializable
            internal data class BodyBullets(
                override val id: String,
                val bullets: List<GenericBulletPoint>
            ) : BodyEntry() {
                @Parcelize
                @Serializable
                internal data class GenericBulletPoint(
                    val id: String,
                    val icon: Image? = null,
                    val title: String? = null,
                    val content: String? = null
                ) : Parcelable
            }

            // TODO@carlosmuvi: Add missing body items: prepane, forms.
        }
    }

    @Parcelize
    @Serializable
    internal data class Footer(
        val disclaimer: String? = null,
        @SerialName("primary_cta")
        val primaryCta: GenericInfoAction? = null,
        @SerialName("secondary_cta")
        val secondaryCta: GenericInfoAction? = null,
        @SerialName("below_cta")
        val belowCta: GenericInfoAction? = null
    ) : Parcelable {
        @Parcelize
        @Serializable
        internal data class GenericInfoAction(
            val id: String,
            val label: String,
            val icon: Image? = null,
            val action: String? = null,
            @SerialName("test_id")
            val testId: String? = null
        ) : Parcelable
    }

    @Parcelize
    @Serializable
    internal data class Options(
        @SerialName("full_width_content")
        val fullWidthContent: Boolean? = null,
        @SerialName("vertical_alignment")
        val verticalAlignment: VerticalAlignment? = null
    ) : Parcelable {

    }
}

@Serializable
internal enum class Alignment {
    @SerialName("left") LEFT,
    @SerialName("center") CENTER,
    @SerialName("right") RIGHT
}

@Serializable
internal enum class VerticalAlignment {
    @SerialName("default") DEFAULT,
    @SerialName("centered") CENTERED
}

@Serializable
internal enum class Size {
    @SerialName("x-small") X_SMALL,
    @SerialName("small") SMALL,
    @SerialName("medium") MEDIUM
}

