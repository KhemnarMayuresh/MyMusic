package app.local.musicplayer.playback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import org.json.JSONArray
import org.json.JSONObject

class QueueStore(context: Context) {
    private val prefs=context.getSharedPreferences("playback",Context.MODE_PRIVATE)
    data class Saved(val items: List<MediaItem>, val index: Int, val position: Long, val repeat: Int, val shuffle: Boolean, val speed: Float)
    fun save(player: Player) {
        val items=JSONArray()
        for(i in 0 until player.mediaItemCount.coerceAtMost(5000)) {
            val item=player.getMediaItemAt(i); val meta=item.mediaMetadata
            val source=meta.extras?.getString("source_uri") ?: item.localConfiguration?.uri?.toString() ?: continue
            items.put(JSONObject().put("id",item.mediaId).put("uri",source).put("title",meta.title?.toString()).put("artist",meta.artist?.toString()).put("album",meta.albumTitle?.toString()).put("artwork",meta.artworkUri?.toString()))
        }
        prefs.edit().putString("queue",items.toString()).putInt("index",player.currentMediaItemIndex.coerceAtLeast(0)).putLong("position",player.currentPosition.coerceAtLeast(0)).putInt("repeat",player.repeatMode).putBoolean("shuffle",player.shuffleModeEnabled).putFloat("speed",player.playbackParameters.speed).apply()
    }
    fun restore(): Saved? = try {
        val items=JSONArray(prefs.getString("queue","[]")); require(items.length()<=5000)
        val restored=(0 until items.length()).map { i ->
            val item=items.getJSONObject(i); val uri=item.getString("uri")
            MediaItem.Builder().setMediaId(item.getString("id")).setUri(uri).setMediaMetadata(MediaMetadata.Builder()
                .setTitle(item.optString("title","Untitled")).setArtist(item.optString("artist","Unknown artist")).setAlbumTitle(item.optString("album",""))
                .setArtworkUri(item.optString("artwork").takeUnless { it.isEmpty() || it=="null" }?.let(Uri::parse))
                .setExtras(Bundle().apply { putString("source_uri",uri) }).build()).build()
        }
        if(restored.isEmpty()) null else Saved(restored,prefs.getInt("index",0).coerceIn(restored.indices),prefs.getLong("position",0).coerceAtLeast(0),prefs.getInt("repeat",0).coerceIn(0,2),prefs.getBoolean("shuffle",false),prefs.getFloat("speed",1f).coerceIn(.5f,2f))
    } catch(_: Exception) { null }
}
