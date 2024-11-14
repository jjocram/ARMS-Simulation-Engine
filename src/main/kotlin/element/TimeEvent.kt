package element

import org.kalasim.Component
import place.Place
import transition.Transition
import kotlin.time.Duration

class TimeEvent(
    id: String,
    inputControl: Place,
    outputControl: Place,
    inputProduct: Place,
    outputProduct: Place,
    val duration: Duration
) : BPMNElement(id, null) {
    private val transition = Transition(id) {
        val controlInputIds = it.getPlace("inputControl").tokens.map { it.ids }.toSet()
        val productInputIds = it.getPlace("inputProduct").tokens.map { it.ids }.toSet()
        return@Transition controlInputIds.intersect(productInputIds).firstOrNull()
    }

    init {
        transition.addPlace("inputControl", inputControl)
        transition.addPlace("outputControl", outputControl)
        transition.addPlace("inputProduct", inputProduct)
        transition.addPlace("outputProduct", outputProduct)
    }

    override fun repeatedProcess(): Sequence<Component> = sequence {
        // Wait for the event to happen
        hold(duration)

        // Take all fireable tokens
        var tokenId = transition.fireableTokenId
        while (tokenId != null) {
            // Move control token
            val controlToken = transition.getPlace("inputControl").take(tokenId)
            transition.getPlace("outputControl").add(controlToken)

            // Move product token
            val productToken = transition.getPlace("inputProduct").take(tokenId)
            transition.getPlace("outputProduct").add(productToken)

            // Update tokenId
            tokenId = transition.fireableTokenId
        }
    }
}