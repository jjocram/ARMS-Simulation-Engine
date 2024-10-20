package token

class ProductToken: Token() {
    private val productProperties: MutableMap<String, String> = mutableMapOf()

    fun addProperty(name: String, value: String) {
        require(name !in productProperties) {"${name} is already defined in ProductToken"}
        productProperties[name] = value
    }

    fun setProperty(name: String, value: String) {
        require(name in productProperties) {"${name} is not present in ProductToken"}
        productProperties[name] = value
    }

    fun getProperty(name: String): String {
        require(name in productProperties) {"${name} is not present in ProductToken"}
        return productProperties.getValue(name)
    }
}