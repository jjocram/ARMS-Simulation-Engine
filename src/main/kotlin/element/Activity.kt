package element

import org.kalasim.Component
import place.Place
import transition.Transition
import kotlin.time.Duration.Companion.seconds


class Activity(id: String, value: String, inputControl: Place, outputControl: Place, inputProduct: Place, outputProduct: Place) : BPMNElement(id, value) {
    private val transition = Transition(value) {
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
        val tokenId = transition.fireableTokenId
        if (tokenId != null) { // There is at least one token with the same id in both controlInputPlace and productInputPlace
            // Take tokens from inputs
            val controlToken = transition.getPlace("inputControl").take(tokenId)
            val productToken = transition.getPlace("inputProduct").take(tokenId)

            // Hold time
            hold(10.seconds)
            log("Done $value")

            // Add token to outputs
            transition.getPlace("outputControl").add(controlToken)
            transition.getPlace("outputProduct").add(productToken)
        } else {
            standby()
        }
    }
}