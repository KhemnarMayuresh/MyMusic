package my.music.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.MediaItem
import my.music.PlayerViewModel
import my.music.playback.PlaybackSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

@Composable private fun QueueRow(item:MediaItem,active:Boolean,lifted:Boolean=false,onClick:()->Unit={},onNext:()->Unit={},onRemove:()->Unit={}) {
 var menu by remember {mutableStateOf(false)}
 Row(Modifier.fillMaxWidth().background(if(lifted) Color(0xFF34224F) else Color.Transparent).clickable(onClick=onClick).padding(start=16.dp,top=10.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically) {
  Box(Modifier.size(48.dp),contentAlignment=Alignment.Center) {Icon(Icons.Rounded.DragHandle,"Drag to reorder ${item.mediaMetadata.title}",tint=Muted)}
  Artwork(item.mediaMetadata.artworkUri?.toString(),Modifier.size(48.dp))
  Column(Modifier.weight(1f).padding(horizontal=12.dp)) {Text(item.mediaMetadata.title?.toString().orEmpty(),maxLines=1,overflow=TextOverflow.Ellipsis,color=if(active) MaterialTheme.colorScheme.primary else Color.White);Text(item.mediaMetadata.artist?.toString().orEmpty(),color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)}
  Box {IconButton(onClick={menu=true}) {Icon(Icons.Rounded.MoreVert,"Queue song actions")};DropdownMenu(menu,{menu=false}) {DropdownMenuItem(text={Text("Play next")},onClick={menu=false;onNext()});DropdownMenuItem(text={Text("Remove from queue")},onClick={menu=false;onRemove()})}}
 }
}
@Composable fun QueueScreen(vm:PlayerViewModel,state:PlaybackSnapshot,onBack:()->Unit) {
 val list=rememberLazyListState()
 var items by remember {mutableStateOf(state.items)}
 var dragging by remember {mutableStateOf<String?>(null)}
 var dragTop by remember {mutableFloatStateOf(0f)}
 var dragHeight by remember {mutableIntStateOf(0)}
 var pointerY by remember {mutableFloatStateOf(0f)}
 LaunchedEffect(state.items,dragging) {if(dragging==null) items=state.items}
 fun reorder() {
  val id=dragging ?: return
  val source=items.indexOfFirst {it.mediaId==id}
  val info=list.layoutInfo
  val rows=info.visibleItemsInfo
  if(source<0 || rows.isEmpty()) return
  val center=(dragTop+dragHeight/2).coerceIn(info.viewportStartOffset+1f,info.viewportEndOffset-1f)
  val target=rows.firstOrNull {center>=it.offset && center<=it.offset+it.size} ?: rows.minByOrNull {abs(center-(it.offset+it.size/2))} ?: return
  if(source!=target.index && target.index in items.indices) items=items.toMutableList().apply {add(target.index,removeAt(source))}
 }
 fun finish(commit:Boolean) {
  val id=dragging
  if(commit && id!=null) vm.connection.controller?.let {p->
   val from=(0 until p.mediaItemCount).firstOrNull {p.getMediaItemAt(it).mediaId==id}
   val nextId=items.getOrNull(items.indexOfFirst {it.mediaId==id}+1)?.mediaId
   val before=nextId?.let {key->(0 until p.mediaItemCount).firstOrNull {p.getMediaItemAt(it).mediaId==key}}
   if(from!=null) {val destination=if(before==null) p.mediaItemCount-1 else if(from<before) before-1 else before;if(from!=destination) p.moveMediaItem(from,destination)}
  }
  dragging=null
 }
 LaunchedEffect(dragging) {while(dragging!=null && isActive) {
  val info=list.layoutInfo
  val amount=when {pointerY<info.viewportStartOffset+90f->-18f;pointerY>info.viewportEndOffset-90f->18f;else->0f}
  if(amount!=0f) {list.scrollBy(amount);reorder()}
  delay(16)
 }}
 Column {
  Header("Playing queue",onBack) {
   IconButton(onClick={vm.reshuffle()}) {Icon(Icons.Rounded.Shuffle,"Shuffle",tint=if(state.shuffle) MaterialTheme.colorScheme.primary else Muted)}
   IconButton(onClick={vm.connection.controller?.repeatMode=when(state.repeat) {0->2;2->1;else->0}}) {Icon(if(state.repeat==1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,"Repeat",tint=if(state.repeat!=0) MaterialTheme.colorScheme.primary else Muted)}
   IconButton(onClick={vm.connection.controller?.clearMediaItems()}) {Icon(Icons.Rounded.DeleteSweep,"Clear queue")}
  }
  Text(if(items.isEmpty()) "Queue is empty" else "${state.index+1} / ${items.size}",Modifier.padding(20.dp),color=Muted)
  Box(Modifier.weight(1f)) {
   // The list owns the gesture: recycling a row must never cancel an in-progress drag.
   LazyColumn(state=list,modifier=Modifier.fillMaxSize().pointerInput(list) {
    awaitEachGesture {
     val down=awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial)
     val start=list.layoutInfo.visibleItemsInfo.firstOrNull {down.position.y>=it.offset && down.position.y<it.offset+it.size}
     if(down.position.x in 16.dp.toPx()..64.dp.toPx() && start!=null) {
      down.consume();dragging=start.key as? String;dragTop=start.offset.toFloat();dragHeight=start.size;pointerY=down.position.y
      try {
       while(true) {
        val event=awaitPointerEvent(PointerEventPass.Initial)
        val change=event.changes.firstOrNull {it.id==down.id} ?: break
        if(!change.pressed) {change.consume();finish(true);break}
        dragTop+=change.positionChange().y;pointerY=change.position.y;change.consume();reorder()
       }
      } finally {if(dragging!=null) finish(false)}
     }
    }
   }) {items(items,key={it.mediaId}) {item->
    Box(Modifier.alpha(if(dragging==item.mediaId) .15f else 1f)) {
     QueueRow(item,state.current?.mediaId==item.mediaId,onClick={vm.connection.controller?.let {p->val index=(0 until p.mediaItemCount).firstOrNull {p.getMediaItemAt(it).mediaId==item.mediaId};if(index!=null) p.seekToDefaultPosition(index)}},onNext={vm.connection.controller?.let {p->val from=(0 until p.mediaItemCount).firstOrNull {p.getMediaItemAt(it).mediaId==item.mediaId};if(from!=null && from!=p.currentMediaItemIndex) p.moveMediaItem(from,(p.currentMediaItemIndex+if(from<p.currentMediaItemIndex) 0 else 1).coerceAtMost(p.mediaItemCount-1))}},onRemove={vm.connection.controller?.let {p->val index=(0 until p.mediaItemCount).firstOrNull {p.getMediaItemAt(it).mediaId==item.mediaId};if(index!=null) p.removeMediaItem(index)}})
    }
   }}
   items.find {it.mediaId==dragging}?.let {item->Box(Modifier.offset {IntOffset(0,dragTop.toInt().coerceIn(0,(list.layoutInfo.viewportEndOffset-dragHeight).coerceAtLeast(0)))}) {QueueRow(item,state.current?.mediaId==item.mediaId,true)}}
  }
 }
}
