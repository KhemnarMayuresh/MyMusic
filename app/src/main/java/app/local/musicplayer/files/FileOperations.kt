package app.local.musicplayer.files

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import app.local.musicplayer.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun InputStream.readBounded(maxBytes: Int): ByteArray {
    val result=ByteArrayOutputStream(minOf(maxBytes,8192)); val buffer=ByteArray(8192)
    while(true) {val count=read(buffer);if(count<0) break;require(count<=maxBytes-result.size()) {"File exceeds the supported size"};result.write(buffer,0,count)}
    return result.toByteArray()
}
class FileOperations(private val context: Context) {
    data class DeleteConsent(val sender: IntentSender,val retry: Boolean)
    suspend fun delete(tracks: List<Track>): DeleteConsent?=withContext(Dispatchers.IO) {
        val uris=tracks.map {Uri.parse(it.id.uri)}
        if(Build.VERSION.SDK_INT>=30) return@withContext DeleteConsent(MediaStore.createDeleteRequest(context.contentResolver,uris).intentSender,false)
        try {uris.forEach {context.contentResolver.delete(it,null,null)};null}
        catch(e: SecurityException) {if(Build.VERSION.SDK_INT>=29 && e is RecoverableSecurityException) DeleteConsent(e.userAction.actionIntent.intentSender,true) else throw e}
    }
    fun share(track: Track) {
        val uri=Uri.parse(track.id.uri)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(track.mimeType ?: "audio/*").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Share song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun ringtone(track: Track): Boolean {
        if(!Settings.System.canWrite(context)) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return false
        }
        RingtoneManager.setActualDefaultRingtoneUri(context,RingtoneManager.TYPE_RINGTONE,Uri.parse(track.id.uri));return true
    }
}
