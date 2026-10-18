package app.local.musicplayer.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import app.local.musicplayer.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

sealed interface LibraryState {
    data object PermissionRequired: LibraryState
    data object Loading: LibraryState
    data class Ready(val tracks: List<Track>): LibraryState
    data class Failed(val failure: OperationResult.Failure): LibraryState
}
class MediaLibrary(private val context: Context) {
    private val mutable=MutableStateFlow<LibraryState>(LibraryState.Loading)
    val state=mutable.asStateFlow()
    val permission get()=if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    fun hasPermission()=context.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED
    suspend fun rescan(): OperationResult<List<Track>> = withContext(Dispatchers.IO) {
        if(!hasPermission()) { mutable.value=LibraryState.PermissionRequired; return@withContext OperationResult.Failure(FailureReason.PERMISSION_REQUIRED,"Allow audio access to browse music on this device.") }
        mutable.value=LibraryState.Loading
        try {
            val uri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val pathColumn=if(Build.VERSION.SDK_INT>=29) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
            val columns=arrayOf("_id","title","artist","album","album_id","duration","mime_type","date_added",pathColumn)
            val tracks: List<Track> = context.contentResolver.query(uri,columns,"duration > 0 AND mime_type LIKE ?",arrayOf("audio/%"),"title COLLATE NOCASE ASC")?.use { c ->
                buildList<Track> {
                    while(c.moveToNext()) {
                        coroutineContext.ensureActive()
                        val id=c.getLong(0); val albumId: Long? = if(c.isNull(4)) null else c.getLong(4)
                        val path=c.getString(8).orEmpty()
                        val folder=if(Build.VERSION.SDK_INT>=29) path.trimEnd('/') else path.substringBeforeLast('/',"")
                        add(Track(TrackId(ContentUris.withAppendedId(uri,id).toString()),c.getString(1)?.takeIf { it.isNotBlank() } ?: "Untitled",
                            c.getString(2)?.takeUnless { it.isBlank() || it=="<unknown>" } ?: "Unknown artist",c.getString(3)?.takeUnless { it.isBlank() || it=="<unknown>" } ?: "Unknown album",
                            albumId,folder,c.getLong(5),c.getString(6),c.getLong(7),albumId?.takeIf { it>0 }?.let { "content://media/external/audio/albumart/$it" }))
                    }
                }
            } ?: throw java.io.IOException("Media library is temporarily unavailable")
            mutable.value=LibraryState.Ready(tracks)
            OperationResult.Success(tracks)
        } catch(e: CancellationException) { throw e
        } catch(e: SecurityException) {
            mutable.value=LibraryState.PermissionRequired
            OperationResult.Failure(FailureReason.PERMISSION_REQUIRED,"Audio access was revoked.")
        } catch(e: Exception) {
            val failure=OperationResult.Failure(FailureReason.MEDIA_UNAVAILABLE,e.message ?: "Unable to scan music")
            mutable.value=LibraryState.Failed(failure); failure
        }
    }
}
