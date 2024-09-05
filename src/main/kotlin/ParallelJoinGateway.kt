import org.kalasim.Component

class ParallelJoinGateway(override val id: String, override var nextElements: List<BPMNElement>, val incomingConnections: Int) : BPMNElement {
    override val activationTokens: MutableList<ActivationToken> = mutableListOf()
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is ParallelJoinGateway && id == other.id
}

class ParallelJoinGatewayExecutor(private val parallelJoinGateway: ParallelJoinGateway): Executor(parallelJoinGateway.id) {
    override fun process(): Sequence<Component> = sequence {
        val tokensWithEnoughPresence = parallelJoinGateway.activationTokens
            .groupingBy { it }
            .eachCount()
            .filter { it.value == parallelJoinGateway.incomingConnections }
            .map { it.key }

        parallelJoinGateway.nextElements.forEach { it.activationTokens.addAll(tokensWithEnoughPresence) }

        parallelJoinGateway.activationTokens.removeAll(tokensWithEnoughPresence)

        standby()
    }
}