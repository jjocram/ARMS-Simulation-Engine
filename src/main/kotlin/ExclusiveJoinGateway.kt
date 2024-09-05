import org.kalasim.Component

class ExclusiveJoinGateway(override val id: String, override var nextElements: List<BPMNElement>) : BPMNElement {
    override val activationTokens: MutableList<ActivationToken> = mutableListOf()
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is ExclusiveJoinGateway && id == other.id
}

class ExclusiveJoinGatewayExecutor(val exclusiveJoinGateway: ExclusiveJoinGateway): Executor(exclusiveJoinGateway.id) {

    override fun process(): Sequence<Component> = sequence {
        while (true) {
            exclusiveJoinGateway.nextElements.forEach {
                it.activationTokens.addAll(exclusiveJoinGateway.activationTokens)
            }

            exclusiveJoinGateway.activationTokens.clear()

            standby()
        }
    }
}