package my.music.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.media3.common.Player
import my.music.R
import my.music.playback.PlaybackService

open class PlayerWidgetProvider:AppWidgetProvider() {
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray) {update(context,manager,ids,javaClass)}
 companion object {
  val providers=listOf(PlayerWidgetProvider::class.java,CompactWidgetProvider::class.java,ArtworkWidgetProvider::class.java)
  fun refresh(context:Context,player:Player) {
   val source=player.currentMediaItem?.mediaMetadata?.extras?.getString("source_uri") ?: player.currentMediaItem?.localConfiguration?.uri?.toString()
   val favorite=(context.applicationContext as my.music.MusicApplication).collections.state.value.favorites.contains(source)
   context.getSharedPreferences("widget_state",0).edit().putBoolean("favorite",favorite).putString("title",player.currentMediaItem?.mediaMetadata?.title?.toString()).putString("artist",player.currentMediaItem?.mediaMetadata?.artist?.toString()).putString("artwork",player.currentMediaItem?.mediaMetadata?.artworkUri?.toString()).putBoolean("playing",player.isPlaying).apply()
   val manager=AppWidgetManager.getInstance(context)
   providers.forEach {provider->val ids=manager.getAppWidgetIds(ComponentName(context,provider));if(ids.isNotEmpty()) update(context,manager,ids,provider)}
  }
  private fun update(context:Context,manager:AppWidgetManager,ids:IntArray,provider:Class<*>) {
   val layout=when(provider) {CompactWidgetProvider::class.java->R.layout.widget_compact;ArtworkWidgetProvider::class.java->R.layout.widget_artwork;else->R.layout.player_widget}
   val views=RemoteViews(context.packageName,layout)
   val state=context.getSharedPreferences("widget_state",0)
   views.setTextViewText(R.id.widget_title,state.getString("title",null) ?: "Music Player")
   views.setTextViewText(R.id.widget_artist,state.getString("artist",null) ?: "Your music, your way")
   views.setInt(R.id.widget_favorite,"setColorFilter",if(state.getBoolean("favorite",false)) android.graphics.Color.rgb(255,166,0) else android.graphics.Color.WHITE)
   views.setImageViewResource(R.id.widget_play,if(state.getBoolean("playing",false)) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
   val launch=context.packageManager.getLaunchIntentForPackage(context.packageName)!!
   val activity=PendingIntent.getActivity(context,0,launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   views.setOnClickPendingIntent(R.id.widget_title,activity);views.setOnClickPendingIntent(R.id.widget_art,activity)
   val queueIntent=context.packageManager.getLaunchIntentForPackage(context.packageName)!!.putExtra("open_queue",true)
   views.setOnClickPendingIntent(R.id.widget_queue,PendingIntent.getActivity(context,10,queueIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
   listOf(R.id.widget_play to "widget.toggle",R.id.widget_previous to "widget.previous",R.id.widget_next to "widget.next",R.id.widget_favorite to "widget.favorite").forEachIndexed {index,(id,action)->views.setOnClickPendingIntent(id,PendingIntent.getService(context,index+1,Intent(context,PlaybackService::class.java).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))}
   ids.forEach {manager.updateAppWidget(it,views)}
   WidgetArtwork.request(context.applicationContext,state.getString("artwork",null)) {bitmap->if(bitmap!=null) {val artwork=RemoteViews(context.packageName,layout);artwork.setImageViewBitmap(R.id.widget_art,bitmap);ids.forEach {manager.partiallyUpdateAppWidget(it,artwork)}}}
  }
 }
}
class CompactWidgetProvider:PlayerWidgetProvider()
class ArtworkWidgetProvider:PlayerWidgetProvider()
