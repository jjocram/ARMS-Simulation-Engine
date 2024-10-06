import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

class BPMNProcess(file: File) {
    private val xmlProcess = XMLProcess(file)

    val finalProducts = xmlProcess.finalProducts.mapValues { ProductFamily(it.value.id, it.value.name) }
    val totalTokens = finalProducts
        .map { xmlProcess.finalProducts.getValue(it.value.id).finalQuantity!!.toInt() }
        .sum()

    val accessories =
        xmlProcess.accessories.mapValues { Accessory(it.value.id, it.value.name, it.value.quantity.toDouble()) }

    val bpmnElements = getBPMNElements()
    private fun getBPMNElements(): Map<String, BPMNElement> {
        val bpmnElements = xmlProcess.xmlElements.mapValues { it.value.toBPMNElement() }

        // 1. Fill nextElements for each BPMN Element
        bpmnElements.forEach { (id, element) ->
            xmlProcess.xmlElements.getValue(id).outgoings
                .mapNotNull { xmlProcess.sequenceFlows.get(it) } // MapNotNull because Activities has outgoings towards executors
                .map { bpmnElements.getValue(it.targetRef) }
                .let { element.nextElements = it }
        }

        // 2. Specific element setup

        // 2.1. Assign transformation to activities
        bpmnElements
            .filter { it.value is Activity }
            .forEach { (_, activity) -> createTransformationMapFor(activity as Activity) }

        // 2.2 Conditions for exclusive gateways
        bpmnElements
            .filter { it.value is ExclusiveForkGateway }
            .forEach { (id, exclusiveForkGateway) ->
                val conditions = xmlProcess.xmlElements.getValue(id).outgoings
                    .map { xmlProcess.sequenceFlows.getValue(it) }
                    .mapNotNull {
                        if (bpmnElements.containsKey(it.targetRef)) Pair(
                            bpmnElements[it.targetRef],
                            it
                        ) else null
                    }
                    .map {
                        Condition(
                            it.first!!,
                            it.second.name?.lowercase() == "default",
                            {
                                it.second.name?.lowercase() != "default" && (Random.nextDouble() <= (it.second.name?.toDouble()
                                    ?: 1.0))
                            })
                    } // TODO improve notation: 0.0 < sequenceFlow < 1.0
                (exclusiveForkGateway as ExclusiveForkGateway).conditions = conditions
            }

        // 2.3 Product request for startEvent
        bpmnElements.filter { it.value is StartEvent }.values.firstOrNull()?.let { startEvent ->
            (startEvent as StartEvent).requests =
                finalProducts.map {
                    ProductFamilyRequest(
                        it.value,
                        xmlProcess.finalProducts.getValue(it.value.id).finalQuantity!!.toInt()
                    )
                }
        }

        return bpmnElements
    }

    private fun createTransformationMapFor(activity: Activity) {
        xmlProcess.transformations
            .filter { it.value.activityId == activity.id }
            .forEach { (_, xmlTransformation) ->
                val associatedProduct = finalProducts.getValue(xmlTransformation.productId)
                val inputs = xmlTransformation.inputs.map {
                    TransformationMap.TransformationIO(
                        ProductFamily(it.id, it.productType),
                        it.quantity
                    )
                }
                val outputs = xmlTransformation.outputs.map {
                    TransformationMap.TransformationIO(
                        ProductFamily(it.id, it.productType),
                        it.quantity
                    )
                }
                activity.transformations.add(associatedProduct, TransformationMap.Transformation(inputs, outputs))
            }
    }

    val productFamilies = getPFs()
    private fun getPFs(): Map<String, ProductFamily> {
        val pfInTransformations = xmlProcess.transformations
            .map { it.value.inputs + it.value.outputs }
            .flatten()
            .map { ProductFamily(it.id, it.productType) }
            .associateBy { it.id }
        println(finalProducts)
        return finalProducts + pfInTransformations
    }

    val executors = getExecutorsForBPMNElements()
    private fun getExecutorsForBPMNElements(): Map<String, Executor> {
        val uniqueExecutors = bpmnElements
            .map { it.value }
            .mapNotNull {
                when (it) {
                    is Activity -> null
                    is EndEvent -> null
                    is ExclusiveForkGateway -> ExclusiveForkGatewayExecutor(it)
                    is ExclusiveJoinGateway -> ExclusiveJoinGatewayExecutor(it)
                    is ParallelForkGateway -> ParallelForkGatewayExecutor(it)
                    is ParallelJoinGateway -> ParallelJoinGatewayExecutor(it)
                    is StartEvent -> StartEventExecutor(it)
                    is TimeEvent -> TimeEventExecutor(it)
                }
            }
            .associateBy { it.id }

        val activityExecutors = xmlProcess.executors
            .map { ActivityExecutor(it.value.id) }
            .associateBy { it.id }

        return uniqueExecutors + activityExecutors
    }

    val compatibilityMap = CompatibilityMap()

    init {
        // Add compatibilities to the CompatibilityMap
        xmlProcess.compatibilities.forEach { (_, compatibility) ->
            compatibilityMap.add(
                bpmnElements.getValue(compatibility.idActivity),
                productFamilies.getValue(compatibility.idProduct),
                executors.getValue(compatibility.idExecutor),
                compatibility.duration,
                compatibility.batch
            )

        }

        // Add a compatibility item for each final product for the unique executors associated to BPMNElements
        bpmnElements
            .filter { it.value !is Activity }
            .filter { it.value !is EndEvent }
            .forEach { (id, element) ->
                val elementExecutor = executors.getValue(id)
                finalProducts.forEach { (_, finalProduct) ->
                    compatibilityMap.add(element, finalProduct, elementExecutor, 0.minutes)
                }
            }
    }

    val inventories = Inventories()

    init {
        productFamilies.forEach { (_, productFamily) ->
            inventories.add(productFamily)
        }
    }
}