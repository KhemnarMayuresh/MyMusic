package app.local.musicplayer.ui

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

private val artCache=object: LruCache<String,Bitmap>(8*1024*1024) { override fun sizeOf(key: String,value: Bitmap)=value.byteCount }
@Composable fun Artwork(uri: String?,modifier: Modifier=Modifier) {
    val context=LocalContext.current
    val bitmap by produceState<Bitmap?>(null,uri) {
        value=uri?.let { key -> artCache.get(key) ?: withContext(Dispatchers.IO) {
            runCatching {
                val source=Uri.parse(key)
                val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it,null,options) }
                if(options.outWidth<=0 || options.outHeight<=0) null else {
                    var sample=1; while(options.outWidth/sample>600 || options.outHeight/sample>600) sample*=2
                    options.inJustDecodeBounds=false; options.inSampleSize=sample
                    context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it,null,options) }?.also { artCache.put(key,it) }
                }
            }.getOrNull()
        } }
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFFEE628A),Color(0xFF8E6EB8),Color(0xFF376BBB)))),contentAlignment=Alignment.Center) {
        if(bitmap!=null) Image(bitmap!!.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        else Icon(Icons.Rounded.MusicNote,null,Modifier.fillMaxSize(.48f),tint=Color.White.copy(alpha=.85f))
    }
}
