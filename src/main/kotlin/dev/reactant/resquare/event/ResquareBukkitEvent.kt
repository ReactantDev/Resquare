package dev.reactant.resquare.event

import org.bukkit.event.Event

abstract class ResquareBukkitEvent<out E : Event, out T : EventTarget>(
    open val bukkitEvent: E
) : ResquareEvent<T>()
