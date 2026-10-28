package my.music
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.media3.common.*
import androidx.media3.session.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import my.music.library.MediaLibrary
import my.music.model.OperationResult
import my.music.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ThemeProgressWorkflowTest {
 private val instrumentation=InstrumentationRegistry.getInstrumentation()
 private val context get()=instrumentation.targetContext
 private val device get()=UiDevice.getInstance(instrumentation)
 private fun launch() {
  instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
  device.wakeUp();device.executeShellCommand("wm dismiss-keyguard")
  context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
  assertTrue(device.wait(Until.hasObject(By.desc("Open menu")),60000))
 }
 private fun progress():android.graphics.Rect {assertTrue(device.wait(Until.hasObject(By.desc("Song progress")),30000));return device.findObject(By.desc("Song progress")).visibleBounds}
 private fun screenshot(name:String) {device.executeShellCommand("mkdir -p /sdcard/Download/MusicPlayerTestOutput");device.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/$name.png")}
 @Test fun previewAndAppliedLayoutsSeekTheSameMediaSession():Unit=runBlocking {
  launch()
  val repository=(context.applicationContext as MusicApplication).collections
  val source=(MediaLibrary(context).rescan() as OperationResult.Success).value.maxBy {it.durationMs}
  val controller=MediaController.Builder(context,SessionToken(context,ComponentName(context,"my.music.playback.PlaybackService"))).buildAsync().get(20,TimeUnit.SECONDS)
  try {
   instrumentation.runOnMainSync {controller.pause();controller.setPlaybackSpeed(1f);controller.repeatMode=Player.REPEAT_MODE_OFF;controller.setMediaItem(MediaItem.Builder().setMediaId("seek-fixture").setUri(source.id.uri).setMediaMetadata(MediaMetadata.Builder().setTitle("Seek fixture").build()).build());controller.prepare()}
   var ready=false;val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20)
   while(!ready && System.nanoTime()<deadline) {Thread.sleep(100);instrumentation.runOnMainSync {ready=controller.duration>0}}
   assertTrue(ready)
   assertTrue(device.wait(Until.hasObject(By.text("Seek fixture")),30000));device.findObject(By.text("Seek fixture")).click()
   fun verifySeek(fraction:Float) {
    val expected=(source.durationMs*fraction).toLong();var position=0L
    val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8)
    do {Thread.sleep(100);instrumentation.runOnMainSync {position=controller.currentPosition}} while(abs(position-expected)>2000 && System.nanoTime()<until)
    assertTrue("Seek fraction=$fraction expected=$expected actual=$position",abs(position-expected)<=2000)
   }
   var standardY=0
   for(name in listOf("Artwork","Minimal","Sunset","Polaroid","Vinyl","Orbit")) {
    repository.setPreference("player_theme",name)
    val layout=PlayerThemes.find(name).layout
    assertTrue(device.wait(Until.hasObject(By.desc("Player layout: ${layout.name}")),30000));device.waitForIdle()
    val rect=progress()
    if(name=="Artwork") standardY=rect.centerY()
    if(name=="Minimal") assertTrue("Minimal moves progress above artwork",rect.centerY()<standardY-100)
    if(layout==PlayerLayout.RING) {
     assertTrue(rect.height()>rect.width()*.8)
     device.click(rect.right-12,rect.centerY());verifySeek(.25f)
     device.swipe(rect.right-12,rect.centerY(),rect.centerX(),rect.bottom-12,100);verifySeek(.5f)
    } else {
     assertTrue(rect.width()>rect.height()*2)
     device.click(rect.left+rect.width()*3/4,rect.centerY());verifySeek(.75f)
     device.swipe(rect.left+rect.width()*3/4,rect.centerY(),rect.left+rect.width()/4,rect.centerY(),100);verifySeek(.25f)
    }
    screenshot("applied-$name")
   }
   instrumentation.runOnMainSync {controller.sendCustomCommand(SessionCommand("timer.start",android.os.Bundle.EMPTY),android.os.Bundle().apply {putLong("duration",60000);putBoolean("finish",false)})}
   device.findObject(By.text("Lyrics")).click();assertTrue(device.wait(Until.hasObject(By.textStartsWith("Sleep in")),10000));device.findObject(By.text("Song")).click()
   instrumentation.runOnMainSync {controller.sendCustomCommand(SessionCommand("timer.cancel",android.os.Bundle.EMPTY),android.os.Bundle.EMPTY)}
   assertTrue(device.wait(Until.hasObject(By.desc("Player themes")),30000));device.findObject(By.desc("Player themes")).click()
   assertTrue(device.wait(Until.hasObject(By.text("Themes")),30000));assertTrue(device.wait(Until.hasObject(By.desc("Player layout: RING")),30000));device.waitForIdle()
   val preview=progress();device.click(preview.right-10,preview.centerY());verifySeek(.25f)
   screenshot("preview-ring")
  } finally {instrumentation.runOnMainSync {controller.pause();controller.release()}}
 }
 @Test fun emptyLibraryThemePreviewProgressCanBeScrubbed():Unit=runBlocking {
  val controller=MediaController.Builder(context,SessionToken(context,ComponentName(context,"my.music.playback.PlaybackService"))).buildAsync().get(20,TimeUnit.SECONDS)
  try {
   instrumentation.runOnMainSync {controller.pause();controller.clearMediaItems()}
   (context.applicationContext as MusicApplication).collections.setPreference("player_theme","Artwork")
   launch();device.findObject(By.desc("Themes")).click()
   assertTrue(device.wait(Until.hasObject(By.text("Player")),30000));device.findObject(By.text("Player")).click()
   val rect=progress();device.click(rect.left+rect.width()*3/4,rect.centerY())
   fun fraction(node:android.view.accessibility.AccessibilityNodeInfo?):Float? {
    if(node==null) return null
    if(node.contentDescription?.toString()=="Song progress") node.rangeInfo?.let {return (it.current-it.min)/(it.max-it.min)}
    for(i in 0 until node.childCount) fraction(node.getChild(i))?.let {return it}
    return null
   }
   val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);var value=0f
   while(value !in .74f.. .76f && System.nanoTime()<deadline) {Thread.sleep(100);value=fraction(instrumentation.uiAutomation.rootInActiveWindow) ?: 0f}
   assertTrue("The demo preview must expose its scrubbed position, actual=$value",value in .74f.. .76f)
   screenshot("preview-demo-seek")
  } finally {instrumentation.runOnMainSync {controller.release()}}
 }
}
