package element

import org.kalasim.Component

sealed class BPMNElement(id: String, val value: String?) : Component(value ?: id) {

}