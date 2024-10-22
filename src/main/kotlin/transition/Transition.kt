package transition

import place.Place
import java.util.UUID

class Transition(val name: String, val canFirePredicate: (Transition) -> List<UUID>?) {
    val places = mutableMapOf<String, Place>()
    fun addPlace(key: String, place: Place) {
        require( key !in places ) {"Place with key $key is already in use in transition $name"}
        places.set(key, place)
    }

    fun getPlace(key: String): Place {
        require(key in places.keys) { "Place ($key) not found in transition $name" }
        return places.getValue(key)
    }

    val fireableTokenId: List<UUID>? get() = canFirePredicate(this)
}