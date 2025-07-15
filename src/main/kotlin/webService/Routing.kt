package webService

import Process
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.*
import io.ktor.server.request.receiveMultipart
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
import org.kalasim.Component
import org.kalasim.createSimulation
import totalTime
import java.io.File
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    routing {
        post("/simulate") {
            call.application.environment.log.info("Simulation requested")
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
            call.application.environment.log.info("File received")

            val startFromEpoch: Long = 1727769600
            var result: MetricResult? = null
            var exception: Exception? = null

            try {
                createSimulation(startDate = Instant.fromEpochSeconds(startFromEpoch)) {
//                    enableComponentLogger()
                    val process = Process(processFile)

                    object : Component("Watcher") {
                        override fun repeatedProcess(): Sequence<Component> = sequence {
                            if (process.places.getValue("end").count() == process.totalProductRequest &&
                                process.places.getValue("end_product").count() == process.totalProductRequest
                            ) {
                                println("There are ${process.places.getValue("end").count()} tokens in the last place")

                                result = MetricResult(
                                    simulation = SimulationResult(env.totalTime()),
                                    executors = process.executors.values.flatten().map { executor ->
                                        val l = executor.waitTimeInQueue
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
            } catch (e: Exception) {
                log.error(e.message, e)
                exception = e
            }


            call.response.headers.append("Access-Control-Allow-Origin", "*")
            if (result != null) {
                call.application.environment.log.info("Result has been generated correctly")
                call.respond(result)
            } else {
                println("Result is null")
                println(exception)
                println(exception?.stackTraceToString())
                call.response.status(HttpStatusCode.InternalServerError)
                call.respond(SimulationError("Something went wrong"))
            }
        }
    }
}