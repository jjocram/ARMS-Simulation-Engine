import element.*
import place.Place
import token.ResourceToken
import java.io.File
import kotlin.collections.List
import kotlin.random.Random

class Process(file: File) {
    private val xmlProcess = XMLProcess(file)

    val totalProductRequest = xmlProcess.productRequests.map { it.value.quantity }.sum()

    val places = (xmlProcess.xmlElements.values
        .map { it.outgoings + it.incomings }
        .flatten() + listOf("end"))
        .toSet()
        .map { listOf(it, it + "_product") }
        .flatten()
        .associateBy { it }
        .mapValues { Place() }

    private val inventories = xmlProcess.inventories.mapValues { Place() }

    init {
        xmlProcess.inventories.forEach {
            inventories.getValue(it.key).add(List(it.value.startQuantity) { ResourceToken() })
        }
    }

    private val accessories = xmlProcess.accessories.mapValues { Place() }

    init {
        xmlProcess.accessories.forEach {
            accessories.getValue(it.key).add(List(it.value.quantity) { ResourceToken() })
        }
    }

    /**
     * Map an executor (its id) in the model to a list of executors representing the different instances give by the quantity.
     * Each executor will have a unique id given by the executor id in the model and the instance number [0, quantity[
     */
    val executors = xmlProcess.executors.mapValues { xmlExecutor ->
        List(xmlExecutor.value.quantity) {
            ActivityExecutor(
                "${xmlExecutor.value.id}-$it",
                "${xmlExecutor.value.name}-$it"
            )
        }
    }

    private val compatibilities = xmlProcess.compatibilities.mapValues { (_, compatibility) ->
        executors.getValue(compatibility.idExecutor).map {
            Compatibility(
                compatibility.id,
                it,
                compatibility.productProperties.associate { it.key to it.value },
                compatibility.accessories.map { ResourceRequest(accessories.getValue(it.id), it.quantity) },
                compatibility.duration,
                compatibility.batchSize
            )
        }
    }

    private val transformations = xmlProcess.transformations.mapValues { (_, transformation) ->
        Transformation(
            transformation.inputs.map {
                ResourceRequest(
                    inventories.getValue(it.id),
                    it.quantity
                )
            }, // TODO: fix inventory input without inventoryId
            transformation.outputs.map { ResourceRequest(inventories.getValue(it.id), it.quantity) },
            transformation.productProperties.associate { it.key to it.value },
            transformation.transformationToApply.associate { it.key to it.value },
        )
    }

    val bpmnElements = getBPMNElements()
    private fun getBPMNElements(): Map<String, BPMNElement> {
        val elements = xmlProcess.xmlElements.mapValues { (_, element) ->
            when (element) {
                is XMLEndEvent -> {
                    require(element.incomings.size == 1) { "To build a end event, there must be exactly one incoming flow" }
                    val incomingFlow = element.incomings.first()
                    EndEvent(
                        element.id,
                        null,
                        places.getValue(incomingFlow),
                        places.getValue("end"),
                        places.getValue(incomingFlow + "_product"),
                        places.getValue("end_product")
                    )
                }

                is XMLExclusiveGateway -> {
                    when (element.type) {
                        GatewayType.FORK -> {
                            require(element.incomings.size == 1) { "To build a fork gateway, there must be exactly one incoming flow" }
                            require(element.outgoings.size > 1) { "To build a fork gateway, there must be at least two outgoing flow" }
                            val incomingFlow = element.incomings.first()
                            ExclusiveSplitGateway(
                                element.id,
                                element.name,
                                places.getValue(incomingFlow),
                                places.getValue(incomingFlow + "_product"),
                                element.outgoings
                                    .map { xmlProcess.sequenceFlows.getValue(it) }
                                    .map { it.toCondition(places) }
                            )
                        }

                        GatewayType.JOIN -> {
                            require(element.incomings.size > 1) { "To build a fork gateway, there must be at least two incoming flow" }
                            require(element.outgoings.size == 1) { "To build a fork gateway, there must be at least two outgoing flow" }
                            val outgoingFlow = element.outgoings.first()
                            ExclusiveJoinGateway(
                                element.id,
                                element.name,
                                element.incomings.map { places.getValue(it) },
                                places.getValue(outgoingFlow),
                                element.incomings.map { places.getValue(it + "_product") },
                                places.getValue(outgoingFlow + "_product")
                            )
                        }
                    }
                }

                is XMLParallelGateway -> {
                    when (element.type) {
                        GatewayType.FORK -> {
                            require(element.incomings.size == 1) { "To build a fork gateway, there must be exactly one incoming flow" }
                            require(element.outgoings.size > 1) { "To build a fork gateway, there must be at least two outgoing flow" }
                            val incomingFlow = element.incomings.first()
                            ParallelSplitGateway(
                                element.id,
                                places.getValue(incomingFlow),
                                element.outgoings.map { places.getValue(it) },
                                places.getValue(incomingFlow + "_product"),
                                element.outgoings.map { places.getValue(it + "_product") },
                            )
                        }

                        GatewayType.JOIN -> {
                            require(element.incomings.size > 1) { "To build a fork gateway, there must be at least two incoming flow" }
                            require(element.outgoings.size == 1) { "To build a fork gateway, there must be at least two outgoing flow" }
                            val outgoingFlow = element.outgoings.first()
                            ParallelJoinGateway(
                                element.id,
                                element.incomings.map { places.getValue(it) },
                                places.getValue(outgoingFlow),
                                element.incomings.map { places.getValue(it + "_product") },
                                places.getValue(outgoingFlow + "_product")
                            )
                        }
                    }
                }

                is XMLStartEvent -> {
                    require(element.outgoings.size == 1) { "To build a start event, there must be exactly one outgoing flow" }
                    require(xmlProcess.productRequests.isNotEmpty()) { "To build a start event, there must be at least one product request" }
                    val outgoingFlow = element.outgoings.first()
                    StartEvent(
                        element.id,
                        null,
                        places.getValue(outgoingFlow),
                        places.getValue(outgoingFlow + "_product"),
                        xmlProcess.productRequests.map { it.value.toProductRequest() })
                }

                is XMLTask -> {
                    val incomingFlows = xmlProcess.sequenceFlows
                        .filterValues { it.targetRef == element.id }
                        .map { it.value.id }
                    val outgoingFlows = xmlProcess.sequenceFlows
                        .filterValues { it.sourceRef == element.id }
                        .map { it.value.id }
                    require(incomingFlows.count() > 0) { "To build a task (${element.id}), there must be at least one incoming flow" }
                    require(outgoingFlows.count() == 1) { "To build a task (${element.id}), there must be exactly one outgoing flow" }

                    val outgoingFlow = outgoingFlows.first()
                    Activity(
                        element.id,
                        element.name,
                        incomingFlows.map { Pair(places.getValue(it), places.getValue(it + "_product")) },
                        places.getValue(outgoingFlow),
                        places.getValue(outgoingFlow + "_product"),
                        xmlProcess.compatibilities.filter { it.value.idActivity == element.id }
                            .map { compatibilities.getValue(it.key) }.flatten(),
                        xmlProcess.transformations.filter { it.value.idActivity == element.id }
                            .map { transformations.getValue(it.key) },
                    )
                }

                is XMLTimeEvent -> {
                    require(element.incomings.size == 1) { "To build a time event, there must be exactly one incoming flow" }
                    require(element.outgoings.size == 1) { "To build a time event, there must be exactly one outgoing flow" }
                    val incomingFlow = element.incomings.first()
                    val outgoingFlow = element.outgoings.first()
                    TimeEvent(
                        element.id,
                        places.getValue(incomingFlow),
                        places.getValue(outgoingFlow),
                        places.getValue(incomingFlow + "_product"),
                        places.getValue(outgoingFlow + "_product"),
                        element.duration
                    )
                }
            }
        }

        return elements
    }
}