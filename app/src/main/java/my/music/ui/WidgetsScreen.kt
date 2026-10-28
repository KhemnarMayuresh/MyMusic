package my.music.ui
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import my.music.PlayerViewModel
import my.music.R
import my.music.widgets.*

@Composable fun WidgetsScreen(vm:PlayerViewModel,onBack:()->Unit) {
 val context=LocalContext.current
 Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF100E35),Color(0xFF191039),Color(0xFF321345)))).verticalScroll(rememberScrollState())) {
  Header("Widgets",onBack)
  Text("Your music, one tap away",Modifier.padding(start=18.dp,end=18.dp,top=8.dp,bottom=16.dp),fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.ExtraBold)
  val options=listOf(Triple("Classic • 4 × 1",R.layout.player_widget,PlayerWidgetProvider::class.java),Triple("Lite • 4 × 1",R.layout.widget_compact,CompactWidgetProvider::class.java),Triple("Vinyl • 4 × 2",R.layout.widget_artwork,ArtworkWidgetProvider::class.java))
  options.forEachIndexed {index,(title,layout,provider)->
   Column(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=4.dp).border(0.7.dp,Color(0xFF664583).copy(alpha=.65f),RoundedCornerShape(14.dp)).background(Color(0xFF1A143D).copy(alpha=.3f),RoundedCornerShape(14.dp)).padding(12.dp)) {
    Text(title,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(bottom=10.dp))
    AndroidView(factory={RemoteViews(it.packageName,layout).apply(it,null)},modifier=Modifier.fillMaxWidth().aspectRatio(when(index) {0->892f/170f;1->892f/152f;else->892f/340f}))
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(22.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFFC900),Color(0xFFFFAD0A),Color(0xFFFFC51B)))).clickable(role=Role.Button) {val manager=AppWidgetManager.getInstance(context);if(Build.VERSION.SDK_INT>=26 && manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context,provider),Bundle.EMPTY,null) else vm.message.value="Long-press your home screen and open Widgets"},contentAlignment=Alignment.Center) {Text("Add widget",fontSize=14.sp,fontWeight=FontWeight.Bold,color=Color.Black)}
   }
  }
 }
}
