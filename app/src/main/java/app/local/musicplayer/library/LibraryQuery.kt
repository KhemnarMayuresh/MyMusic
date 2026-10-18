package app.local.musicplayer.library

import app.local.musicplayer.model.Track
import java.text.Normalizer

object LibraryQuery {
    fun browse(tracks: List<Track>,query: String,order: String?): List<Track> = filter(tracks,query).let {if(order==null) it else sort(it,order)}
    private fun key(value: String) = Normalizer.normalize(value,Normalizer.Form.NFD).replace("\\p{M}+".toRegex(),"").lowercase(java.util.Locale.ROOT)
    fun filter(tracks: List<Track>, query: String): List<Track> {
        val words = key(query.trim()).split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return if (words.isEmpty()) tracks else tracks.filter { track ->
            val haystack = key("${track.title} ${track.artist} ${track.album} ${track.folder}")
            words.all { it in haystack }
        }
    }
    fun sort(tracks: List<Track>, order: String): List<Track> = when(order) {
        "Newest" -> tracks.sortedByDescending { it.dateAddedSeconds }
        "Oldest" -> tracks.sortedBy { it.dateAddedSeconds }
        "Artist" -> tracks.sortedWith(compareBy({key(it.artist)},{key(it.title)}))
        "Duration" -> tracks.sortedByDescending { it.durationMs }
        "Title Z–A" -> tracks.sortedByDescending { key(it.title) }
        else -> tracks.sortedBy { key(it.title) }
    }
}
