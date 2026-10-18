package app.local.musicplayer.ui

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestorePage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.local.musicplayer.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable fun TagBackupsScreen(vm: PlayerViewModel,onBack: ()->Unit) {
    val context=LocalContext.current
    var revision by remember {mutableIntStateOf(0)};var confirm by remember {mutableStateOf(false)}
    val files by produceState(emptyList<File>(),revision) {value=withContext(Dispatchers.IO) {File(context.filesDir,"tag-backups").listFiles()?.filter {it.extension=="mp3"}?.sortedByDescending {it.lastModified()} ?: emptyList()}}
    Column {
        Header("Original file backups",onBack)
        Text("Before writing MP3 tags, the original file is preserved here. Export a copy to keep it elsewhere or restore it with your file manager.",Modifier.padding(20.dp),color=Muted)
        TextButton(onClick={confirm=true},enabled=files.isNotEmpty(),modifier=Modifier.padding(horizontal=12.dp)) {Text("Remove stored backups")}
        LazyColumn {items(files,key={it.name}) {file->ActionRow(Icons.Rounded.RestorePage,"Original • ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(file.lastModified()))}","%.1f MB • tap to export".format(file.length()/1048576.0)) {
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("audio/mpeg").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Export original audio"))
        }}}
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Remove stored backups?")},text={Text("This removes the preserved originals in this player. Your current music files are kept.")},confirmButton={TextButton(onClick={confirm=false;vm.work {withContext(Dispatchers.IO) {files.forEach {file->check(file.delete()) {"Could not remove ${file.name}"};File(file.parentFile,file.nameWithoutExtension+".json").delete()}};revision++}}) {Text("Remove")}},dismissButton={TextButton(onClick={confirm=false}) {Text("Cancel")}})
}
