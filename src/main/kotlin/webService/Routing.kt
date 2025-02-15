package webService

import Process
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.http.content.file
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.read
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    routing {
        post("/simulate") {
            val processFile = File("process.bpmn")

            val multipartData = call.receiveMultipart()
            multipartData.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        part.provider().copyAndClose(processFile.writeChannel())
                    }

                    else -> {
                        println("It is not something we are interested in right now")
                    }
                }
                part.dispose()
            }

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
                                        },
                                        avgQueueLength = executor.queueLengthMetric.mean,
                                        varQueueLength = executor.queueLengthMetric.variance,
                                        stdQueueLength = executor.queueLengthMetric.std,
                                        maxQueueLength = executor.queueLengthMetric.max
                                    )
                                }
                            )
                            stopSimulation()
                        }

                        hold(5.seconds)
                    }
                }

            }.run()


            call.response.headers.append("Access-Control-Allow-Origin", "*")
            if (result != null) {
                println("Result is $result")
                call.respond(result)
            } else {
                println("Result is null")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}