@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class,androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.local.musicplayer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.collections.CollectionState
import app.local.musicplayer.library.LibraryQuery
import app.local.musicplayer.library.LibraryState
import app.local.musicplayer.model.Track
import app.local.musicplayer.playback.PlaybackSnapshot

data class BrowserGroup(val kind: String,val key: String,val title: String)

@Composable fun Header(title: String,onBack: ()->Unit,actions: @Composable RowScope.()->Unit={}) {
    Row(Modifier.fillMaxWidth().heightIn(min=64.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Back") }
        Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
        actions()
    }
}
@Composable fun ActionRow(icon: ImageVector,title: String,subtitle: String?=null,onClick: ()->Unit) {
    ListItem(headlineContent={Text(title)},supportingContent=subtitle?.let { {Text(it)} },leadingContent={Icon(icon,null,tint=MaterialTheme.colorScheme.primary)},modifier=Modifier.clickable(onClick=onClick),colors=ListItemDefaults.colors(containerColor=Color.Transparent))
}
@Composable fun TrackRow(track: Track,active: Boolean,onClick: ()->Unit,onMenu: ()->Unit,onLongClick: ()->Unit={},selected: Boolean=false,trailing: @Composable (() -> Unit)?=null) {
    Row(Modifier.fillMaxWidth().combinedClickable(onClick=onClick,onLongClick=onLongClick).padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
        if(selected) Icon(Icons.Rounded.CheckCircle,null,Modifier.size(52.dp),tint=MaterialTheme.colorScheme.primary) else Artwork(track.artworkUri,Modifier.size(52.dp))
        Column(Modifier.weight(1f).padding(start=14.dp)) {
            Text(track.title,color=if(active) MaterialTheme.colorScheme.primary else Color.White,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleMedium)
            Text(track.artist,maxLines=1,overflow=TextOverflow.Ellipsis,color=Muted,style=MaterialTheme.typography.bodyMedium)
        }
        if(trailing!=null) trailing() else {
            Text(timeLabel(track.durationMs),color=Muted,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(start=6.dp))
            IconButton(onClick=onMenu,modifier=Modifier.size(44.dp)) { Icon(Icons.Rounded.MoreVert,"Song actions",tint=Muted) }
        }
    }
}
@Composable fun LibraryScreen(vm: PlayerViewModel,tracks: List<Track>,data: CollectionState,libraryState: LibraryState,playback: PlaybackSnapshot,group: BrowserGroup?,onBack: ()->Unit,onMenu: ()->Unit,onGroup: (BrowserGroup)->Unit,onTrackMenu: (Track)->Unit,onNewPlaylist: ()->Unit,onAddSelection: (List<Track>)->Unit,onTheme: ()->Unit,onPermission: ()->Unit,onDeleteSelection: (List<Track>)->Unit) {
    var tab by rememberSaveable { mutableStateOf("Songs") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    val sort=data.pref("sort","Title A–Z")
    val groupTracks=when(group?.kind) {
        "folder"->tracks.filter { it.folder==group.key }
        "album"->tracks.filter { (it.albumId?.toString() ?: it.album)==group.key }
        "artist"->tracks.filter { it.artist==group.key }
        "favorites"->tracks.filter { it.id.uri in data.favorites }
        "recent"->data.history.mapNotNull { record->tracks.find { it.id.uri==record.trackUri } }
        "most"->data.history.sortedByDescending { it.count }.mapNotNull { record->tracks.find { it.id.uri==record.trackUri } }
        else->tracks
    }
    val ranked=group?.kind in listOf("recent","most")
    val displayed=remember(groupTracks,query,sort,ranked) { LibraryQuery.browse(groupTracks,query,if(ranked) null else sort) }
    Column(Modifier.fillMaxSize()) {
        if(group!=null) Header(group.title,onBack) else Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onMenu) { Icon(Icons.Rounded.Menu,"Open menu") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick=onTheme) { Icon(Icons.Rounded.Palette,"Themes") }
            IconButton(onClick={searching=!searching;if(!searching) query=""}) { Icon(Icons.Rounded.Search,"Search") }
            IconButton(onClick={onGroup(BrowserGroup("settings","","Settings"))}) { Icon(Icons.Rounded.Settings,"Settings") }
        }
        if(searching || group!=null) {
            OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=4.dp),singleLine=true,placeholder={Text("Search songs, artists, albums")},leadingIcon={Icon(Icons.Rounded.Search,null)},shape=RoundedCornerShape(24.dp))
        }
        if(group==null && !searching) ScrollableTabRow(selectedTabIndex=listOf("Songs","Playlists","Folders","Albums","Artists").indexOf(tab),containerColor=Color.Transparent,edgePadding=8.dp,divider={}) {
            listOf("Songs","Playlists","Folders","Albums","Artists").forEach { label -> Tab(selected=tab==label,onClick={tab=label;selection=emptySet()},text={Text(label,fontWeight=if(tab==label) FontWeight.Bold else FontWeight.Normal,style=MaterialTheme.typography.titleLarge)}) }
        }
        if(libraryState is LibraryState.PermissionRequired) {
            EmptyLibrary("Your music, on your device","Allow audio access to discover your songs.","Allow audio access",onPermission)
        } else if(libraryState is LibraryState.Failed) {
            EmptyLibrary("Can't read music",libraryState.failure.message,"Try again",{vm.rescan()})
        } else if(libraryState is LibraryState.Loading && tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
        } else if(group!=null || tab=="Songs" || searching) {
            Row(Modifier.fillMaxWidth().padding(start=16.dp,end=8.dp,top=14.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(if(selection.isEmpty()) "${displayed.size} ${if(displayed.size==1) "Song" else "Songs"}" else "${selection.size} selected",color=Muted,modifier=Modifier.weight(1f))
                if(!ranked) Box {
                    IconButton(onClick={sortMenu=true}) { Icon(Icons.Rounded.Sort,"Sort songs",tint=Muted) }
                    DropdownMenu(sortMenu,{sortMenu=false}) { listOf("Title A–Z","Title Z–A","Newest","Oldest","Artist","Duration").forEach { order -> DropdownMenuItem(text={Text(order)},onClick={vm.pref("sort",order);sortMenu=false}) } }
                }
                IconButton(onClick={selection=if(selection.isEmpty()) displayed.map { it.id.uri }.toSet() else emptySet()}) { Icon(Icons.Rounded.Checklist,"Select songs",tint=Muted) }
            }
            if(selection.isNotEmpty()) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                TextButton(onClick={onAddSelection(displayed.filter { it.id.uri in selection })}) { Text("Add to playlist") }
                TextButton(onClick={vm.enqueue(displayed.filter { it.id.uri in selection });selection=emptySet()}) { Text("Add to queue") }
                TextButton(onClick={selection=emptySet()}) { Text("Cancel") }
            } else Row(Modifier.padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Button(onClick={vm.play(displayed,shuffle=true)},enabled=displayed.isNotEmpty(),modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Ink)) { Icon(Icons.Rounded.Shuffle,null);Spacer(Modifier.width(8.dp));Text("Shuffle") }
                Button(onClick={vm.play(displayed)},enabled=displayed.isNotEmpty(),modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Ink)) { Icon(Icons.Rounded.PlayArrow,null);Spacer(Modifier.width(8.dp));Text("Play") }
            }
            if(displayed.isEmpty()) EmptyLibrary(if(query.isBlank()) "No music yet" else "No matching songs",if(query.isBlank()) "Add local audio files, then scan your music." else "Try another title, artist or album.","Scan music",{vm.rescan()})
            else LazyColumn(Modifier.weight(1f)) { itemsIndexed(displayed,key={index,track->"$index:${track.id.uri}"}) { index,track ->
                TrackRow(track,playback.current?.mediaMetadata?.extras?.getString("source_uri")==track.id.uri,onClick={if(selection.isEmpty()) vm.play(displayed,index) else selection=if(track.id.uri in selection) selection-track.id.uri else selection+track.id.uri},onMenu={onTrackMenu(track)},onLongClick={selection=selection+track.id.uri},selected=track.id.uri in selection)
            } }
        } else if(tab=="Playlists") {
            LazyColumn {
                item { ActionRow(Icons.Rounded.Favorite,"Favorites","${data.favorites.size} songs") { onGroup(BrowserGroup("favorites","","Favorites")) } }
                item { ActionRow(Icons.Rounded.History,"Recently played") { onGroup(BrowserGroup("recent","","Recently played")) } }
                item { ActionRow(Icons.Rounded.TrendingUp,"Most played") { onGroup(BrowserGroup("most","","Most played")) } }
                item { ActionRow(Icons.Rounded.PlaylistAdd,"New playlist",onClick=onNewPlaylist) }
                items(data.playlists,key={it.id}) { p -> ActionRow(Icons.Rounded.QueueMusic,p.name,"${p.entries.size} songs") { onGroup(BrowserGroup("playlist",p.id,p.name)) } }
            }
        } else {
            GroupBrowser(vm,tracks,when(tab) {"Folders"->"folder";"Albums"->"album";else->"artist"},onGroup,onAddSelection,onDeleteSelection)
        }
    }
}
@Composable private fun EmptyLibrary(title: String,subtitle: String,button: String,onClick: ()->Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Icon(Icons.Rounded.LibraryMusic,null,Modifier.size(76.dp),tint=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp));Text(title,style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(8.dp));Text(subtitle,color=Muted)
        Spacer(Modifier.height(24.dp));Button(onClick=onClick) { Text(button) }
    }
}
@Composable fun PlaylistDetail(vm: PlayerViewModel,group: BrowserGroup,tracks: List<Track>,data: CollectionState,onBack: ()->Unit,onTrackMenu: (Track)->Unit,onRename: ()->Unit,onAdd: ()->Unit) {
    val playlist=data.playlists.find { it.id==group.key }
    var options by remember { mutableStateOf(false) }
    Column {
        Header(playlist?.name ?: group.title,onBack) {
            IconButton(onClick=onAdd) { Icon(Icons.Rounded.Add,"Add songs") }
            Box { IconButton(onClick={options=true}) { Icon(Icons.Rounded.MoreVert,"Playlist actions") }; DropdownMenu(options,{options=false}) {
                DropdownMenuItem(text={Text("Rename")},onClick={options=false;onRename()})
                DropdownMenuItem(text={Text("Delete playlist")},onClick={options=false;vm.work { vm.repository.deletePlaylist(group.key) };onBack()})
            } }
        }
        val playable=playlist?.entries?.mapNotNull { entry->tracks.find { it.id.uri==entry.trackUri } } ?: emptyList()
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Button(onClick={vm.play(playable)},enabled=playable.isNotEmpty()) { Icon(Icons.Rounded.PlayArrow,null);Text("Play") }
            OutlinedButton(onClick={vm.play(playable,shuffle=true)},enabled=playable.isNotEmpty()) { Text("Shuffle") }
        }
        LazyColumn { itemsIndexed(playlist?.entries ?: emptyList(),key={_,entry->entry.id}) { index,entry ->
            val track=tracks.find { it.id.uri==entry.trackUri }
            if(track==null) ListItem(headlineContent={Text("Unavailable song")},supportingContent={Text(entry.trackUri,maxLines=1)},trailingContent={IconButton(onClick={vm.work { vm.repository.removePlaylistEntry(entry.id) }}) {Icon(Icons.Rounded.Close,"Remove from playlist")}},colors=ListItemDefaults.colors(containerColor=Color.Transparent))
            else TrackRow(track,false,{app.local.musicplayer.collections.PlaylistPlayback.plan(playlist?.entries.orEmpty(),tracks,entry.id)?.let {vm.play(it.tracks,it.startIndex)}},{onTrackMenu(track)},trailing={
                IconButton(onClick={if(index>0) vm.work { vm.repository.movePlaylistEntry(group.key,index,index-1) }},enabled=index>0) {Icon(Icons.Rounded.KeyboardArrowUp,"Move up")}
                IconButton(onClick={vm.work { vm.repository.removePlaylistEntry(entry.id) }}) {Icon(Icons.Rounded.Close,"Remove from playlist")}
            })
        } }
    }
}
