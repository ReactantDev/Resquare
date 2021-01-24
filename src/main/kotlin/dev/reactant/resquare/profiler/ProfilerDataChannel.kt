package dev.reactant.resquare.profiler

import dev.reactant.resquare.bukkit.container.BukkitRootContainer
import dev.reactant.resquare.dom.RootContainer
import dev.reactant.resquare.render.NodeRenderState

interface ProfilerRenderTask {
    val totalTimePeriod: TaskTimePeriod
    val threadName: String
}

data class TaskTimePeriod(
    var startTime: Long? = null,
    var endTime: Long? = null,
) {
    fun start() {
        this.startTime = System.nanoTime()
    }

    fun end() {
        this.endTime = System.nanoTime()
    }
}

data class ProfilerNodeRenderState(
    val id: String,
    val name: String,
    val subNodeRenderStates: List<ProfilerNodeRenderState>
) {
    companion object {
        fun from(nodeRenderState: NodeRenderState): ProfilerNodeRenderState =
            ProfilerNodeRenderState(
                nodeRenderState.id,
                nodeRenderState.debugName,
                nodeRenderState.subNodeRenderStates.values.map(::from)
            )
    }
}

data class ProfilerDOMRenderTaskIteration(
    val totalTimePeriod: TaskTimePeriod = TaskTimePeriod(),
    val nodeStateIdTimePeriodMap: HashMap<String, TaskTimePeriod> = HashMap(),
    var profilerNodeRenderState: ProfilerNodeRenderState? = null,
)

data class RootContainerInfo(
    val title: String?,
    val viewer: List<String>,
    val width: Int,
    val height: Int,
    val multithread: Boolean,
) {
    companion object {
        fun from(rootContainer: BukkitRootContainer) = RootContainerInfo(
            rootContainer.title,
            rootContainer.inventory.viewers.map { it.name },
            rootContainer.width,
            rootContainer.height,
            rootContainer.multiThread,
        )
    }
}

data class ProfilerDOMRenderTask(
    val iterations: ArrayList<ProfilerDOMRenderTaskIteration> = ArrayList(),
    override val totalTimePeriod: TaskTimePeriod = TaskTimePeriod(),
    override val threadName: String,
    val rootContainerInfo: RootContainerInfo,
) : ProfilerRenderTask {
    val type = "DOMRenderTask"
    fun createIteration() = ProfilerDOMRenderTaskIteration().also { iterations.add(it) }
}

data class ProfilerResult(
    val domRenderTasks: ArrayList<ProfilerDOMRenderTask> = ArrayList(),
    val totalTimePeriod: TaskTimePeriod = TaskTimePeriod(),
) {
    fun createDOMRenderTask(rootContainer: RootContainer) =
        ProfilerDOMRenderTask(threadName = Thread.currentThread().name,
            rootContainerInfo = RootContainerInfo.from(rootContainer as BukkitRootContainer)
        ).also {
            synchronized(domRenderTasks) {
                domRenderTasks.add(it)
            }
        }
}

object ProfilerDataChannel {
    var currentProfilerResult: ProfilerResult? = null

    fun startProfiling() {
        assert(currentProfilerResult == null)
        this.currentProfilerResult = ProfilerResult().also { it.totalTimePeriod.start() }
    }

    fun stopProfiling(): ProfilerResult {
        assert(currentProfilerResult != null)
        return this.currentProfilerResult!!.also {
            it.totalTimePeriod.end()
            this.currentProfilerResult = null
        }
    }
}
