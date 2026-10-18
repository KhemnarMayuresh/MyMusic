package app.local.musicplayer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.model.Track
import app.local.musicplayer.transfer.TransferArchive
import kotlinx.coroutines.*

@Composable fun TransferScreen(vm: PlayerViewModel,tracks: List<Track>,onBack: ()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var selection by remember {mutableStateOf(setOf<String>())};var progress by remember {mutableStateOf<String?>(null)};var job by remember {mutableStateOf<Job?>(null)}
    val archive=remember {TransferArchive(context,vm.repository)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {uri->if(uri!=null) {
        val selected=tracks.filter {it.id.uri in selection}
        job=scope.launch {try {progress="Preparing music…";archive.export(uri,selected) {text->scope.launch {progress=text}};vm.message.value="Music package saved — share it with your other device"} catch(e: CancellationException) {vm.message.value="Transfer cancelled"} catch(e: Exception) {vm.message.value=e.message} finally {progress=null;job=null}}
    }}
    val importPackage=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) job=scope.launch {
        try {progress="Reading package…";val files=archive.importPackage(uri) {text->scope.launch {progress=text}};vm.rescan();vm.message.value="Imported ${files.size} songs"} catch(e: CancellationException) {vm.message.value="Import cancelled"} catch(e: Exception) {vm.message.value=e.message} finally {progress=null;job=null}
    }}
    Column {
        Header("Music transfer",{if(job==null) onBack() else vm.message.value="Cancel the transfer before leaving"})
        Text("Move songs and their playlists using a local ZIP package. Export here, copy or share the file to another device, then import it in Music Player. No server or account is needed.",Modifier.padding(20.dp),color=Muted)
        if(progress!=null) Column(Modifier.padding(20.dp)) {LinearProgressIndicator(Modifier.fillMaxWidth());Text(progress.orEmpty(),Modifier.padding(vertical=12.dp));TextButton(onClick={job?.cancel()}) {Text("Cancel")}}
        Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={export.launch("MusicPlayer-transfer.zip")},enabled=selection.isNotEmpty() && job==null) {Text("Export ${selection.size}")}
            OutlinedButton(onClick={importPackage.launch(arrayOf("application/zip","application/octet-stream"))},enabled=job==null) {Text("Import package")}
        }
        TextButton(onClick={selection=if(selection.isEmpty()) tracks.take(TransferArchive.MAX_TRACKS).map {it.id.uri}.toSet() else emptySet()},enabled=job==null,modifier=Modifier.padding(horizontal=12.dp)) {Text(if(selection.isEmpty()) "Select all" else "Clear selection")}
        LazyColumn {items(tracks,key={it.id.uri}) {track->Row(Modifier.fillMaxWidth().clickable(enabled=job==null) {selection=if(track.id.uri in selection) selection-track.id.uri else selection+track.id.uri}.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {Checkbox(track.id.uri in selection,{checked->selection=if(checked) selection+track.id.uri else selection-track.id.uri},enabled=job==null);Column(Modifier.padding(8.dp)) {Text(track.title,maxLines=1);Text(track.artist,color=Muted)}}}}
    }
}
