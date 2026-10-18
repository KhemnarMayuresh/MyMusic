package app.local.musicplayer.files

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.model.Track

@Composable fun rememberTagEditor(vm: PlayerViewModel): (Track,TagChange)->Unit {
    val context=LocalContext.current
    var pending by remember {mutableStateOf<Pair<Track,TagChange>?>(null)}
    val consent=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {result->
        val edit=pending;pending=null
        if(result.resultCode==Activity.RESULT_OK && edit!=null) vm.work {vm.writeTags(edit.first,edit.second)} else vm.message.value="Tag edit cancelled"
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->
        val edit=pending;pending=null
        if(granted && edit!=null) vm.work {vm.writeTags(edit.first,edit.second)} else vm.message.value="Storage write access is required to edit file tags"
    }
    return {track,change->
        pending=track to change
        when {
            Build.VERSION.SDK_INT>=30 -> runCatching {val request=MediaStore.createWriteRequest(context.contentResolver,listOf(Uri.parse(track.id.uri)));consent.launch(IntentSenderRequest.Builder(request.intentSender).build())}.onFailure {pending=null;vm.message.value=it.message}
            Build.VERSION.SDK_INT<=28 && context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED -> permission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else -> vm.work {
                try {vm.writeTags(track,change);pending=null}
                catch(e: SecurityException) {if(Build.VERSION.SDK_INT==29 && e is RecoverableSecurityException) consent.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()) else {pending=null;throw e}}
            }
        }
    }
}
