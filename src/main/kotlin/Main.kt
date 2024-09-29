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
        //enableComponentLogger()
        val process = BPMNProcess(processFile)

        dependency { process.inventories }
        dependency { process.compatibilityMap }
        dependency { process.accessories }

        process.executors

        object : Component("Watcher") {
            override fun repeatedProcess(): Sequence<Component> = sequence {

                process.finalProducts.forEach {
                    println("${it.value.name}: ${process.inventories.get(it.value).level}")
                }
//                println("###")
//                process.bpmnElements.forEach {
//                    println("${it.value.id} => ${it.value.activationTokens.count()}")
//                }
                println("***")

                standby()
            }
        }

    }.run()
}