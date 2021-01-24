package dev.reactant.resquare.bukkit.debugger.server

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.reactant.resquare.bukkit.BukkitRootContainerController
import dev.reactant.resquare.bukkit.container.BukkitRootContainer
import dev.reactant.resquare.bukkit.debugger.server.models.DebuggerRootContainer
import dev.reactant.resquare.profiler.ProfilerDataChannel
import dev.reactant.resquare.profiler.ProfilerResult
import io.ktor.application.install
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.awaitFirst

enum class RemoteInputType {
    RenderingInspectStart,
    RenderingInspectStop,

    ProfilingStart,
    ProfilingStop,
}

data class RemoteInput<T>(
    val type: RemoteInputType,
    val data: T,
)

enum class RemoteOutputType {
    RootContainerListUpdate,
    RenderingInspectFrame,
    ProfilingResult,
}

data class RemoteOutput<T>(
    val type: RemoteOutputType,
    val data: T,
)

data class RootContainerRecord(
    val id: String,
    val title: String,
    val viewer: List<String>
) {
    companion object {
        fun from(rootContainer: BukkitRootContainer) = RootContainerRecord(
            id = rootContainer.id,
            title = rootContainer.title,
            viewer = rootContainer.inventory.viewers.map { it.name }
        )
    }
}

object ResquareWebsocketServer {
    val gson = Gson()
    var server: NettyApplicationEngine = embeddedServer(Netty, port = 27465) {
        install(WebSockets)

        routing {
            webSocket("/") {
                send(Frame.Text(gson.toJson(
                    RemoteOutput(
                        type = RemoteOutputType.RootContainerListUpdate,
                        data = BukkitRootContainerController.rootContainers.map(RootContainerRecord.Companion::from)
                    ),
                    object : TypeToken<RemoteOutput<Collection<RootContainerRecord>>>() {}.type
                )))
                BukkitRootContainerController.rootContainers
                launch {
                    while (true) {
                        send(Frame.Text(gson.toJson(
                            RemoteOutput(
                                type = RemoteOutputType.RootContainerListUpdate,
                                data = BukkitRootContainerController.rootContainersObservable.awaitFirst()
                                    .map(RootContainerRecord.Companion::from)
                            ),
                            object : TypeToken<RemoteOutput<Collection<RootContainerRecord>>>() {}.type
                        )))
                    }
                }

                try {
                    var launchingInspectJob: Job? = null
                    while (true) {
                        val rawInput = (incoming.receive() as Frame.Text).readText()
                        parseDebugInput(rawInput).let { remoteInput ->
                            when (remoteInput.type) {
                                RemoteInputType.ProfilingStart -> ProfilerDataChannel.startProfiling()
                                RemoteInputType.ProfilingStop -> {
                                    ProfilerDataChannel.stopProfiling().let {
                                        send(Frame.Text(gson.toJson(
                                            RemoteOutput(
                                                type = RemoteOutputType.ProfilingResult,
                                                data = it
                                            ),
                                            object :
                                                TypeToken<RemoteOutput<ProfilerResult>>() {}.type
                                        )))
                                    }
                                }
                                RemoteInputType.RenderingInspectStart -> {
                                    launchingInspectJob?.cancel()
                                    launchingInspectJob = launch {
                                        val rootContainerSubject = PublishSubject.create<BukkitRootContainer>()
                                        BukkitRootContainerController.getRootContainerById(remoteInput.data as String)
                                            ?.let { bukkitRootContainer ->
                                                bukkitRootContainer.styleRenderResultObservable.subscribe {
                                                    rootContainerSubject.onNext(bukkitRootContainer)
                                                }
                                            }
                                        while (true) {
                                            val rootContainer = rootContainerSubject.awaitFirst()
                                            send(Frame.Text(gson.toJson(
                                                RemoteOutput(
                                                    type = RemoteOutputType.RenderingInspectFrame,
                                                    data = DebuggerRootContainer.from(rootContainer)
                                                ),
                                                object :
                                                    TypeToken<RemoteOutput<DebuggerRootContainer>>() {}.type
                                            )))
                                        }
                                    }
                                }
                                RemoteInputType.RenderingInspectStop -> launchingInspectJob?.cancel()
                            }
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    if (ProfilerDataChannel.currentProfilerResult != null) {
                        ProfilerDataChannel.stopProfiling()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onStart() {
        server.start(false)
    }

    fun onStop() {
        server.stop(100, 100)
    }
}
