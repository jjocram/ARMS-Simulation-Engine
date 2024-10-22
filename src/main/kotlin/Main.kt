import element.*
import org.kalasim.*
import place.Place
import java.io.File
import kotlin.random.Random
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val processFile = File(args[0])
    if (!processFile.exists()) {
        error("File: ${args[0]}} not found")
        exitProcess(1)
    }

    createSimulation {
        enableComponentLogger()
        val p1 = Place()
        val p2 = Place()
        val p3 = Place()
        val p4 = Place()
        val p5 = Place()
        val p6 = Place()
        val p7 = Place()
        val p1Product = Place()
        val p2Product = Place()
        val p3Product = Place()
        val p4Product = Place()
        val p5Product = Place()
        val p6Product = Place()
        val p7Product = Place()

        StartEvent("1", "Start", p1, p1Product, 10)
        ExclusiveSplitGateway("2", "splitCheck", p1, p1Product, listOf(
            ExclusiveSplitCondition(p2, p2Product, false, { return@ExclusiveSplitCondition Random.nextDouble() >= 0.2 }),
            ExclusiveSplitCondition(p4, p4Product, true, { return@ExclusiveSplitCondition false }),))
        Activity("3", "A", p2, p3, p2Product, p3Product)
        Activity("4", "B", p4, p5, p4Product, p5Product)
        ExclusiveJoinGateway("5", "joinCheck", listOf(p3, p5), p6, listOf(p3Product, p5Product), p6Product)
        EndEvent("6", "End", p6, p7, p6Product, p7Product)

        object : Component("Watcher") {
            override fun repeatedProcess(): Sequence<Component> = sequence {
                if (p7.tokens.count() == 10 && p7Product.tokens.count() == 10) {
                    stopSimulation()
                }

                standby()
            }
        }

    }.run()
}