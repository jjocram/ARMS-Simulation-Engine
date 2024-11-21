package metrics

class ResourceMetric {
    var counter = 0
    var min = 0
    var max = 0

    fun add(quantity: Int = 1) {
        counter += quantity
        if (counter > max) max = counter
    }

    fun take(quantity: Int = 1) {
        counter -= quantity
        if (counter < min) min = counter
    }
}