package dev.reactant.resquare.bukkit.debugger

import dev.reactant.resquare.bukkit.container.createUI
import dev.reactant.resquare.bukkit.debugger.server.ResquareWebsocketServer
import dev.reactant.resquare.dom.childrenOf
import dev.reactant.resquare.dom.declareComponent
import dev.reactant.resquare.dom.unaryPlus
import dev.reactant.resquare.elements.DivProps
import dev.reactant.resquare.elements.div
import dev.reactant.resquare.elements.styleOf
import dev.reactant.resquare.render.useEffect
import dev.reactant.resquare.render.useMemo
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

            val progressBar = declareComponent { props: ProgressBarProps ->

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
                            },
                            background = props.fill
                        ))

                    )
                ))
            }

            val test = declareComponent {
                val (progress, setProgress) = useState(5)

                useEffect({
                    Bukkit.getScheduler().runTaskTimer(ResquareBukkitDebugger.instance, Runnable {
                        if (progress == 100) {
                            setProgress(0)
                        } else {
                            setProgress(progress + 1)
                        }
                    }, 1, 1).let { task -> { task.cancel() } }
                }, arrayOf(progress))

                val items = useMemo({
                    (0..(progress / 2)).map { ItemStack(Material.COOKED_BEEF) }
                }, arrayOf(progress))

                childrenOf(
                    div(DivProps(
                        style = styleOf {
                            width = 100.percent
                            height = 100.percent
                            flexDirection.column()
                            alignItems.center()
                        },
                        children = childrenOf(
                            progressBar(ProgressBarProps(
                                percentage = progress.toFloat(),
                                fill = ItemStack(Material.WHITE_STAINED_GLASS),
                                background = ItemStack(Material.RED_STAINED_GLASS),
                            )),
                            div(DivProps(
                                style = styleOf {
                                    width = 5.px
                                    height = 1.px
                                },
                                background = ItemStack(Material.BAKED_POTATO),
                            )),
                            div(DivProps(
                                style = styleOf {
                                    width = 7.px
                                    height = 1.px
                                },
                                background = ItemStack(Material.COOKIE),
                            )),
                            div(DivProps(
                                style = styleOf {
                                    width = 12.px
                                    height = 1.px
                                    flexGrow = 1f
                                    flexDirection.row()
                                    flexWrap.wrap()
                                    alignItems.flexStart()
                                    alignContent.flexStart()
                                    justifyContent.flexEnd()
                                },
                                background = ItemStack(Material.BEEF),
                                children = childrenOf(
                                    +items.mapIndexed { index, item ->
                                        div(DivProps(
                                            style = styleOf {
                                                width = 1.px
                                                height = 1.px
                                            },
                                            background = item
                                        ), key = index.toString())
                                    }
                                )
                            )),
                        )
                    ))
                )
            }

            val ui = createUI(test, 9, 6, "test", true)

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
