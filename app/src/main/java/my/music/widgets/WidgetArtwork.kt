package my.music.widgets
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/** One bounded artwork decode, reused by all widget sizes. */
internal object WidgetArtwork {
 private val worker=Executors.newSingleThreadExecutor()
 private val main=Handler(Looper.getMainLooper())
 private var key:String?=null
 private var loading:String?=null
 private var bitmap:Bitmap?=null
 private val requests=ArtworkRequests()
 private var latestTicket=0L
 private val waiting=mutableListOf<(Bitmap?)->Unit>()
 fun request(context:Context,uri:String?,ready:(Bitmap?)->Unit) {
  val ticket=requests.select(uri)
  if(ticket!=latestTicket) {latestTicket=ticket;loading=null;waiting.clear()}
  if(uri==null) {ready(null);return}
  if(key==uri) {ready(bitmap);return}
  if(loading==uri) {waiting.add(ready);return}
  waiting.clear();waiting.add(ready)
  loading=uri
  worker.execute {
   val decoded=runCatching {
    val source=Uri.parse(uri);val options=BitmapFactory.Options().apply {inJustDecodeBounds=true}
    context.contentResolver.openInputStream(source)?.use {BitmapFactory.decodeStream(it,null,options)}
    if(options.outWidth<=0 || options.outHeight<=0) null else {
     var sample=1;while(options.outWidth/sample>256 || options.outHeight/sample>256) sample*=2
     options.inJustDecodeBounds=false;options.inSampleSize=sample
     context.contentResolver.openInputStream(source)?.use {BitmapFactory.decodeStream(it,null,options)}
    }
   }.getOrNull()
   main.post {if(requests.owns(ticket,uri) && loading==uri) {loading=null;key=uri;bitmap=decoded;val callbacks=waiting.toList();waiting.clear();callbacks.forEach {it(decoded)}}}
  }
 }
}
