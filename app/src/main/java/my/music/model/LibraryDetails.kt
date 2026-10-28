package my.music.model

import org.json.JSONObject

/** Parsed app-local overrides shared by the library and playing artwork selection. */
data class LibraryDetails(val title:String?,val artist:String?,val album:String?,val cover:String?) {
    fun apply(track:Track)=track.copy(title=title ?: track.title,artist=artist ?: track.artist,album=album ?: track.album,artworkUri=cover ?: track.artworkUri)
    companion object {
        fun parse(raw:String?):LibraryDetails? = raw?.let {text->runCatching {
            val json=JSONObject(text)
            fun field(key:String)=if(json.isNull(key)) null else json.optString(key).takeIf {it.isNotBlank()}
            LibraryDetails(field("title"),field("artist"),field("album"),field("cover"))
        }.getOrNull()}
    }
}
