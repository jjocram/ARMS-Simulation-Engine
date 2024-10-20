import element.Activity
import element.EndEvent
import element.StartEvent
import org.kalasim.*
import place.Place
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val processFile = File(args[0])
    if (!processFile.exists()) {
        error("File: ${args[0]}} not found")
        exitProcess(1)
    }

    createSimulation {
        enableComponentLogger()
        val p1 = Place()
        val p2 = Place()
        val p3 = Place()
        val p4 = Place()
        val p1Product = Place()
        val p2Product = Place()
        val p3Product = Place()
        val p4Product = Place()

        StartEvent("1", "Start", p1, p2, p1Product, p2Product, 10)
        Activity("2", "A", p2, p3, p2Product, p3Product)
        EndEvent("3", "End", p3, p4, p3Product, p4Product)

        object : Component("Watcher") {
            override fun repeatedProcess(): Sequence<Component> = sequence {
                if (p4.tokens.count() == 10) {
                    stopSimulation()
                }

                standby()
            }
        }

    }.run()
}