import org.kalasim.Component
import kotlin.time.Duration

class TimeEvent(override val id: String, override var nextElements: List<BPMNElement>, val holdDuration: Duration) : BPMNElement {
    override val activationTokens: MutableList<ActivationToken> = mutableListOf()
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is TimeEvent && id == other.id
}

class TimeEventExecutor(private val timeEvent: TimeEvent): Executor(timeEvent.id) {
    override fun process(): Sequence<Component> = sequence {
        while(true) {
            hold(timeEvent.holdDuration)

            timeEvent.nextElements.forEach { it.activationTokens.addAll(timeEvent.activationTokens) }

            timeEvent.activationTokens.clear()

            wakeUpNextElementsOf(timeEvent)
         }
    }
}