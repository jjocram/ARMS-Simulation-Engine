import org.kalasim.*
import java.io.File

fun main() {
    createSimulation {
        //enableComponentLogger()

        val file = File("bpmnExamples/paper.bpmn")
        val process = BPMNProcess(file)

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