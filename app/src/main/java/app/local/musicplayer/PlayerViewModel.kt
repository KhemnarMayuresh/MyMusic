package app.local.musicplayer

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.local.musicplayer.library.LibraryState
import app.local.musicplayer.model.*
import app.local.musicplayer.playback.PlaybackConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import app.local.musicplayer.files.TagChange
import app.local.musicplayer.files.TagFileWriter

class PlayerViewModel(application: Application): AndroidViewModel(application) {
    val app=application as MusicApplication
    val repository=app.collections
    val collections=repository.state
    val libraryState=app.library.state
    val connection=PlaybackConnection(app,viewModelScope)
    val playback=connection.state
    private val external=MutableStateFlow<List<Track>>(emptyList())
    val allTracks=combine(libraryState,collections,external) { library,data,extra ->
        (((library as? LibraryState.Ready)?.tracks ?: emptyList())+extra).distinctBy { it.id }.map { track ->
            LibraryDetails.parse(data.overrides[track.id.uri])?.apply(track) ?: track
        }
    }.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val playingArtwork=combine(playback,collections) {state,data->
        val item=state.current
        val source=item?.mediaMetadata?.extras?.getString("source_uri") ?: item?.localConfiguration?.uri?.toString()
        LibraryDetails.parse(data.overrides[source])?.cover ?: item?.mediaMetadata?.artworkUri?.toString()
    }.stateIn(viewModelScope,SharingStarted.Eagerly,null)
    val tracks=combine(allTracks,collections) { all,data ->
        val minimum=data.pref("min_duration","0").toLongOrNull()?.coerceIn(0,3600) ?: 0
        val excluded=data.pref("excluded_folders").lines().filter { it.isNotBlank() }
        all.filter { it.id.uri !in data.hidden && it.durationMs>=minimum*1000 && excluded.none { folder -> it.folder.trimEnd('/' )==folder.trimEnd('/') || it.folder.startsWith(folder.trimEnd('/')+"/") } }
    }.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val navigation=MutableStateFlow<String?>(null)
    val message=MutableStateFlow<String?>(null)
    private var scanJob: Job?=null
    private val observer=object: ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) { rescan(300) } }
    init {
        app.contentResolver.registerContentObserver(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,true,observer)
        rescan()
    }
    fun rescan(debounceMs: Long=0) { scanJob?.cancel(); scanJob=viewModelScope.launch { delay(debounceMs); app.library.rescan() } }
    fun work(block: suspend ()->Unit) { viewModelScope.launch { try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) { message.value=e.message ?: "Unable to complete this action" } } }
    fun pref(key: String,value: String) { work { repository.setPreference(key,value) } }
    fun favorite(track: Track) { work { repository.setFavorite(track.id.uri,track.id.uri !in collections.value.favorites) } }
    fun play(list: List<Track>,start: Int=0,shuffle: Boolean?=null) { try { connection.play(list,start,shuffle) } catch(e: Exception) { message.value=e.message } }
    fun enqueue(list: List<Track>,next: Boolean=false) { try { connection.add(list,next); message.value=if(next) "Playing next" else "Added to queue" } catch(e: Exception) { message.value=e.message } }
    fun openExternal(uri: Uri) { work {
        val track=withContext(Dispatchers.IO) {
            val retriever=MediaMetadataRetriever()
            try {
                retriever.setDataSource(app,uri)
                Track(TrackId(uri.toString()),retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: uri.lastPathSegment ?: "Audio file",retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown artist",retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown album",null,"Opened file",retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,null,0,null)
            } finally { retriever.release() }
        }
        external.value=external.value+track
        while(!playback.value.connected) { delay(100) }
        play(listOf(track))
    } }
    suspend fun writeTags(track: Track,change: TagChange) {
        val controller=connection.controller
        val item=controller?.currentMediaItem
        val editingCurrent=item?.mediaMetadata?.extras?.getString("source_uri")==track.id.uri || item?.localConfiguration?.uri?.toString()==track.id.uri
        val position=if(editingCurrent) controller?.currentPosition ?: 0 else 0
        val resume=editingCurrent && controller?.isPlaying==true
        if(editingCurrent) {controller?.pause();controller?.stop()}
        try {
            TagFileWriter(app).write(track,change)
            val json=JSONObject(collections.value.overrides[track.id.uri] ?: "{}").put("title",change.title).put("artist",change.artist).put("album",change.album)
            repository.setOverride(track.id.uri,json.toString())
            if(editingCurrent && controller?.currentMediaItem?.mediaId==item?.mediaId && item!=null) controller?.replaceMediaItem(controller.currentMediaItemIndex,item.buildUpon().setUri(track.id.uri).setMediaMetadata(item.mediaMetadata.buildUpon().setTitle(change.title).setArtist(change.artist).setAlbumTitle(change.album).build()).build())
            rescan();message.value="File tags saved. The original is in Backup & restore → Original file backups."
        } finally {
            if(editingCurrent && controller?.currentMediaItem?.mediaId==item?.mediaId) {controller?.prepare();controller?.seekTo(position);if(resume) controller?.play()}
        }
    }
    override fun onCleared() { app.contentResolver.unregisterContentObserver(observer); connection.release(); super.onCleared() }
}
