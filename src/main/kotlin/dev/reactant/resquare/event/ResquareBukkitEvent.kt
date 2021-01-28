package dev.reactant.resquare.event

import org.bukkit.event.Event

abstract class ResquareBukkitEvent<out E : Event>(
    open val bukkitEvent: E
) : ResquareEvent()
