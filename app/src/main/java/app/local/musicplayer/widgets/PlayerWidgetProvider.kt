package app.local.musicplayer.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.media3.common.Player
import app.local.musicplayer.R
import app.local.musicplayer.playback.PlaybackService

class PlayerWidgetProvider: AppWidgetProvider() {
    override fun onUpdate(context: Context,manager: AppWidgetManager,ids: IntArray) { update(context,manager,ids,null) }
    companion object {
        fun refresh(context: Context,player: Player) {
            val manager=AppWidgetManager.getInstance(context)
            val ids=manager.getAppWidgetIds(ComponentName(context,PlayerWidgetProvider::class.java))
            if(ids.isNotEmpty()) update(context,manager,ids,player)
        }
        private fun update(context: Context,manager: AppWidgetManager,ids: IntArray,player: Player?) {
            val views=RemoteViews(context.packageName,R.layout.player_widget)
            views.setTextViewText(R.id.widget_title,player?.currentMediaItem?.mediaMetadata?.title ?: "Music Player")
            views.setTextViewText(R.id.widget_play,if(player?.isPlaying==true) "Ⅱ" else "▶")
            val launch=context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            views.setOnClickPendingIntent(R.id.widget_title,PendingIntent.getActivity(context,0,launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            listOf(R.id.widget_play to "widget.toggle",R.id.widget_previous to "widget.previous",R.id.widget_next to "widget.next").forEachIndexed { index,(id,action) ->
                val intent=Intent(context,PlaybackService::class.java).setAction(action)
                views.setOnClickPendingIntent(id,PendingIntent.getService(context,index+1,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            ids.forEach { manager.updateAppWidget(it,views) }
        }
    }
}
