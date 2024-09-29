import org.kalasim.Component

class ParallelForkGateway(override val id: String, override var nextElements: List<BPMNElement>) : BPMNElement {
    override val activationTokens: MutableList<ActivationToken> = mutableListOf()
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is ParallelJoinGateway && id == other.id
}

class ParallelForkGatewayExecutor(private val parallelForkGateway: ParallelForkGateway): Executor(parallelForkGateway.id) {
    override fun process(): Sequence<Component> = sequence {
        while (true) {
            parallelForkGateway.nextElements.forEach { it.activationTokens.addAll(parallelForkGateway.activationTokens) }

            parallelForkGateway.activationTokens.clear()

            wakeUpNextElementsOf(parallelForkGateway)

            passivate()
        }
    }
}