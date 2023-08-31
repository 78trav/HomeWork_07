package otus.homework.customview

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Date


@Serializable
data class Payment (
    val id: Int,
    val name: String,
    val amount: Int,
    val category: String,
    @Serializable(with = DateAsLongSerializer::class)
    val time: Date
)

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }

    override fun deserialize(decoder: Decoder) = Date(decoder.decodeLong())
}

@Serializable
data class Category (
    val name: String,
    val amount: Int
)