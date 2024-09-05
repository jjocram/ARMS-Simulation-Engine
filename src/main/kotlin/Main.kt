import org.kalasim.Component
import org.kalasim.createSimulation
import org.kalasim.dependency
import org.kalasim.enableComponentLogger

fun main() {
    createSimulation {
        enableComponentLogger()

        val compatibilityMap = CompatibilityMap()
        val inventories = Inventories()

        dependency { compatibilityMap }
        dependency { inventories }

        object : Component("Watcher") {
            override fun repeatedProcess(): Sequence<Component> = sequence {
                // log("P1: ${inventories.get(p1).level}")
                // log("P2: ${inventories.get(p2).level}")

                standby()
            }
        }
    }.run()
}