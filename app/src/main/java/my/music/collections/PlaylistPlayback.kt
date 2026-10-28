package my.music.collections

import my.music.model.Track

object PlaylistPlayback {
    data class Plan(val tracks: List<Track>,val startIndex: Int)
    fun plan(entries: List<PlaylistEntry>,tracks: List<Track>,selectedEntryId: String): Plan? {
        val available=tracks.associateBy {it.id.uri}
        val playable=entries.mapNotNull {entry->available[entry.trackUri]?.let {entry.id to it}}
        val index=playable.indexOfFirst {it.first==selectedEntryId}
        return if(index<0) null else Plan(playable.map {it.second},index)
    }
}
