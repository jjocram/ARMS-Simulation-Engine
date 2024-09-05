import org.kalasim.Component

class Condition(val toElement: BPMNElement, val defaultRoute: Boolean, val condition: (() -> Boolean)?)

class ExclusiveForkGateway(
    override val id: String,
    override var nextElements: List<BPMNElement>,
    var conditions: List<Condition>
) : BPMNElement {
    override val activationTokens: MutableList<ActivationToken> = mutableListOf()
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is ExclusiveForkGateway && id == other.id
}

class ExclusiveForkGatewayExecutor(private val exclusiveForkGateway: ExclusiveForkGateway) :
    Executor(exclusiveForkGateway.id) {
    override fun process(): Sequence<Component> = sequence {
        while (true) {
            val defaultCondition = exclusiveForkGateway.conditions.first { it.defaultRoute }

            for (token in exclusiveForkGateway.activationTokens) {
                val trueCondition =
                    exclusiveForkGateway.conditions.firstOrNull { it.condition?.invoke() == true } ?: defaultCondition
                trueCondition.toElement.activationTokens.add(token)
            }

            exclusiveForkGateway.activationTokens.clear()

            standby()
        }
    }
}