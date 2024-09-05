import org.kalasim.Component

data class ProductFamilyRequest(val productFamily: ProductFamily, val quantity: Int)

class StartEvent(
    override val id: String,
    override var nextElements: List<BPMNElement>,
    val requests: List<ProductFamilyRequest>
) : BPMNElement {
    override val activationTokens: MutableList<ActivationToken> = mutableListOf()

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is StartEvent && id == other.id
}

class StartEventExecutor(private val startEvent: StartEvent) : Executor(startEvent.id) {
    override fun process(): Sequence<Component> = sequence {
        val activationTokens = startEvent.requests
            .map { request -> List(request.quantity) { ActivationToken(productFamily = request.productFamily) } }
            .flatten()

        startEvent.nextElements.forEach { element ->
            element.activationTokens.addAll(activationTokens)
        }

        passivate()
    }
}