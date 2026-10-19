@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.local.musicplayer.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.model.Track
import app.local.musicplayer.model.TrackId
import app.local.musicplayer.backup.BackupCodec
import app.local.musicplayer.backup.BackupData
import app.local.musicplayer.files.*
import app.local.musicplayer.widgets.PlayerWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable fun MusicPlayerApp(vm: PlayerViewModel) {
    val data by vm.collections.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val allTracks by vm.allTracks.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val playingArtwork by vm.playingArtwork.collectAsStateWithLifecycle()
    val library by vm.libraryState.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val navigation by vm.navigation.collectAsStateWithLifecycle()
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val fileOps=remember {FileOperations(context)}
    val editFileTags=rememberTagEditor(vm)
    var route by rememberSaveable {mutableStateOf("library")}
    var groupKind by rememberSaveable {mutableStateOf("")};var groupKey by rememberSaveable {mutableStateOf("")};var groupTitle by rememberSaveable {mutableStateOf("")}
    val group=if(groupKind.isEmpty()) null else BrowserGroup(groupKind,groupKey,groupTitle)
    var dialog by remember {mutableStateOf<String?>(null)}
    var actionTrack by remember {mutableStateOf<Track?>(null)}
    var playlistTracks by remember {mutableStateOf<List<Track>>(emptyList())}
    var restore by remember {mutableStateOf<BackupData?>(null)}
    var pendingDelete by remember {mutableStateOf<List<Track>>(emptyList())}
    var retryDelete by remember {mutableStateOf(false)}
    val currentUri=playback.current?.mediaMetadata?.extras?.getString("source_uri") ?: playback.current?.localConfiguration?.uri?.toString()
    val current=allTracks.find {it.id.uri==currentUri} ?: playback.current?.let {item-> Track(TrackId(currentUri.orEmpty()),item.mediaMetadata.title?.toString() ?: "Audio file",item.mediaMetadata.artist?.toString() ?: "Unknown artist",item.mediaMetadata.albumTitle?.toString().orEmpty(),null,"",playback.duration,null,0,item.mediaMetadata.artworkUri?.toString())}
    val drawer=rememberDrawerState(DrawerValue.Closed)
    val snackbar=remember {SnackbarHostState()}
    fun back() {if(route=="library") {groupKind=""} else route=if((route=="queue" || route=="playerThemes") && playback.current!=null) "player" else "library"}
    LaunchedEffect(navigation) {navigation?.let {route=it;vm.navigation.value=null}}
    fun navigate(destination: String) {route=destination}
    BackHandler(drawer.isOpen || route!="library" || group!=null) {if(drawer.isOpen) scope.launch {drawer.close()} else back()}
    LaunchedEffect(message) {message?.let {snackbar.showSnackbar(it);vm.message.value=null}}
    LaunchedEffect(playback.error) {playback.error?.let {snackbar.showSnackbar(it)}}
    DisposableEffect(route,data.flag("keep_screen")) {
        val window=(context as? Activity)?.window
        if(route=="player" && data.flag("keep_screen")) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
    }
    DisposableEffect(data.flag("lock_screen",true),playback.playing) {
        val activity=context as? Activity;val enabled=data.flag("lock_screen",true) && playback.playing
        if(Build.VERSION.SDK_INT>=27) activity?.setShowWhenLocked(enabled) else if(enabled) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED) else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        onDispose {if(Build.VERSION.SDK_INT>=27) activity?.setShowWhenLocked(false) else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)}
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {vm.rescan()}
    val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(playback.playing) {if(playback.playing && Build.VERSION.SDK_INT>=33 && !data.flag("notification_asked")) {vm.pref("notification_asked","true");notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)}}
    lateinit var continueDeletion: (List<Track>)->Unit
    val consent=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {result->
        val targets=pendingDelete;pendingDelete=emptyList()
        if(result.resultCode==Activity.RESULT_OK) {if(retryDelete) continueDeletion(targets) else {vm.rescan();vm.message.value="Deleted from device"}} else vm.message.value="Deletion cancelled"
    }
    val deletePermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->
        val targets=pendingDelete;pendingDelete=emptyList()
        if(granted) vm.work {fileOps.delete(targets);vm.rescan()} else vm.message.value="Storage write access is required to delete audio files"
    }
    fun delete(targets: List<Track>) {
        pendingDelete=targets
        if(WriteAccess.needsPermission(Build.VERSION.SDK_INT,context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)==android.content.pm.PackageManager.PERMISSION_GRANTED)) deletePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else vm.work {val request=fileOps.delete(targets);if(request!=null) {retryDelete=request.retry;consent.launch(IntentSenderRequest.Builder(request.sender).build())} else {pendingDelete=emptyList();vm.rescan()}}
    }
    continueDeletion=::delete
    val cover=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        if(uri!=null) actionTrack?.let {track->vm.work {
            context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val custom=JSONObject(data.overrides[track.id.uri] ?: "{}").put("cover",uri.toString());vm.repository.setOverride(track.id.uri,custom.toString())
        }}
    }
    val lyricsImport=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null && current!=null) vm.work {
        val text=withContext(Dispatchers.IO) {context.contentResolver.openInputStream(uri)?.use {it.readBounded(1_000_000).toString(Charsets.UTF_8)} ?: error("Unable to read lyrics")}
        vm.repository.setLyrics(current.id.uri,text)
    }}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {uri->if(uri!=null) vm.work {
        val text=BackupCodec.encode(vm.repository.export())
        withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri,"wt")?.use {it.write(text.toByteArray())} ?: error("Unable to write backup")}
        vm.message.value="Backup saved"
    }}
    val importBackup=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) vm.work {
        restore=withContext(Dispatchers.IO) {context.contentResolver.openInputStream(uri)?.use {BackupCodec.decode(it.readBounded(BackupCodec.MAX_BYTES).toString(Charsets.UTF_8))} ?: error("Unable to read backup")}
    }}
    MusicTheme(data.pref("theme","Midnight")) {
        val gradient=when(data.pref("theme")) {"Obsidian"->listOf(Color(0xFF080808),Color(0xFF19191D));"Ocean"->listOf(Color(0xFF061824),Color(0xFF123B45));"Sunset"->listOf(Color(0xFF261125),Color(0xFF472735));else->listOf(Ink,Color(0xFF17103F),Color(0xFF35134D))}
        val playerTheme=PlayerThemes.find(data.pref("player_theme","Artwork"))
        val playerCover=rememberCover(if(route=="player") playingArtwork else null)
        val background=if(route=="player") playerCover?.let {listOf(Color(it.colors[0]),Color(it.colors[1]))} ?: listOf(Color(playerTheme.top),Color(playerTheme.bottom)) else gradient
        ModalNavigationDrawer(drawerState=drawer,drawerContent={ModalDrawerSheet(drawerContainerColor=Ink,modifier=Modifier.width(300.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding()) {
                Icon(Icons.Rounded.GraphicEq,null,Modifier.padding(start=24.dp,top=24.dp).size(44.dp),tint=MaterialTheme.colorScheme.primary)
                Text("Music Player",Modifier.padding(24.dp),style=MaterialTheme.typography.headlineMedium)
                listOf(Triple("Library","library",Icons.Rounded.LibraryMusic),Triple("Settings","settings",Icons.Rounded.Settings),Triple("Equalizer","equalizer",Icons.Rounded.Tune),Triple("Sleep timer","timer",Icons.Rounded.Alarm),Triple("Skin theme","themes",Icons.Rounded.Palette),Triple("Widgets","widgets",Icons.Rounded.Widgets),Triple("Backup & restore","backup",Icons.Rounded.Backup),Triple("Music transfer","transfer",Icons.Rounded.PhonelinkRing)).forEach { (title,destination,icon)->
                    ActionRow(icon,title) {scope.launch {drawer.close()};if(destination=="timer") dialog="timer" else {route=destination;if(destination=="library") groupKind=""}}
                }
            }
        }}) {
            Scaffold(containerColor=Color.Transparent,contentColor=Color.White,snackbarHost={SnackbarHost(snackbar)},modifier=Modifier.background(Brush.verticalGradient(background)).statusBarsPadding(),bottomBar={if(route!="player" && playback.current!=null) CompactPlayer(vm,playback,{route="player"},{route="queue"}) else Spacer(Modifier.navigationBarsPadding())}) {padding->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when(route) {
                        "library"->if(group?.kind=="playlist") PlaylistDetail(vm,group,allTracks,data,{groupKind=""},{actionTrack=it;dialog="track"},{dialog="rename"},{dialog="playlistSongs"}) else LibraryScreen(vm,tracks,data,library,playback,group,{groupKind=""},{scope.launch {drawer.open()}},{next->if(next.kind=="settings") route="settings" else {groupKind=next.kind;groupKey=next.key;groupTitle=next.title}},{actionTrack=it;dialog="track"},{dialog="newPlaylist"},{playlistTracks=it;dialog="playlist"},{route="themes"},{permission.launch(vm.app.library.permission)},{pendingDelete=it;dialog="deleteGroup"})
                        "player"->MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(primary=Color(playerTheme.accent))) {FullPlayer(vm,playback,current,currentUri in data.favorites,data.lyrics[currentUri].orEmpty(),::back,{actionTrack=current;dialog="playerActions"},{route="queue"},{route="equalizer"},{dialog="timer"},{playlistTracks=listOfNotNull(current);dialog="playlist"},{dialog="lyrics"},{route="playerThemes"})}
                        "queue"->QueueScreen(vm,playback,::back)
                        "settings"->SettingsScreen(vm,data,::back,::navigate)
                        "scan"->ScanScreen(vm,data,::back,{permission.launch(vm.app.library.permission)})
                        "hidden"->HiddenScreen(vm,data,allTracks,::back)
                        "themes","playerThemes"->ThemeScreen(vm,data,::back,route=="playerThemes")
                        "equalizer"->EqualizerScreen(vm,data,::back)
                        "backup"->Column {Header("Backup & restore",::back);Text("Save playlists, favorites, hidden songs, library details, lyrics and preferences. Music files are not included. Restoring replaces these saved settings and collections.",Modifier.padding(20.dp),color=Muted);ActionRow(Icons.Rounded.UploadFile,"Create backup") {export.launch("MusicPlayer-backup.json")};ActionRow(Icons.Rounded.Restore,"Restore backup") {importBackup.launch(arrayOf("application/json","text/plain","application/octet-stream"))};ActionRow(Icons.Rounded.RestorePage,"Original file backups") {route="tagBackups"}}
                        "tagBackups"->TagBackupsScreen(vm,::back)
                        "transfer"->TransferScreen(vm,tracks,::back)
                        "widgets"->WidgetsScreen(vm,::back)
                    }
                }
            }
        }
        if(dialog=="track" || dialog=="playerActions") actionTrack?.let {track->
            ModalBottomSheet(onDismissRequest={dialog=null}) {
                Text(track.title,Modifier.padding(horizontal=24.dp),style=MaterialTheme.typography.titleLarge,maxLines=2)
                Text("${track.artist} • ${timeLabel(track.durationMs)}",Modifier.padding(horizontal=24.dp,vertical=8.dp),color=Muted)
                LazyColumn(Modifier.heightIn(max=480.dp).navigationBarsPadding()) {
                    val actions=listOf(Triple("Play next","next",Icons.Rounded.PlaylistPlay),Triple("Add to queue","queue",Icons.Rounded.QueueMusic),Triple("Add to playlist","playlist",Icons.Rounded.PlaylistAdd),Triple(if(track.id.uri in data.favorites) "Remove favorite" else "Favorite","favorite",Icons.Rounded.FavoriteBorder),Triple("Share","share",Icons.Rounded.Share),Triple("Set as ringtone","ringtone",Icons.Rounded.NotificationsActive),Triple("Change cover","cover",Icons.Rounded.Image),Triple("Edit library details","edit",Icons.Rounded.Edit),Triple("Go to album","album",Icons.Rounded.Album),Triple("Hide song","hide",Icons.Rounded.VisibilityOff),Triple("Delete from device","delete",Icons.Rounded.Delete),Triple("Speed","speed",Icons.Rounded.Speed),Triple("Playback settings","settings",Icons.Rounded.Settings),Triple("Details","details",Icons.Rounded.Info))
                    items(actions) { (title,action,icon)->ActionRow(icon,title) {
                        dialog=null
                        when(action) {
                            "next"->vm.enqueue(listOf(track),true);"queue"->vm.enqueue(listOf(track));"favorite"->vm.favorite(track)
                            "playlist"->{playlistTracks=listOf(track);dialog="playlist"}
                            "share"->runCatching {fileOps.share(track)}.onFailure {vm.message.value=it.message}
                            "ringtone"->runCatching {if(fileOps.ringtone(track)) vm.message.value="Ringtone set" else vm.message.value="Allow system settings access, then choose Set as ringtone again"}.onFailure {vm.message.value=it.message}
                            "cover"->cover.launch(arrayOf("image/*"));"edit"->dialog="edit"
                            "album"->{groupKind="album";groupKey=track.albumId?.toString() ?: track.album;groupTitle=track.album;route="library"}
                            "hide"->vm.work {vm.repository.setHidden(track.id.uri,true)}
                            "delete"->dialog="delete";"speed"->dialog="speed";"settings"->route="settings";"details"->dialog="details"
                        }
                    }}
                }
            }
        }
        when(dialog) {
            "deleteGroup"->AlertDialog(onDismissRequest={dialog=null},title={Text("Delete selected music?")},text={Text("${pendingDelete.size} audio files will be permanently deleted. Android may ask for confirmation.")},confirmButton={TextButton(onClick={val targets=pendingDelete;dialog=null;delete(targets)}) {Text("Delete")}},dismissButton={TextButton(onClick={dialog=null;pendingDelete=emptyList()}) {Text("Cancel")}})
            "timer"->TimerDialog(vm,playback,{dialog=null})
            "speed"->SpeedDialog(vm,playback,{dialog=null})
            "newPlaylist"->TextEntryDialog("New playlist","Playlist name","",{dialog=null}) {name->vm.work {val id=vm.repository.createPlaylist(name);if(playlistTracks.isNotEmpty()) {vm.repository.addToPlaylist(id,playlistTracks.map {it.id.uri});playlistTracks=emptyList()}};dialog=null}
            "rename"->TextEntryDialog("Rename playlist","Playlist name",groupTitle,{dialog=null}) {name->vm.work {vm.repository.renamePlaylist(groupKey,name)};groupTitle=name;dialog=null}
            "playlist"->AlertDialog(onDismissRequest={dialog=null},title={Text("Add to playlist")},text={LazyColumn {item {TextButton(onClick={dialog="newPlaylist"}) {Icon(Icons.Rounded.Add,null);Text("New playlist")}};items(data.playlists) {p->TextButton(onClick={vm.work {vm.repository.addToPlaylist(p.id,playlistTracks.map {it.id.uri});vm.message.value="Added to ${p.name}"};dialog=null;playlistTracks=emptyList()}) {Text(p.name)}}}},confirmButton={TextButton(onClick={dialog=null;playlistTracks=emptyList()}) {Text("Cancel")}})
            "playlistSongs"->SongPickerDialog(tracks,{dialog=null}) {chosen->vm.work {vm.repository.addToPlaylist(groupKey,chosen.map {it.id.uri})};dialog=null}
            "lyrics"->LyricsEditor(data.lyrics[currentUri].orEmpty(),{dialog=null},{dialog=null;lyricsImport.launch(arrayOf("text/*","application/octet-stream"))}) {text->current?.let {vm.work {vm.repository.setLyrics(it.id.uri,text)}};dialog=null}
            "edit"->actionTrack?.let {track->MetadataDialog(track,{dialog=null}) {title,artist,album,writeFile->if(writeFile) editFileTags(track,TagChange(title,artist,album)) else vm.work {val json=JSONObject(data.overrides[track.id.uri] ?: "{}").put("title",title).put("artist",artist).put("album",album);vm.repository.setOverride(track.id.uri,json.toString())};dialog=null}}
            "delete"->actionTrack?.let {track->AlertDialog(onDismissRequest={dialog=null},title={Text("Delete from device?")},text={Text("The music file “${track.title}” will be permanently deleted. Android may ask for confirmation.")},confirmButton={TextButton(onClick={dialog=null;delete(listOf(track))}) {Text("Delete")}},dismissButton={TextButton(onClick={dialog=null}) {Text("Cancel")}})}
            "details"->actionTrack?.let {track->AlertDialog(onDismissRequest={dialog=null},title={Text("Song details")},text={Column(Modifier.verticalScroll(rememberScrollState())) {Text("${track.title}\n${track.artist}\n${track.album}\n${timeLabel(track.durationMs)}\n${track.mimeType.orEmpty()}\n\n${track.folder}\n${track.id.uri}")}},confirmButton={TextButton(onClick={dialog=null}) {Text("Done")}})}
        }
        restore?.let {backup->AlertDialog(onDismissRequest={restore=null},title={Text("Restore backup?")},text={Text("Replace saved collections and preferences with ${backup.playlists.size} playlists, ${backup.favorites.size} favorites and ${backup.lyrics.size} lyrics entries? Source music files are preserved. Media from another device may need to be relinked.")},confirmButton={TextButton(onClick={vm.work {vm.repository.restore(backup);vm.message.value="Backup restored"};restore=null}) {Text("Restore")}},dismissButton={TextButton(onClick={restore=null}) {Text("Cancel")}})}
    }
}

@Composable private fun TextEntryDialog(title: String,label: String,initial: String,onDismiss: ()->Unit,onSave: (String)->Unit) {
    var text by remember {mutableStateOf(initial)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(text,{text=it.take(200)},label={Text(label)},singleLine=true)},confirmButton={TextButton(onClick={onSave(text.trim())},enabled=text.isNotBlank()) {Text("Save")}},dismissButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
@Composable private fun MetadataDialog(track: Track,onDismiss: ()->Unit,onSave: (String,String,String,Boolean)->Unit) {
    var title by remember {mutableStateOf(track.title)};var artist by remember {mutableStateOf(track.artist)};var album by remember {mutableStateOf(track.album)}
    val mp3=track.mimeType in listOf("audio/mpeg","audio/mp3")
    var writeFile by remember {mutableStateOf(mp3)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Edit tags")},text={Column {Text(if(writeFile) "Write MP3 tags after Android approval. An original-file backup is kept." else "Changes are saved inside this player.",color=Muted);OutlinedTextField(title,{title=it.take(10000)},label={Text("Title")});OutlinedTextField(artist,{artist=it.take(10000)},label={Text("Artist")});OutlinedTextField(album,{album=it.take(10000)},label={Text("Album")});if(mp3) Row(verticalAlignment=Alignment.CenterVertically) {Checkbox(writeFile,{writeFile=it});Text("Write tags to music file")}}},confirmButton={TextButton(onClick={onSave(title,artist,album,writeFile)},enabled=title.isNotBlank()) {Text("Save")}},dismissButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
@Composable private fun LyricsEditor(initial: String,onDismiss: ()->Unit,onImport: ()->Unit,onSave: (String)->Unit) {
    var text by remember {mutableStateOf(initial)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Lyrics")},text={Column {Text("Plain text or LRC timestamps, for example [00:12.50]Your lyric",color=Muted);OutlinedTextField(text,{text=it.take(1_000_000)},Modifier.fillMaxWidth().height(260.dp));TextButton(onClick=onImport) {Text("Import LRC / text file")}}},confirmButton={TextButton(onClick={onSave(text)}) {Text("Save")}},dismissButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
@Composable private fun SongPickerDialog(tracks: List<Track>,onDismiss: ()->Unit,onSave: (List<Track>)->Unit) {
    var chosen by remember {mutableStateOf(setOf<String>())}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Add songs")},text={LazyColumn(Modifier.heightIn(max=360.dp)) {items(tracks,key={it.id.uri}) {track->Row(Modifier.fillMaxWidth().clickable {chosen=if(track.id.uri in chosen) chosen-track.id.uri else chosen+track.id.uri},verticalAlignment=Alignment.CenterVertically) {Checkbox(track.id.uri in chosen,{checked->chosen=if(checked) chosen+track.id.uri else chosen-track.id.uri});Text(track.title,maxLines=1)}}}},confirmButton={TextButton(onClick={onSave(tracks.filter {it.id.uri in chosen})},enabled=chosen.isNotEmpty()) {Text("Add ${chosen.size}")}},dismissButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
