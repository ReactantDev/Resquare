package dev.reactant.resquare.bukkit.debugger

import dev.reactant.resquare.bukkit.container.createUI
import dev.reactant.resquare.bukkit.debugger.server.ResquareWebsocketServer
import dev.reactant.resquare.dom.childrenOf
import dev.reactant.resquare.dom.declareComponent
import dev.reactant.resquare.elements.DivProps
import dev.reactant.resquare.elements.div
import dev.reactant.resquare.elements.styleOf
import dev.reactant.resquare.render.useState
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
            val test = declareComponent {
                val (x, setX) = useState(5)

                childrenOf(
                    div(DivProps(
                        style = styleOf {
                            width = 100.percent
                            height = 100.percent
                            alignItems.stretch()
                        },
                        children = childrenOf(
                            div(DivProps(
                                style = styleOf {
                                    width = x.px
                                    height = 4.px
                                },
                                background = ItemStack(Material.APPLE)
                            )),
                            div(DivProps(
                                style = styleOf {
                                    flexGrow = 1f
                                    height = 100.percent
                                },
                                background = ItemStack(Material.FEATHER)
                            )),
                        )
                    ))
                )
            }

            val ui = createUI(test, 9, 6, "test")
            ui.openInventory(sender as Player)

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
