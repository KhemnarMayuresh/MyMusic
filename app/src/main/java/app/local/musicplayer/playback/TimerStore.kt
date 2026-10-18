package app.local.musicplayer.playback

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/** Elapsed-time deadlines are applicable only to the same authenticated boot count. */
class TimerStore(private val context: Context,name: String="sleep-timer") {
    private val prefs=context.getSharedPreferences(name,Context.MODE_PRIVATE)
    data class Restored(val deadline: SleepDeadline,val entryId: String?)
    private fun boot()=Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,-1)
    fun save(deadline: SleepDeadline,entryId: String?) {
        prefs.edit().putLong("end",deadline.startedAtMs+deadline.durationMs).putInt("boot",boot()).putBoolean("finish",deadline.finishCurrentTrack).putString("entry",entryId).apply()
    }
    fun restore(): Restored? {
        val currentBoot=boot()
        if(!prefs.contains("end") || currentBoot<0 || prefs.getInt("boot",-1)!=currentBoot) return null
        val end=prefs.getLong("end",-1);val now=SystemClock.elapsedRealtime();val remaining=end-now
        val finish=prefs.getBoolean("finish",false)
        if(remaining>86_400_000 || (!finish && remaining<=0)) {clear();return null}
        return Restored(if(remaining>0) SleepDeadline(now,remaining,finish) else SleepDeadline(now-1,1,finish),prefs.getString("entry",null))
    }
    fun clear() {prefs.edit().clear().apply()}
}
