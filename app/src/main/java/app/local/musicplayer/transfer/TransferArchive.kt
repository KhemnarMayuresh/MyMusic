package app.local.musicplayer.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import app.local.musicplayer.backup.*
import app.local.musicplayer.collections.CollectionRepository
import app.local.musicplayer.files.readBounded
import app.local.musicplayer.model.Track
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

class TransferArchive(private val context: Context,private val repository: CollectionRepository) {
    private val resolver=context.contentResolver
    companion object {const val MAX_TRACK_BYTES=128L*1024*1024;const val MAX_TOTAL_BYTES=512L*1024*1024;const val MAX_TRACKS=500}
    private fun hash(file: File): String {
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use {input->val buffer=ByteArray(65536);while(true) {val n=input.read(buffer);if(n<0) break;digest.update(buffer,0,n)}}
        return digest.digest().joinToString("") {"%02x".format(it)}
    }
    private suspend fun copy(input: java.io.InputStream,output: java.io.OutputStream,max: Long): Long {
        var count=0L;val buffer=ByteArray(65536)
        while(true) {coroutineContext.ensureActive();val n=input.read(buffer);if(n<0) break;require(n<=max-count) {"Transfer size limit exceeded"};output.write(buffer,0,n);count+=n}
        return count
    }
    suspend fun export(destination: Uri,tracks: List<Track>,progress: (String)->Unit) = withContext(Dispatchers.IO) {
        require(tracks.isNotEmpty() && tracks.size<=MAX_TRACKS) {"Select between 1 and $MAX_TRACKS songs"}
        val staging=File(context.cacheDir,"transfer-${UUID.randomUUID()}").apply {mkdirs()}
        try {
            var total=0L;val files=tracks.mapIndexed {index,track->
                progress("Preparing ${index+1}/${tracks.size}: ${track.title}")
                val file=File(staging,"$index.audio")
                val size=resolver.openInputStream(Uri.parse(track.id.uri))?.use {input->file.outputStream().use {copy(input,it,minOf(MAX_TRACK_BYTES,MAX_TOTAL_BYTES-total))}} ?: error("Cannot read ${track.title}")
                total+=size
                Triple(file,size,hash(file))
            }
            val selected=tracks.map {it.id.uri}.toSet()
            val backup=repository.export().let {b->b.copy(playlists=b.playlists.map {it.copy(tracks=it.tracks.filter(selected::contains))}.filter {it.tracks.isNotEmpty()},favorites=b.favorites.intersect(selected),hidden=b.hidden.intersect(selected),lyrics=b.lyrics.filterKeys(selected::contains),overrides=emptyMap())}
            val manifest=JSONObject().put("format","musicplayer-transfer-v1").put("backup",JSONObject(BackupCodec.encode(backup))).put("tracks",JSONArray().apply {
                tracks.forEachIndexed {index,track->val (_,size,digest)=files[index];put(JSONObject().put("entry","audio/$index").put("source",track.id.uri).put("title",track.title).put("mime",track.mimeType ?: "audio/mpeg").put("size",size).put("sha256",digest))}
            })
            resolver.openOutputStream(destination,"wt")?.use {output->ZipOutputStream(output).use {zip->
                zip.putNextEntry(ZipEntry("manifest.json"));zip.write(manifest.toString().toByteArray());zip.closeEntry()
                files.forEachIndexed {index,(file,_,_)->progress("Exporting ${index+1}/${files.size}");zip.putNextEntry(ZipEntry("audio/$index"));file.inputStream().use {copy(it,zip,MAX_TRACK_BYTES)};zip.closeEntry()}
            }} ?: error("Cannot write transfer package")
        } catch(e: Exception) {
            withContext(NonCancellable) {runCatching {resolver.delete(destination,null,null)}}
            throw e
        } finally {staging.deleteRecursively()}
    }
    suspend fun importPackage(source: Uri,progress: (String)->Unit): List<String> = withContext(Dispatchers.IO) {
        require(Build.VERSION.SDK_INT>=29) {"Music-package import requires Android 10 or newer. Older devices can copy music using their file manager, then scan."}
        val created=mutableListOf<Uri>();var committed=false
        val staging=File(context.cacheDir,"import-${UUID.randomUUID()}").apply {mkdirs()}
        try {
            val mapped=mutableMapOf<String,String>();var backup: BackupData?=null
            resolver.openInputStream(source)?.use {input->ZipInputStream(input).use {zip->
                require(zip.nextEntry?.name=="manifest.json") {"Not a Music Player transfer package"}
                val manifest=JSONObject(zip.readBounded(BackupCodec.MAX_BYTES).toString(Charsets.UTF_8));zip.closeEntry()
                require(manifest.getString("format")=="musicplayer-transfer-v1") {"Unsupported transfer format"}
                backup=BackupCodec.decode(manifest.getJSONObject("backup").toString())
                val tracks=manifest.getJSONArray("tracks");require(tracks.length() in 1..MAX_TRACKS)
                var total=0L
                for(index in 0 until tracks.length()) {
                    coroutineContext.ensureActive()
                    val track=tracks.getJSONObject(index);require(track.getString("entry")=="audio/$index")
                    val size=track.getLong("size");require(size in 1..MAX_TRACK_BYTES && size<=MAX_TOTAL_BYTES-total);total+=size
                    require(zip.nextEntry?.name=="audio/$index") {"Transfer entry missing or reordered"}
                    progress("Importing ${index+1}/${tracks.length()}")
                    val file=File(staging,"$index.audio");val actual=file.outputStream().use {copy(zip,it,size)};zip.closeEntry()
                    require(actual==size && hash(file)==track.getString("sha256")) {"Audio integrity check failed"}
                    val mime=track.getString("mime");require(mime.startsWith("audio/") && mime.length<=100)
                    val extension=when(mime) {"audio/wav","audio/x-wav"->"wav";"audio/flac"->"flac";"audio/mp4","audio/aac"->"m4a";"audio/ogg"->"ogg";else->"mp3"}
                    val title=track.getString("title").replace(Regex("[\\p{Cntrl}/\\\\]"),"_").take(100).ifBlank {"Song"}
                    val uri=resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,ContentValues().apply {put(MediaStore.Audio.Media.DISPLAY_NAME,"$title.$extension");put(MediaStore.Audio.Media.MIME_TYPE,mime);put(MediaStore.Audio.Media.RELATIVE_PATH,"Music/MusicPlayer Transfers/");put(MediaStore.Audio.Media.IS_PENDING,1)}) ?: error("Cannot create imported song")
                    created+=uri
                    resolver.openOutputStream(uri)?.use {out->file.inputStream().use {copy(it,out,size)}} ?: error("Cannot write imported song")
                    resolver.update(uri,ContentValues().apply {put(MediaStore.Audio.Media.IS_PENDING,0)},null,null)
                    mapped[track.getString("source")]=uri.toString()
                    file.delete()
                }
                require(zip.nextEntry==null) {"Unexpected transfer entries"}
            }} ?: error("Cannot read transfer package")
            val incoming=backup ?: error("Missing collection data");val existing=repository.export()
            val merged=BackupData(existing.playlists+incoming.playlists.map {p->BackupPlaylist(UUID.randomUUID().toString(),p.name,p.tracks.mapNotNull(mapped::get))},existing.favorites+incoming.favorites.mapNotNull(mapped::get),existing.hidden+incoming.hidden.mapNotNull(mapped::get),existing.lyrics+incoming.lyrics.mapNotNull {(uri,text)->mapped[uri]?.let {it to text}}.toMap(),existing.preferences,existing.overrides)
            withContext(NonCancellable) {repository.restore(merged);committed=true}
            created.map(Uri::toString)
        } finally {
            if(!committed) withContext(NonCancellable) {
                val failed=created.filter {uri->runCatching {resolver.delete(uri,null,null)>0}.getOrDefault(false).not()}
                if(failed.isNotEmpty()) File(context.filesDir,"transfer-cleanup.json").appendText(JSONArray(failed.map(Uri::toString)).toString()+"\n")
            }
            staging.deleteRecursively()
        }
    }
}
