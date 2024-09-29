class TransformationMap {
    data class TransformationIO(val productFamily: ProductFamily, val quantity: Double = 1.0)
    data class Transformation(val inputs: List<TransformationIO>, val outputs: List<TransformationIO>) {
        fun isDoable(inventories: Inventories): Boolean {
            return inputs.all { inventories.get(it.productFamily).level >= it.quantity }
        }
    }

    val transformationMap = mutableMapOf<ProductFamily, MutableList<Transformation>>()

    fun add(productFamily: ProductFamily, transformation: Transformation) {
        transformationMap.getOrPut(productFamily, {mutableListOf<Transformation>()}).add(transformation)
    }

    fun get(productFamily: ProductFamily): List<Transformation>? {
        return transformationMap[productFamily]?.toList()
    }

}