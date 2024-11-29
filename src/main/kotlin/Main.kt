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
                if (process.places.getValue("end").count() == process.totalProductRequest && process.places.getValue("end_product").count() == process.totalProductRequest) {
                    println("There are 100 token in the last place")
                    process.executors.forEach { _, executor ->
                        println("""Id: ${executor.id} (${executor.name})
                            |   maxWaitTimeInQueue: ${executor.jobsInQueueMetrics.max}
                            |   busy: ${executor.stateTimeline.summed().getValue(ComponentState.SCHEDULED)}
                            |   idle: ${executor.stateTimeline.summed().getValue(ComponentState.PASSIVE)}""".trimMargin())
                    }
                    stopSimulation()
                }

                standby()
            }
        }

    }.run()
}