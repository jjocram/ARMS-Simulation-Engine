package token

class ProductToken(val productRequestId: String): Token() {
    val productProperties: MutableMap<String, String> = mutableMapOf()

    fun setProperty(name: String, value: String) {
        productProperties.set(name, value)
    }

    fun getProperty(name: String): String {
        require(name in productProperties) {"${name} is not present in ProductToken"}
        return productProperties.getValue(name)
    }

    fun apply(transformation: Map<String, String>) {
        transformation.forEach { setProperty(it.key, it.value) }
    }
}