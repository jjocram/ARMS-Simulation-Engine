import kotlinx.datetime.Instant
import org.json.JSONException
import org.json.JSONObject
import org.kalasim.*
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import kotlin.system.exitProcess

fun Environment.totalTime(): Long {
    return (now - startDate).inWholeSeconds / 60
}

fun main(args: Array<String>) {
    val processFile = File(args[0])
    if (!processFile.exists()) {
        error("File: ${args[0]}} not found")
        exitProcess(1)
    }

    val startFromEpoch: Long = 1727769600

    createSimulation(startDate = Instant.fromEpochSeconds(startFromEpoch)) {
        enableComponentLogger()
        val process = Process(processFile)

        object : Component("Watcher") {
            override fun repeatedProcess(): Sequence<Component> = sequence {
                if (process.places.getValue("end").count() == process.totalProductRequest && process.places.getValue("end_product").count() == process.totalProductRequest) {
                    println("There are ${process.places.getValue("end").count()} tokens in the last place")

                    val json = JSONObject()

                    try {
                        json.put("simulation", mapOf<String, Any>(
                            "totalTime" to env.totalTime()
                        ))

                        json.put("executors", process.executors.map { (_, executor) ->
                            mapOf<String, Any>(
                                "id" to executor.id,
                                "maxWaitTimeInQueue" to executor.jobsInQueueMetrics.max,
                                "busy" to executor.totalBusyTime,
                                "idle" to executor.totalIdleTime
                            )
                        })
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }


                    try {
                        PrintWriter(FileWriter("example.json")).use { it.write(json.toString()) }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }


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