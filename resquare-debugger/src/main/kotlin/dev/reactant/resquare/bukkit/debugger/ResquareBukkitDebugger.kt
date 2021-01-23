package dev.reactant.resquare.bukkit.debugger

import dev.reactant.resquare.bukkit.container.createUI
import dev.reactant.resquare.bukkit.debugger.server.ResquareWebsocketServer
import dev.reactant.resquare.dom.childrenOf
import dev.reactant.resquare.dom.declareComponent
import dev.reactant.resquare.elements.DivProps
import dev.reactant.resquare.elements.div
import dev.reactant.resquare.elements.styleOf
import dev.reactant.resquare.event.ResquareClickEvent
import dev.reactant.resquare.render.memo
import dev.reactant.resquare.render.useCallback
import dev.reactant.resquare.render.useEffect
import dev.reactant.resquare.render.useRootContainer
import dev.reactant.resquare.render.useState
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class ResquareBukkitDebugger : JavaPlugin() {
    init {
        _instance = this
    }

    override fun onEnable() {
        ResquareWebsocketServer.onStart()
    }

    override fun onDisable() {
        ResquareWebsocketServer.onStop()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.toLowerCase() == "resquare-debug") {
            data class ProgressBarProps(
                val percentage: Float,
                val background: ItemStack?,
                val fill: ItemStack?,
            )

            val deco = declareComponent("deco") {
                val (decoItem, setDecoItem) = useState(ItemStack(Material.CAKE))
                useEffect({
                    val task = Bukkit.getScheduler().runTaskTimer(ResquareBukkitDebugger.instance, Runnable {
                        setDecoItem(ItemStack(Material.values().random()))
                    }, 50, 50);
                    {
                        task.cancel()
                    }
                }, arrayOf())
                div(DivProps(
                    style = styleOf {
                        width = 1.px
                        height = 1.px
                    },
                    background = decoItem
                ))
            }

            val memoDeco = memo(deco)

            val progressBar = declareComponent("progressBar") { props: ProgressBarProps ->

                div(DivProps(
                    style = styleOf {
                        width = 100.percent
                        height = 1.px
                    },
                    background = props.background,
                    children = childrenOf(

                        div(DivProps(
                            style = styleOf {
                                width = props.percentage.percent
                                height = 1.px
                                alignItems.flexEnd()
                            },
                            background = props.fill,
                            children = childrenOf(deco())
                        ))

                    )
                ))
            }

            val memoProgressBar = memo(progressBar)

            val testApp = declareComponent {
                val (progress, setProgress) = useState(0f)
                val rootContainer = useRootContainer()
                useEffect({
                    val handler = rootContainer.addEventListener<ResquareClickEvent<*>> { it.preventDefault() }
                    return@useEffect { rootContainer.removeEventListener(handler) }
                }, arrayOf())

                val onIncreaseButtonClick = useCallback({ e: ResquareClickEvent<*> ->
                    setProgress((progress + (1f / 9) * 100).coerceAtMost(100f))
                }, arrayOf(progress))

                val onDecreaseButtonClick = useCallback({ e: ResquareClickEvent<*> ->
                    setProgress((progress - (1f / 9) * 100).coerceAtLeast(0f))
                }, arrayOf(progress))

                div(DivProps(
                    style = styleOf {
                        flexDirection.column()
                        alignItems.center()
                        justifyContent.spaceAround()
                        width = 100.percent
                        height = 100.percent
                    },
                    children = childrenOf(
                        memoProgressBar(ProgressBarProps(
                            percentage = progress,
                            background = ItemStack(Material.WHITE_STAINED_GLASS),
                            fill = ItemStack(Material.GREEN_STAINED_GLASS)
                        )),

                        memoProgressBar(ProgressBarProps(
                            percentage = 14.5f,
                            background = ItemStack(Material.WHITE_STAINED_GLASS),
                            fill = ItemStack(Material.GREEN_STAINED_GLASS)
                        )),

                        div(DivProps(
                            style = styleOf {
                                width = 3.px
                                justifyContent.spaceBetween()
                            },
                            children = childrenOf(
                                div(DivProps(
                                    style = styleOf {
                                        width = 1.px
                                        height = 1.px
                                    },
                                    background = ItemStack(Material.GREEN_WOOL),
                                    onClick = onIncreaseButtonClick,
                                )),

                                div(DivProps(
                                    style = styleOf {
                                        width = 1.px
                                        height = 1.px
                                    },
                                    background = ItemStack(Material.RED_WOOL),
                                    onClick = onDecreaseButtonClick,
                                ))
                            )
                        )),

                        )
                ))
            }

            val ui = createUI(testApp, 9, 6, "test", true)

            if (sender is Player) {
                ui.openInventory(sender)
            }

            return true
        }
        return false
    }

    companion object {
        private var _instance: ResquareBukkitDebugger? = null
        val instance: ResquareBukkitDebugger
            get() = _instance ?: throw IllegalStateException("Resquare Debugger is not yet enabled")
    }
}

internal val bukkitLogger get() = ResquareBukkitDebugger.instance.logger
