import org.kalasim.DepletableResource

class Inventories {
    private val inventories: MutableMap<ProductFamily, DepletableResource> = mutableMapOf()

    fun add(productFamily: ProductFamily, initialQuantity: Double = 0.0) {
        inventories[productFamily] = DepletableResource(productFamily.id, capacity = Int.MAX_VALUE, initialLevel = initialQuantity)
    }

    fun get(productFamily: ProductFamily): DepletableResource {
        require(productFamily in inventories) {"ProductFamily $productFamily not in inventories"}
        return inventories[productFamily]!!
    }
}