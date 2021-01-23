package dev.reactant.resquare.bukkit.debugger.server

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

val gson = Gson()
fun parseDebugInput(raw: String): RemoteInput<*> {
    val rawObject = JsonParser().parse(raw).asJsonObject
    val type = RemoteInputType.valueOf(rawObject.get("type").asString)
    val data = rawObject.get("data")
    val typeToken: TypeToken<*> = when (type) {
        RemoteInputType.ProfilingStart -> object : TypeToken<RemoteInput<String>>() {}
        RemoteInputType.ProfilingStop -> object : TypeToken<RemoteInput<Any>>() {}
        RemoteInputType.RenderingInspectStart -> object : TypeToken<RemoteInput<String>>() {}
        RemoteInputType.RenderingInspectStop -> object : TypeToken<RemoteInput<String>>() {}
        else -> throw IllegalArgumentException()
    }

    return gson.fromJson(raw, typeToken.type)
}
