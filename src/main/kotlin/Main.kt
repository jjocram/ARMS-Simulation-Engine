import kotlinx.datetime.Instant
import metrics.Metric
import metrics.metricsByActivity
import metrics.processedItems
import metrics.totalBusyTime
import metrics.totalIdleTime
import metrics.waitTimeInQueue
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

        dependency { process.executors }

        object : Component("Watcher") {
            override fun repeatedProcess(): Sequence<Component> = sequence {
                if (process.places.getValue("end")
                        .count() == process.totalProductRequest && process.places.getValue("end_product")
                        .count() == process.totalProductRequest
                ) {
                    println("There are ${process.places.getValue("end").count()} tokens in the last place")

                    val json = JSONObject()

                    try {
                        json.put(
                            "simulation", mapOf<String, Any>(
                                "totalTime" to env.totalTime()
                            )
                        )

                        json.put("executors", process.executors.values.flatten().map { executor ->
                            mapOf<String, Any>(
                                "id" to executor.id,
                                "maxWaitTimeInQueue" to executor.waitTimeInQueue.getValue(Metric.MAX),
                                "avgWaitTimeInQueue" to executor.waitTimeInQueue.getValue(Metric.MEAN),
                                "sumWaitTimeInQueue" to executor.metricsByActivity.map { it.value.getValue("queue").sum }
                                    .sum(),
                                "busy" to executor.totalBusyTime,
                                "idle" to executor.totalIdleTime,
                                "processedItems" to executor.processedItems,
                                "activities" to executor.metricsByActivity.map {
                                    mapOf<String, Any>(
                                        "id" to it.key,
                                        "maxWaitTimeInQueue" to it.value.getValue("queue").max,
                                        "avgWaitTimeInQueue" to it.value.getValue("queue").mean,
                                        "sumWaitTimeInQueue" to it.value.getValue("queue").sum,
                                        "busy" to it.value.getValue("busy").sum,
                                        "processedItems" to it.value.getValue("queue").count,
                                    )
                                }
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

                    stopSimulation()
                }

                standby()
            }
        }

    }.run()
}