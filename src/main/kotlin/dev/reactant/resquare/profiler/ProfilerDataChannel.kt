package dev.reactant.resquare.profiler

import dev.reactant.resquare.bukkit.BukkitRootContainerController
import dev.reactant.resquare.dom.RootContainer
import dev.reactant.resquare.render.NodeRenderState
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

interface ProfilerRenderTask {
    val totalTimePeriod: TaskTimePeriod
    val threadName: String
}

data class TaskTimePeriod(
    var startTime: String? = null,
    var endTime: String? = null,
) {
    fun start() {
        this.startTime = Instant.now().toString()
    }

    fun end() {
        this.endTime = Instant.now().toString()
    }
}

data class ProfilerNodeRenderState(
    val id: String,
    val subNodeRenderStates: List<NodeRenderState>
)

data class ProfilerDOMRenderTaskIteration(
    val totalTimePeriod: TaskTimePeriod = TaskTimePeriod(),
    val nodeStateIdTimePeriodMap: HashMap<String, TaskTimePeriod> = HashMap(),
    val profilerNodeRenderState: ProfilerNodeRenderState? = null,
)

data class ProfilerDOMRenderTask(
    val iterations: ArrayList<ProfilerDOMRenderTaskIteration> = ArrayList(),
    override val totalTimePeriod: TaskTimePeriod = TaskTimePeriod(),
    override val threadName: String
) : ProfilerRenderTask {
    fun createIteration() = ProfilerDOMRenderTaskIteration().also { iterations.add(it) }
}

data class ProfilerData(
    val domRenderTasks: ArrayList<ProfilerDOMRenderTask> = ArrayList()
) {
    fun createDOMRenderTask() =
        ProfilerDOMRenderTask(threadName = Thread.currentThread().name).also { domRenderTasks.add(it) }
}

object ProfilerDataChannel {
    val profilerDataCollectMap = ConcurrentHashMap<RootContainer, ProfilerData>()
    fun startProfiling(rootContainerId: String): Boolean {
        val rootContainer = BukkitRootContainerController.getRootContainerById(rootContainerId) ?: return false
        if (!profilerDataCollectMap.contains(rootContainer)) {
            this.profilerDataCollectMap[rootContainer] = ProfilerData()
        } else {
            throw IllegalStateException()
        }
        return true
    }

    fun stopProfiling(rootContainerId: String): ProfilerData? {
        val rootContainer = BukkitRootContainerController.getRootContainerById(rootContainerId)
        if (rootContainer == null) throw IllegalArgumentException()
        if (!profilerDataCollectMap.contains(rootContainer)) throw IllegalStateException()
        return profilerDataCollectMap[rootContainer]
    }
}
