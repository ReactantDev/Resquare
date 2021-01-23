package dev.reactant.resquare.bukkit.container

import dev.reactant.resquare.bukkit.ResquareBukkit
import dev.reactant.resquare.bukkit.stylerender.BukkitStyleRender
import dev.reactant.resquare.bukkit.stylerender.BukkitStyleRenderResult
import dev.reactant.resquare.dom.Component
import dev.reactant.resquare.dom.Node
import dev.reactant.resquare.dom.RootContainer
import dev.reactant.resquare.dom.unaryPlus
import dev.reactant.resquare.elements.Body
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.render.NodeRenderState
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import java.util.concurrent.Executors

class BukkitRootContainer internal constructor(
    override val content: () -> Node,
    val width: Int,
    val height: Int,
    val title: String,
    /**
     * True to enable multithreading rendering for update instead of running in main thread
     */
    val multiThread: Boolean = false
) : RootContainer() {
    override val rootState: NodeRenderState = NodeRenderState(
        parentState = null,
        debugName = "root",
        isDebug = ResquareBukkit.instance.isDebug,
        logger = ResquareBukkit.instance.logger,
        rootContainer = this,
    )
    private val threadPool = if (multiThread) Executors.newFixedThreadPool(1) else null
    override val updateObservable: Observable<Boolean> =
        (if (!multiThread) rootState.updatesObservable.observeOn(ResquareBukkit.instance.mainThreadScheduler)
        else rootState.updatesObservable.observeOn(Schedulers.from(threadPool!!)))
    private var destroyed = false

    val inventory = Bukkit.createInventory(null, width * height, title)

    val styleRenderResultObservable = renderResultObservable
        .observeOn(ResquareBukkit.instance.mainThreadScheduler)
        .map { BukkitStyleRender.convertBodyToPixels(it as Body, width, height) }!!

    private var inventoryRendered = false

    var lastStyleRenderResult: BukkitStyleRenderResult? = null
        private set

    // TODO: to develop event system, it is needed to store pixel.element so we can find out which element is being click
    private val styleRenderResultSubscription = styleRenderResultObservable.subscribe {
        lastStyleRenderResult = it
        inventoryRendered = true
        for (x in 0 until width) {
            for (y in 0 until height) {
                inventory.setItem(x + (y * width), it.pixels[x to y]?.itemStack)
            }
        }
    }

    init {
        render()
    }

    // TODO: AUTO dispose when no one watching it

    override fun destroy() {
        super.destroy()
        styleRenderResultSubscription?.dispose()
        threadPool?.shutdown()
        destroyed = true
    }

    fun openInventory(entity: HumanEntity) {
        entity.openInventory(this.inventory)
    }

    internal fun getElementByRawSlot(rawSlot: Int): Element {
        if (lastStyleRenderResult == null) throw IllegalStateException()
        if (rawSlot >= width * height) return this
        val position = rawSlot % width to rawSlot / width
        return lastStyleRenderResult?.pixels?.get(position)?.element ?: this
    }
}

fun createUI(
    root: Component.WithoutProps,
    width: Int,
    height: Int,
    title: String,
    /**
     * True to enable multithreading rendering for update instead of running in main thread
     */
    multiThread: Boolean = false
) = BukkitRootContainer({ +root() }, width, height, title, multiThread)
    .also { ResquareBukkit.instance.rootContainerController.addRootContainer(it) }