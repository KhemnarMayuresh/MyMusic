package my.music
import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.media3.common.*
import androidx.media3.session.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import my.music.library.MediaLibrary
import my.music.model.OperationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
@RunWith(AndroidJUnit4::class)
class QueueReshuffleWorkflowTest {
 @Test fun repeatedReshufflePreservesCurrentOccurrenceAndPosition():Unit=runBlocking {
  val ins=InstrumentationRegistry.getInstrumentation();val context=ins.targetContext
  ins.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
  if(Build.VERSION.SDK_INT>=33) ins.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
  val device=androidx.test.uiautomator.UiDevice.getInstance(ins)
  device.wakeUp();device.executeShellCommand("wm dismiss-keyguard")
  context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
  assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.desc("Open menu")),60000))
  val source=(MediaLibrary(context).rescan() as OperationResult.Success).value.first()
  val p=MediaController.Builder(context,SessionToken(context,ComponentName(context,"my.music.playback.PlaybackService"))).buildAsync().get(20,TimeUnit.SECONDS)
  try {
   ins.runOnMainSync {p.pause();p.setMediaItems(List(12){MediaItem.Builder().setMediaId("reshuffle-$it").setUri(source.id.uri).build()},4,1000);p.prepare()}
   Thread.sleep(500)
   fun ids():List<String> {var result=listOf<String>();ins.runOnMainSync {result=(0 until p.mediaItemCount).map {p.getMediaItemAt(it).mediaId}};return result}
   var previous=ids()
   repeat(3) {
    if(it==0) {
     lateinit var future:com.google.common.util.concurrent.ListenableFuture<SessionResult>
     ins.runOnMainSync {future=p.sendCustomCommand(SessionCommand("queue.reshuffle",Bundle.EMPTY),Bundle.EMPTY)}
     assertEquals(SessionResult.RESULT_SUCCESS,future.get(10,TimeUnit.SECONDS).resultCode)
    } else {
     if(it==1) {
      context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.putExtra("open_queue",true).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP))
      assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.text("Playing queue")),30000))
     } else {device.pressBack();assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.desc("Collapse player")),30000))}
     assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.desc("Shuffle")),30000))
     device.findObject(androidx.test.uiautomator.By.desc("Shuffle")).click()
    }
    val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10)
    while(ids()==previous && System.nanoTime()<deadline) Thread.sleep(50)
    val next=ids();assertNotEquals(previous,next);assertEquals(previous.toSet(),next.toSet());assertEquals(12,next.size)
    ins.runOnMainSync {assertEquals("reshuffle-4",p.currentMediaItem!!.mediaId);assertEquals(1000L,p.currentPosition);assertFalse(p.isPlaying)}
    previous=next
   }
   ins.runOnMainSync {p.play()}
   var playing=false;val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(15)
   while(!playing && System.nanoTime()<until) {Thread.sleep(50);ins.runOnMainSync {playing=p.isPlaying && p.currentPosition>1100}}
   assertTrue(playing)
   var before=0L;lateinit var future:com.google.common.util.concurrent.ListenableFuture<SessionResult>
   ins.runOnMainSync {before=p.currentPosition;future=p.sendCustomCommand(SessionCommand("queue.reshuffle",Bundle.EMPTY),Bundle.EMPTY)}
   assertEquals(SessionResult.RESULT_SUCCESS,future.get(10,TimeUnit.SECONDS).resultCode)
   ins.runOnMainSync {assertTrue(p.isPlaying);assertEquals("reshuffle-4",p.currentMediaItem!!.mediaId);assertTrue(p.currentPosition>=before-100)}

  } finally {ins.runOnMainSync {p.pause();p.release()}}
 }
}
