import kotlinx.datetime.Instant
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
//        enableComponentLogger()
        val process = BPMNProcess(processFile)

        dependency { process.inventories }
        dependency { process.compatibilityMap }
        dependency { process.accessories }

        process.executors

        object : Component("Watcher") {
            override fun repeatedProcess(): Sequence<Component> = sequence {

                process.productFamilies.filter { process.inventories.get(it.value).level>0 }.forEach {
                    val i = process.inventories.get(it.value)
                    println("${it.value.name}: ${i.level}")
                }
                println("###")

                process.bpmnElements.filter { it.value.activationTokens.isNotEmpty() }.forEach {
                    println("${it.value.id} has ${it.value.activationTokens.count()} tokens")
                }

                println("***")

                if (process.bpmnElements.filter { it.value is EndEvent }.map { it.value.activationTokens.count() }.sum() == process.totalTokens) {
                    val rawData = process.finalProducts
                        .map { process.inventories.get(it.value) }
                        .map {
                            mapOf("timestamps" to it.levelTimeline.timestamps, "values" to it.levelTimeline.values, "name" to it.name)
                        }

                    for (data in rawData) {
                        val f = File("${data.get("name")}.csv")
                        f.printWriter().use { out->
                            out.println("timestamp;level")
                            for (info in (data.get("timestamps") as MutableList<*>).zip((data.get("values") as MutableList<*>))) {
                                out.println("${(info.first as Instant).epochSeconds+1704119060};${info.second}")
                            }
                        }
                    }

                    stopSimulation()
                }

                standby()
            }
        }


    }.run()
}