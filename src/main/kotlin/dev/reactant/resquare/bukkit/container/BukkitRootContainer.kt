package dev.reactant.resquare.bukkit.container

import dev.reactant.resquare.bukkit.BukkitRootContainerController
import dev.reactant.resquare.bukkit.ResquareBukkit
import dev.reactant.resquare.bukkit.stylerender.BukkitStyleRender
import dev.reactant.resquare.bukkit.stylerender.BukkitStyleRenderResult
import dev.reactant.resquare.dom.Component
import dev.reactant.resquare.dom.Node
import dev.reactant.resquare.dom.RootContainer
import dev.reactant.resquare.dom.unaryPlus
import dev.reactant.resquare.elements.Body
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.event.ResquareCloseEvent
import dev.reactant.resquare.profiler.ProfilerDataChannel
import dev.reactant.resquare.render.NodeRenderState
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
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
    val multiThreadComponentRender: Boolean = false,
    val multiThreadStyleRender: Boolean = false,
    val autoDestroy: Boolean = true,
    override val debugName: String = title,
) : RootContainer() {
    override val rootState: NodeRenderState = NodeRenderState(
        parentState = null,
        debugName = "root",
        isDebug = ResquareBukkit.instance.isDebug,
        logger = ResquareBukkit.instance.logger,
        rootContainer = this,
    )
    private val threadPool = if (multiThreadComponentRender) Executors.newFixedThreadPool(1) else null
    private val threadScheduler = threadPool?.let { Schedulers.from(it) }
    override val updateObservable: Observable<Boolean> =
        (if (!multiThreadComponentRender) rootState.updatesObservable.observeOn(ResquareBukkit.instance.uiUpdateMainThreadScheduler)
        else rootState.updatesObservable.observeOn(Schedulers.from(threadPool!!)))
    private var destroyed = false

    val inventory = Bukkit.createInventory(null, width * height, title)

    val styleRenderResultObservable = renderResultObservable
        .observeOn(if (multiThreadStyleRender) threadScheduler else ResquareBukkit.instance.uiUpdateMainThreadScheduler)
        .map { BukkitStyleRender.convertBodyToPixels(this, it as Body, width, height) }!!

    private var inventoryRendered = false

    var lastStyleRenderResult: BukkitStyleRenderResult? = null
        private set

    private val compositeDisposable = CompositeDisposable()

    init {
        styleRenderResultObservable
            .let {
                if (multiThreadStyleRender) it.observeOn(ResquareBukkit.instance.uiUpdateMainThreadScheduler)
                else it
            }
            .subscribe {
                lastStyleRenderResult = it
                inventoryRendered = true
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        inventory.setItem(x + (y * width), it.pixels[x to y]?.itemStack)
                    }
                }
            }.let { compositeDisposable.add(it) }
    }

    init {
        val profilerDOMRenderTask = ProfilerDataChannel.currentProfilerResult?.createDOMRenderTask(this)
        profilerDOMRenderTask?.totalTimePeriod?.start()
        renderIteration(profilerDOMRenderTask = profilerDOMRenderTask)
        profilerDOMRenderTask?.totalTimePeriod?.end()
        if (autoDestroy) {
            addEventListener { e: ResquareCloseEvent ->
                this.destroy()
            }
        }
    }

    override fun destroy() {
        if (destroyed) return
        super.destroy()
        compositeDisposable.dispose()
        threadPool?.shutdown()
        destroyed = true
        BukkitRootContainerController.removeRootContainer(this)
    }

    fun openInventory(entity: HumanEntity) {
        if (lastStyleRenderResult == null) {
            styleRenderResultObservable.firstElement().subscribe {
                entity.openInventory(this.inventory)
            }.also { compositeDisposable.add(it) }
        } else {
            entity.openInventory(this.inventory)
        }
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
    multiThreadComponentRender: Boolean = false,
    multiThreadStyleRender: Boolean = false,
    autoDestroy: Boolean = true,
    debugName: String = title,
) = BukkitRootContainer({ +root() },
    width,
    height,
    title,
    multiThreadComponentRender,
    multiThreadStyleRender,
    autoDestroy,
    debugName)
    .also { BukkitRootContainerController.addRootContainer(it) }

fun <P : Any> createUI(
    root: Component.WithOptionalProps<P>,
    props: P? = null,
    width: Int,
    height: Int,
    title: String,
    /**
     * True to enable multithreading rendering for update instead of running in main thread
     */
    multiThreadComponentRender: Boolean = false,
    multiThreadStyleRender: Boolean = false,
    autoDestroy: Boolean = true,
    debugName: String = title,
) = BukkitRootContainer({ +root(props) },
    width,
    height,
    title,
    multiThreadComponentRender,
    multiThreadStyleRender,
    autoDestroy,
    debugName)
    .also { BukkitRootContainerController.addRootContainer(it) }

fun <P : Any> createUI(
    root: Component.WithProps<P>,
    props: P,
    width: Int,
    height: Int,
    title: String,
    /**
     * True to enable multithreading rendering for update instead of running in main thread
     */
    multiThreadComponentRender: Boolean = false,
    multiThreadStyleRender: Boolean = false,
    autoDestroy: Boolean = true,
    debugName: String = title,
) = BukkitRootContainer({ +root(props) },
    width,
    height,
    title,
    multiThreadComponentRender,
    multiThreadStyleRender,
    autoDestroy,
    debugName)
    .also { BukkitRootContainerController.addRootContainer(it) }
