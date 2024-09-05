import java.util.*

class ActivationToken(val id: UUID = UUID.randomUUID(), val productFamily: ProductFamily)

sealed interface BPMNElement {
    val id: String
    var nextElements: List<BPMNElement>
    val activationTokens: MutableList<ActivationToken>

    override fun hashCode(): Int
    override fun equals(other: Any?): Boolean
}