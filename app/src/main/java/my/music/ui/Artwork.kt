package my.music.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class CoverArt(val bitmap:Bitmap,val blur:Bitmap,val colors:LongArray)
private val artCache=object:LruCache<String,CoverArt>(8*1024*1024) {override fun sizeOf(key:String,value:CoverArt)=value.bitmap.byteCount+value.blur.byteCount}
private val artLoad=kotlinx.coroutines.sync.Mutex()
@Composable internal fun rememberCover(uri:String?):CoverArt? = key(uri) {
    val context=LocalContext.current
    val art by produceState<CoverArt?>(null,uri) {
        if(uri!=null) value=withContext(Dispatchers.IO) {
            artLoad.lock()
            try {artCache.get(uri) ?: decodeCover(context,uri)?.also {artCache.put(uri,it)}} finally {artLoad.unlock()}
        }
    }
    art
}
private fun decodeCover(context:android.content.Context,key:String):CoverArt? {
    try {
        val source=Uri.parse(key)
        val options=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        context.contentResolver.openInputStream(source)?.use {BitmapFactory.decodeStream(it,null,options)}
        if(options.outWidth<=0 || options.outHeight<=0) return null
        var sample=1;while(options.outWidth/sample>600 || options.outHeight/sample>600) sample*=2
        options.inJustDecodeBounds=false;options.inSampleSize=sample
        val bitmap=context.contentResolver.openInputStream(source)?.use {BitmapFactory.decodeStream(it,null,options)} ?: return null
        val small=Bitmap.createScaledBitmap(bitmap,40,40,true)
        var pixels=IntArray(1600);small.getPixels(pixels,0,40,0,0,40,40)
        val colors=coverColors(pixels)
        repeat(3) {
            val next=IntArray(1600)
            for(y in 0..39) for(x in 0..39) {
                var red=0;var green=0;var blue=0;var count=0
                for(dy in -3..3) for(dx in -3..3) {val c=pixels[(y+dy).coerceIn(0,39)*40+(x+dx).coerceIn(0,39)];red+=c ushr 16 and 255;green+=c ushr 8 and 255;blue+=c and 255;count++}
                next[y*40+x]=(255 shl 24) or ((red/count) shl 16) or ((green/count) shl 8) or (blue/count)
            }
            pixels=next
        }
        val blur=Bitmap.createBitmap(pixels,40,40,Bitmap.Config.ARGB_8888)
        if(small!==bitmap) small.recycle()
        return CoverArt(bitmap,blur,colors)
    } catch(e:kotlinx.coroutines.CancellationException) {throw e} catch(_:Exception) {return null}
}
@Composable fun Artwork(uri:String?,modifier:Modifier=Modifier) {
    val bitmap=rememberCover(uri)?.bitmap
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFFEE628A),Color(0xFF8E6EB8),Color(0xFF376BBB)))),contentAlignment=Alignment.Center) {
        if(bitmap!=null) Image(bitmap!!.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        else Icon(Icons.Rounded.MusicNote,null,Modifier.fillMaxSize(.48f),tint=Color.White.copy(alpha=.85f))
    }
}
