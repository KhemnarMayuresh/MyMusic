package my.music

import android.Manifest
import android.os.Build
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import my.music.library.MediaLibrary
import my.music.model.OperationResult
import my.music.playback.PlaybackConnection
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShuffleWorkflowTest {
 @Test fun shuffleVisitsEveryEntryAndSurvivesSongSelection()=runBlocking {
  val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
  instrumentation.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
  val source=(MediaLibrary(context).rescan() as OperationResult.Success).value.first()
  val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
  lateinit var connection:PlaybackConnection
  instrumentation.runOnMainSync {connection=PlaybackConnection(context,scope)}
  try {
   val deadline=System.nanoTime()+20_000_000_000L
   while(connection.controller==null && System.nanoTime()<deadline) delay(50)
   assertNotNull(connection.controller)
   val p=connection.controller!!
   instrumentation.runOnMainSync {p.repeatMode=Player.REPEAT_MODE_OFF;connection.play(List(20){source},shuffle=true);p.pause()}
   // Yield so the service replaces MediaController's temporary unshuffled masking timeline.
   delay(1000)
   val visited=mutableSetOf<Int>()
   repeat(20) {step->
    var index=-1;var next=false
    instrumentation.runOnMainSync {index=p.currentMediaItemIndex;next=p.hasNextMediaItem()}
    assertTrue("Repeated index $index at step $step, visited=$visited",visited.add(index))
    if(step<19) {
     assertTrue("Shuffle ended early at step $step, visited=$visited",next)
     instrumentation.runOnMainSync {p.seekToNextMediaItem()}
     delay(150)
    }
   }
   assertEquals(20,visited.size)
   instrumentation.runOnMainSync {connection.play(listOf(source,source),1);p.pause()}
   delay(500)
   instrumentation.runOnMainSync {assertTrue("Selecting a song must preserve enabled shuffle",p.shuffleModeEnabled)}

  } finally {instrumentation.runOnMainSync {connection.controller?.pause();connection.release()};scope.cancel()}
 }
}

