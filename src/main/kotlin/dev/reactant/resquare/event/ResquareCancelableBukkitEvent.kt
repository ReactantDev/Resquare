package dev.reactant.resquare.event

import org.bukkit.event.Cancellable
import org.bukkit.event.Event

abstract class ResquareCancelableBukkitEvent<out E : Event>(
    bukkitEvent: E
) : ResquareBukkitEvent<E>(bukkitEvent), ResquareCancelableEvent {
    override var defaultPrevented: Boolean
        get() = (bukkitEvent as Cancellable).isCancelled
        set(value) {
            (bukkitEvent as Cancellable).isCancelled = value
        }

    override fun preventDefault() {
        this.defaultPrevented = true
    }
}
