package app.local.musicplayer.playback

import app.local.musicplayer.model.QueueEntry

data class QueueState(val entries: List<QueueEntry>, val activeEntryId: String?) {
    init {
        require(entries.map { it.entryId }.distinct().size == entries.size) { "Queue entry IDs must be unique" }
        require(activeEntryId == null || entries.any { it.entryId == activeEntryId })
    }
    fun move(from: Int, to: Int): QueueState {
        require(from in entries.indices && to in entries.indices)
        val items = entries.toMutableList(); items.add(to,items.removeAt(from))
        return copy(entries=items)
    }
    fun remove(entryId: String): QueueState {
        val index = entries.indexOfFirst { it.entryId == entryId }
        if(index < 0) return this
        val rest = entries.filterNot { it.entryId == entryId }
        val active = if(activeEntryId != entryId) activeEntryId else rest.getOrNull(index.coerceAtMost(rest.lastIndex).coerceAtLeast(0))?.entryId
        return QueueState(rest,active)
    }
}
