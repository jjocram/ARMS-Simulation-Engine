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



    }.run()
}