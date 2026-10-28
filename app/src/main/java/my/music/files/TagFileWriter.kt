package my.music.files

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import my.music.model.Track
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

data class TagChange(val title: String,val artist: String,val album: String)
class TagFileWriter(private val context: Context) {
    suspend fun write(track: Track,change: TagChange): File = withContext(Dispatchers.IO) {
        val backups=File(context.filesDir,"tag-backups").apply {mkdirs()}
        val retained=backups.listFiles()?.filter {it.extension=="mp3"}?.sumOf {it.length()} ?: 0
        require(retained<256L*1024*1024) {"Original tag backups use 256 MB. Export or remove old backups before editing more files."}
        val id=UUID.randomUUID().toString();val backup=File(backups,"$id.mp3");val staged=File(context.cacheDir,"$id-tags.mp3")
        var touched=false;var success=false
        try {
            val descriptor=context.contentResolver.openFileDescriptor(Uri.parse(track.id.uri),"rw") ?: error("Cannot open audio for editing")
            descriptor.use {master->
                val limit=128L*1024*1024
                require(master.statSize in 10..limit) {"File tag editing supports seekable MP3 files up to 128 MB"}
                require(context.filesDir.usableSpace>master.statSize*2+16*1024*1024) {"Not enough space to preserve the original file"}
                ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(master.fileDescriptor)).use {input->backup.outputStream().use {out->
                    val buffer=ByteArray(65536);var copied=0L
                    while(true) {coroutineContext.ensureActive();val n=input.read(buffer);if(n<0) break;require(n<=limit-copied);out.write(buffer,0,n);copied+=n};out.fd.sync()
                }}
                Id3Editor.rewrite(backup,staged,change.title,change.artist,change.album)
                coroutineContext.ensureActive()
                withContext(NonCancellable) {
                    try {
                        ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.dup(master.fileDescriptor)).use {out->
                            out.channel.position(0);touched=true;out.channel.truncate(0);staged.inputStream().use {it.copyTo(out)};out.fd.sync()
                        }
                        success=true
                    } catch(e: Exception) {
                        val restored=runCatching {ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.dup(master.fileDescriptor)).use {out->out.channel.position(0);out.channel.truncate(0);backup.inputStream().use {it.copyTo(out)};out.fd.sync()}}.isSuccess
                        throw java.io.IOException(if(restored) "Could not save tags; original file was restored" else "Could not save or restore tags. Original retained at ${backup.absolutePath}",e)
                    }
                }
            }
            File(backups,"$id.json").writeText(JSONObject().put("title",track.title).put("uri",track.id.uri).put("created",System.currentTimeMillis()).toString())
            val path=runCatching {context.contentResolver.query(Uri.parse(track.id.uri),arrayOf(android.provider.MediaStore.Audio.Media.DATA),null,null,null)?.use {if(it.moveToFirst()) it.getString(0) else null}}.getOrNull()
            if(path!=null) android.media.MediaScannerConnection.scanFile(context,arrayOf(path),arrayOf(track.mimeType),null)
            context.contentResolver.notifyChange(Uri.parse(track.id.uri),null)
            backup
        } finally {staged.delete();if(!touched && !success) backup.delete()}
    }
}
