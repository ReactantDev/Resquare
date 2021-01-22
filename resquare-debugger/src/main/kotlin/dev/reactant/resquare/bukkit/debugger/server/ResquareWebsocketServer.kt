package dev.reactant.resquare.bukkit.debugger.server

import com.google.gson.Gson
import dev.reactant.resquare.bukkit.ResquareBukkit
import dev.reactant.resquare.bukkit.debugger.server.models.DebuggerRootContainer
import io.ktor.application.install
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.awaitFirst

// data class RenderInfo(
//     val
// )

object ResquareWebsocketServer {
    val gson = Gson()
    var server: NettyApplicationEngine = embeddedServer(Netty, port = 27465) {
        install(WebSockets)

        routing {
            webSocket("/") {
                try {
                    launch {
                        while (true) {
                            ResquareBukkit.instance.rootContainerController.rootContainers.lastOrNull()
                                ?.let { rootContainer ->
                                    rootContainer.styleRenderResultObservable.awaitFirst()
                                    send(Frame.Text(gson.toJson(
                                        DebuggerRootContainer.from(rootContainer), DebuggerRootContainer::class.java
                                    )))
                                }
                        }
                    }
                    while (true) {
                        val text = (incoming.receive() as Frame.Text).readText()
                        println(text)
                    }
                } catch (e: ClosedReceiveChannelException) {
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
