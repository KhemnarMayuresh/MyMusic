package app.local.musicplayer.ui
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.local.musicplayer.PlayerViewModel
import app.local.musicplayer.R
import app.local.musicplayer.widgets.*

@Composable fun WidgetsScreen(vm:PlayerViewModel,onBack:()->Unit) {
 val context=LocalContext.current
 Column(Modifier.verticalScroll(rememberScrollState())) {
  Header("Widgets",onBack)
  Text("Your music, one tap away",Modifier.padding(20.dp),style=MaterialTheme.typography.titleLarge)
  val options=listOf(Triple("Classic • 4 × 1",R.layout.player_widget,PlayerWidgetProvider::class.java),Triple("Lite • 4 × 1",R.layout.widget_compact,CompactWidgetProvider::class.java),Triple("Vinyl • 4 × 2",R.layout.widget_artwork,ArtworkWidgetProvider::class.java))
  options.forEachIndexed {index,(title,layout,provider)->
   Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp).border(1.dp,Muted.copy(alpha=.25f),RoundedCornerShape(20.dp)).padding(16.dp)) {
    Text(title,style=MaterialTheme.typography.titleMedium)
    AndroidView(factory={RemoteViews(it.packageName,layout).apply(it,null)},modifier=Modifier.fillMaxWidth().height(when(index) {0->86.dp;1->86.dp;else->152.dp}).padding(vertical=8.dp))
    Button(onClick={val manager=AppWidgetManager.getInstance(context);if(Build.VERSION.SDK_INT>=26 && manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context,provider),Bundle.EMPTY,null) else vm.message.value="Long-press your home screen and open Widgets"},modifier=Modifier.fillMaxWidth()) {Text("Add widget")}
   }
  }
 }
}
