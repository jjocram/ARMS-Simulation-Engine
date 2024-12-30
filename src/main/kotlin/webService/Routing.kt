package webService

import Process
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.http.content.file
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.datetime.Instant
import metrics.Metric
import metrics.metricsByActivity
import metrics.processedItems
import metrics.totalBusyTime
import metrics.totalIdleTime
import metrics.waitTimeInQueue
import org.json.JSONException
import org.json.JSONObject
import org.kalasim.Component
import org.kalasim.createSimulation
import org.kalasim.enableComponentLogger
import totalTime
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import kotlin.collections.component1
import kotlin.collections.component2

fun Application.configureRouting() {
    routing {
        post("/simulate") {
            val processFile = File("process.bpmn")
            call.receiveChannel().copyAndClose(processFile.writeChannel())

            val startFromEpoch: Long = 1727769600

            var result: MetricResult? = null
            createSimulation(startDate = Instant.fromEpochSeconds(startFromEpoch)) {
                //enableComponentLogger()
                val process = Process(processFile)

                object : Component("Watcher") {
                    override fun repeatedProcess(): Sequence<Component> = sequence {
                        if (process.places.getValue("end")
                                .count() == process.totalProductRequest && process.places.getValue("end_product")
                                .count() == process.totalProductRequest
                        ) {
                            println("There are ${process.places.getValue("end").count()} tokens in the last place")

                            result = MetricResult(
                                simulation = SimulationResult(env.totalTime()),
                                executors = process.executors.values.flatten().map { executor ->
                                    ExecutorResult(
                                        id = executor.id,
                                        maxWaitTimeInQueue = executor.waitTimeInQueue.getValue(Metric.MAX) as Long,
                                        avgWaitTimeInQueue = executor.waitTimeInQueue.getValue(Metric.MEAN) as Double,
                                        sumWaitTimeInQueue = executor.metricsByActivity
                                            .map { it.value.getValue("queue").sum }
                                            .sum(),
                                        busy = executor.totalBusyTime,
                                        idle = executor.totalIdleTime,
                                        processedItems = executor.processedItems,
                                        activities = executor.metricsByActivity.map {
                                            ExecutorActivityResult(
                                                id = it.key,
                                                maxWaitTimeInQueue = it.value.getValue("queue").max,
                                                avgWaitTimeInQueue = it.value.getValue("queue").mean,
                                                sumWaitTimeInQueue = it.value.getValue("queue").sum,
                                                busy = it.value.getValue("busy").sum,
                                                processedItems = it.value.getValue("queue").count,
                                            )
                                        }
                                    )
                                }
                            )
                            stopSimulation()
                        }

                        standby()
                    }
                }

            }.run()

            if (result != null) {
                call.respond(result)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}