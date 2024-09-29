import org.kalasim.Resource

data class Accessory(val id: String, val name: String, val quantity: Double)

class Accessories {
    private val accessories: MutableMap<Accessory, Resource> = mutableMapOf()

    fun add(accessory: Accessory) {
        accessories[accessory] = Resource(accessory.name, accessory.quantity)
    }

    fun get(accessory: Accessory): Resource {
        require(accessory in accessories) {".Accessory $accessory not in map of accessories"}
        return accessories[accessory]!!
    }
}