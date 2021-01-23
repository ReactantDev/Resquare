package dev.reactant.resquare.render

import dev.reactant.resquare.dom.Component
import dev.reactant.resquare.dom.declareComponent

fun <P : Any> memo(
    componentNode: Component.WithProps<P>,
    compareFn: (prev: P, next: P) -> Boolean = { prev, next -> prev == next },
) =
    declareComponent<P>("memo(${componentNode.name})") { props ->
        val (getPrevProps, setPrevProps) = claimState(props)
        val (getMemoNode, setMemoNode) = claimStateLazy { componentNode(props) }

        if (!compareFn(getPrevProps(), props)) {
            setMemoNode(componentNode(props))
        }
        setPrevProps(props)

        getMemoNode()
    }

fun <P : Any> memo(
    componentNode: Component.WithOptionalProps<P>,
    compareFn: (prev: P, next: P) -> Boolean = { prev, next -> prev == next },
) =
    declareComponent("memo(${componentNode.name})", componentNode.defaultPropsFactory) { props ->
        val (getPrevProps, setPrevProps) = claimState(props)
        val (getMemoNode, setMemoNode) = claimStateLazy { componentNode(props) }

        if (!compareFn(getPrevProps(), props)) {
            setMemoNode(componentNode(props))
        }
        setPrevProps(props)

        getMemoNode()
    }

fun memo(componentNode: Component.WithoutProps) =
    declareComponent("memo(${componentNode.name})") {
        val (getMemoNode, setMemoNode) = claimStateLazy { componentNode() }
        getMemoNode()
    }
