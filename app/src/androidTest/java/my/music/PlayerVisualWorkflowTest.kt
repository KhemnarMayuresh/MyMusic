package my.music

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.media3.common.*
import androidx.media3.session.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import my.music.library.MediaLibrary
import my.music.model.OperationResult
import my.music.playback.PlayerAudioSignal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlayerVisualWorkflowTest {
 @Test fun coverBackgroundChangesAndPlaybackDrivesAnimationAndAudioLevels():Unit=runBlocking {
  val ins=InstrumentationRegistry.getInstrumentation();val context=ins.targetContext;val device=UiDevice.getInstance(ins)
  ins.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
  if(Build.VERSION.SDK_INT>=33) ins.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
  device.wakeUp();device.executeShellCommand("wm dismiss-keyguard")
  context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
  assertTrue(device.wait(Until.hasObject(By.desc("Open menu")),60000))
  val repository=(context.applicationContext as MusicApplication).collections
  repository.setPreference("player_theme","Vinyl");repository.setPreference("crossfade","false")
  val source=(MediaLibrary(context).rescan() as OperationResult.Success).value.maxBy {it.durationMs}
  val oldOverride=repository.state.value.overrides[source.id.uri] ?: "{}"
  val controller=MediaController.Builder(context,SessionToken(context,ComponentName(context,"my.music.playback.PlaybackService"))).buildAsync().get(20,TimeUnit.SECONDS)
  val files=mutableListOf<File>()
  fun item(color:Int,name:String):MediaItem {
   val file=File(context.cacheDir,"visual-test-$name.png");files+=file
   val bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);bitmap.eraseColor(color)
   for(y in 0..99) for(x in 0..24) bitmap.setPixel(x,y,Color.WHITE)
   file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
   return MediaItem.Builder().setMediaId("visual-$name").setUri(source.id.uri).setMediaMetadata(MediaMetadata.Builder().setTitle("Visual fixture").setArtworkUri(Uri.fromFile(file)).build()).build()
  }
  fun shot():Bitmap=ins.uiAutomation.takeScreenshot()!!
  fun backgroundHas(red:Boolean):Boolean {val bitmap=shot();val c=bitmap.getPixel(4,bitmap.height/2);bitmap.recycle();return if(red) Color.red(c)>Color.blue(c)+10 else Color.blue(c)>Color.red(c)+10}
  fun waitFor(condition:()->Boolean) {val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);while(!condition() && System.nanoTime()<deadline) Thread.sleep(100);assertTrue(condition())}
  try {
   val red=item(Color.RED,"red");val blue=item(Color.BLUE,"blue")
   ins.runOnMainSync {controller.pause();controller.setPlaybackSpeed(1f);controller.setMediaItem(red);controller.prepare()}
   assertTrue(device.wait(Until.hasObject(By.text("Visual fixture")),30000));device.findObject(By.text("Visual fixture")).click()
   assertTrue(device.wait(Until.hasObject(By.desc("Collapse player")),30000));waitFor {backgroundHas(true)}
   ins.runOnMainSync {controller.setMediaItem(blue);controller.prepare()};waitFor {backgroundHas(false)}
   val bounds=device.findObject(By.desc("Player layout: STANDARD")).visibleBounds
   fun crop():Bitmap {val original=shot();val bitmap=Bitmap.createBitmap(original,30,bounds.top+10,original.width-60,(bounds.height()/2).coerceAtLeast(1));original.recycle();return bitmap}
   Thread.sleep(300);val paused=crop();Thread.sleep(400);val pausedAgain=crop();assertTrue("Vinyl stays still while paused",paused.sameAs(pausedAgain));pausedAgain.recycle()
   ins.runOnMainSync {controller.play()}
   waitFor {PlayerAudioSignal.active?.snapshot()?.any {it>.01f}==true}
   waitFor {val moving=crop();val changed=!paused.sameAs(moving);moving.recycle();changed};paused.recycle()
   ins.runOnMainSync {controller.pause()};Thread.sleep(1000)
   waitFor {val stopped=crop();Thread.sleep(350);val stoppedAgain=crop();val same=stopped.sameAs(stoppedAgain);stopped.recycle();stoppedAgain.recycle();same}
   device.executeShellCommand("mkdir -p /sdcard/Download/MusicPlayerTestOutput");device.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/cover-vinyl-blue.png")
   val vinyl=crop()
   repository.setPreference("player_theme","Cassette")
   waitFor {val frame=crop();val changed=!vinyl.sameAs(frame);frame.recycle();changed};vinyl.recycle()
   val cassette=crop();ins.runOnMainSync {controller.play()}
   waitFor {val frame=crop();val changed=!cassette.sameAs(frame);frame.recycle();changed};cassette.recycle()
   ins.runOnMainSync {controller.pause()};Thread.sleep(1000)
   waitFor {val first=crop();Thread.sleep(350);val second=crop();val same=first.sameAs(second);first.recycle();second.recycle();same}
   device.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/cover-cassette-blue.png")
   for(theme in listOf("Neon","Film","Waveform","Minimal")) {
    repository.setPreference("player_theme",theme);Thread.sleep(700)
    device.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/revised-$theme.png")
    if(theme!="Minimal") {
     waitFor {val first=crop();Thread.sleep(350);val second=crop();val same=first.sameAs(second);first.recycle();second.recycle();same}
     val still=crop()
     ins.runOnMainSync {controller.play()}
     if(theme=="Film") {
      Thread.sleep(900);val frame=crop();assertTrue("Film stays static during playback",still.sameAs(frame));frame.recycle()
     } else waitFor {val frame=crop();val changed=!still.sameAs(frame);frame.recycle();changed}
     still.recycle()
     ins.runOnMainSync {controller.pause()};Thread.sleep(700)
    }
   }
   repository.setOverride(source.id.uri,org.json.JSONObject(oldOverride).put("cover",red.mediaMetadata.artworkUri.toString()).toString())
   waitFor {backgroundHas(true)}
   repository.setOverride(source.id.uri,oldOverride)
   waitFor {backgroundHas(false)}
  } finally {ins.runOnMainSync {controller.pause();controller.release()};files.forEach {it.delete()};repository.setOverride(source.id.uri,oldOverride)}
 }
}
