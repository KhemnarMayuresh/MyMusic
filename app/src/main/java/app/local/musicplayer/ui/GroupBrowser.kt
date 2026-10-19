@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class,androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.local.musicplayer.ui
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.library.GroupLibrary
import app.local.musicplayer.model.Track

@Composable fun GroupBrowser(vm:PlayerViewModel,tracks:List<Track>,kind:String,onGroup:(BrowserGroup)->Unit,onPlaylist:(List<Track>)->Unit,onDelete:(List<Track>)->Unit) {
 val data by vm.collections.collectAsState()
 val order=data.pref("group_sort_$kind","Name A–Z")
 var selected by remember(kind) {mutableStateOf(setOf<String>())}
 var sorting by remember {mutableStateOf(false)}
 val groups=remember(tracks,kind,order) {GroupLibrary.groups(tracks,kind,order)}
 LaunchedEffect(groups) {selected=selected.intersect(groups.map {it.key}.toSet())}
 val chosen=GroupLibrary.selected(groups,selected)
 val noun=when(kind) {"folder"->"folder";"album"->"album";else->"artist"}
 Column {
  Row(Modifier.fillMaxWidth().padding(start=20.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically) {
   Text(if(selected.isEmpty()) "${groups.size} ${noun}s" else "${selected.size} selected",Modifier.weight(1f),color=Muted)
   Box {IconButton(onClick={sorting=true}) {Icon(Icons.Rounded.Sort,"Sort $noun"+"s")};DropdownMenu(sorting,{sorting=false}) {listOf("Name A–Z","Name Z–A","Song count","Newest").forEach {label->DropdownMenuItem(text={Text(label)},onClick={vm.pref("group_sort_$kind",label);sorting=false})}}}
   IconButton(onClick={selected=if(selected.size==groups.size) emptySet() else groups.map {it.key}.toSet()}) {Icon(Icons.Rounded.Checklist,"Select $noun"+"s")}
  }
  if(selected.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState())) {
   TextButton(onClick={vm.play(chosen)}) {Text("Play")}
   TextButton(onClick={vm.enqueue(chosen)}) {Text("Add to queue")}
   TextButton(onClick={onPlaylist(chosen)}) {Text("Add to playlist")}
   TextButton(onClick={onDelete(chosen);selected=emptySet()}) {Text("Delete")}
   TextButton(onClick={selected=emptySet()}) {Text("Cancel")}
  }
  LazyColumn {items(groups,key={it.key}) {group->
   var menu by remember {mutableStateOf(false)}
   Row(Modifier.fillMaxWidth().combinedClickable(onClick={if(selected.isEmpty()) onGroup(BrowserGroup(kind,group.key,group.title)) else selected=if(group.key in selected) selected-group.key else selected+group.key},onLongClick={selected=selected+group.key}).padding(start=16.dp,top=12.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically) {
    if(group.key in selected) Icon(Icons.Rounded.CheckCircle,null,Modifier.size(52.dp),tint=MaterialTheme.colorScheme.primary)
    else if(kind=="album") Artwork(group.tracks.firstOrNull()?.artworkUri,Modifier.size(52.dp))
    else Icon(if(kind=="folder") Icons.Rounded.Folder else Icons.Rounded.Person,null,Modifier.size(52.dp),tint=MaterialTheme.colorScheme.primary)
    Column(Modifier.weight(1f).padding(start=16.dp)) {Text(group.title,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${group.tracks.size} songs",color=Muted,style=MaterialTheme.typography.bodyMedium)}
    Box {IconButton(onClick={menu=true}) {Icon(Icons.Rounded.MoreVert,"$noun actions: ${group.title}")};if(menu) ModalBottomSheet(onDismissRequest={menu=false}) {
     Text(group.title,Modifier.padding(horizontal=24.dp),style=MaterialTheme.typography.titleLarge);Text("${group.tracks.size} songs",Modifier.padding(horizontal=24.dp,vertical=8.dp),color=Muted)
     listOf("Play","Play next","Add to queue","Add to playlist","Hide $noun").forEach {label->DropdownMenuItem(text={Text(label)},onClick={menu=false;when(label) {"Play"->vm.play(group.tracks);"Play next"->vm.enqueue(group.tracks,true);"Add to queue"->vm.enqueue(group.tracks);"Add to playlist"->onPlaylist(group.tracks);else->vm.work {if(kind=="folder") {val old=vm.collections.value.pref("excluded_folders").lines().filter {it.isNotBlank()};vm.repository.setPreference("excluded_folders",(old+group.key).distinct().joinToString("\n"))} else group.tracks.forEach {vm.repository.setHidden(it.id.uri,true)}}}})}
    }}
   }
  }}
 }
}
