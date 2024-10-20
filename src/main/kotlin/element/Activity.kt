package element

import org.kalasim.Component
import place.Place
import transition.Transition
import kotlin.time.Duration.Companion.seconds


class Activity(id: String, value: String, inputControl: Place, outputControl: Place, inputProduct: Place, outputProduct: Place) : BPMNElement(id, value) {
    private val transition = Transition(value) {
        for (controlInputToken in it.connections[0].input.tokens) {
            for (productInputToken in it.connections[1].input.tokens) {
                if (controlInputToken.ids == productInputToken.ids) {
                    return@Transition true
                }
            }
        }

        return@Transition false
    }

    init {
        transition.setControlPlaces(inputControl, outputControl)
        transition.setProductPlaces(inputProduct, outputProduct)
    }

    override fun repeatedProcess(): Sequence<Component> = sequence {
        if (transition.canFire) {
            val retainedTokens = transition.takeInputTransientTokens()
            hold(10.seconds)
            transition.moveTransientTokens(retainedTokens)
        } else {
            standby()
        }
    }
}