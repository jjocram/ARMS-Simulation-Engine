class TransformationMap {
    data class TransformationIO(val productFamily: ProductFamily, val quantity: Double = 1.0)
    data class Transformation(val inputs: List<TransformationIO>, val outputs: List<TransformationIO>)

    val transformationMap = mutableMapOf<ProductFamily, Transformation>()

    fun add(productFamily: ProductFamily, transformation: Transformation) {
        transformationMap[productFamily] = transformation
    }

    fun get(productFamily: ProductFamily): Transformation {
        require(productFamily in transformationMap) {"ProductFamily $productFamily not in map"}
        return transformationMap[productFamily]!!
    }
}