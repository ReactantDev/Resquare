package dev.reactant.resquare.bukkit

import dev.reactant.resquare.bukkit.container.BukkitRootContainer
import dev.reactant.resquare.event.ResquareClickEvent
import dev.reactant.resquare.event.ResquareCloseEvent
import dev.reactant.resquare.event.ResquareDragEvent
import dev.reactant.resquare.event.ResquareEvent
import dev.reactant.resquare.event.ResquareOpenEvent
import io.reactivex.rxjava3.subjects.PublishSubject
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

object BukkitRootContainerController {
    private val inventoryRootContainerMap = ConcurrentHashMap<Inventory, BukkitRootContainer>()
    private val rootContainerIdMap = ConcurrentHashMap<String, BukkitRootContainer>()

    fun onEnable() {
        Bukkit.getServer().pluginManager.registerEvents(eventListener, ResquareBukkit.instance)
    }

    fun onDisable() {
        inventoryRootContainerMap.values.forEach { it.destroy() }
    }

    val rootContainers get() = inventoryRootContainerMap.values

    private val _rootContainersSubject = PublishSubject.create<Collection<BukkitRootContainer>>()
    val rootContainersObservable = _rootContainersSubject.hide()

    internal fun addRootContainer(rootContainer: BukkitRootContainer) {
        this.inventoryRootContainerMap[rootContainer.inventory] = rootContainer
        this.rootContainerIdMap[rootContainer.id] = rootContainer
        this._rootContainersSubject.onNext(rootContainers)
    }

    internal fun removeRootContainer(rootContainer: BukkitRootContainer) {
        this.inventoryRootContainerMap.remove(rootContainer.inventory)
        this.rootContainerIdMap.remove(rootContainer.id)
        this._rootContainersSubject.onNext(rootContainers)
    }

    fun getRootContainerById(rootContainerId: String) = rootContainerIdMap[rootContainerId]

    private fun dispatchEventToTarget(resquareEvent: ResquareEvent<*>) {
        resquareEvent.target.dispatchEvent(resquareEvent)
    }

    private val eventListener = object : Listener {
        @EventHandler(priority = EventPriority.HIGH)
        fun onInventoryClick(event: InventoryClickEvent) {
            inventoryRootContainerMap[event.inventory]?.let {
                dispatchEventToTarget(ResquareClickEvent(event, it.getElementByRawSlot(event.rawSlot)))
            }
        }

        @EventHandler(priority = EventPriority.HIGH)
        fun onInventoryDrag(event: InventoryDragEvent) {
            inventoryRootContainerMap[event.inventory]?.let {
                dispatchEventToTarget(ResquareDragEvent(event, it))
            }
        }

        @EventHandler(priority = EventPriority.HIGH)
        fun onInventoryClose(event: InventoryCloseEvent) {
            inventoryRootContainerMap[event.inventory]?.let {
                dispatchEventToTarget(ResquareCloseEvent(event, it))
            }
        }

        @EventHandler(priority = EventPriority.HIGH)
        fun onInventoryOpen(event: InventoryOpenEvent) {
            inventoryRootContainerMap[event.inventory]?.let {
                dispatchEventToTarget(ResquareOpenEvent(event, it))
            }
        }
    }
}
