import org.kalasim.*
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
        val process = Process(processFile)
        process.executors
        process.bpmnElements

        object : Component("Watcher") {
            override fun repeatedProcess(): Sequence<Component> = sequence {
                if (process.places.getValue("end").count() == 10 && process.places.getValue("end_product").count() == 10) {
                    println("There are 10 token in the last place")
                    stopSimulation()
                }

                standby()
            }
        }

    }.run()
}