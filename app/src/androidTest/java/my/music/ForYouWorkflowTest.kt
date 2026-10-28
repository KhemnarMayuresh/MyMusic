package my.music
import android.Manifest
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import my.music.library.MediaLibrary
import my.music.model.OperationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class ForYouWorkflowTest {
 @Test fun songsDefaultMixPlaybackDisableRetainReenableAndConfirmedCleanup():Unit=runBlocking {
  val ins=InstrumentationRegistry.getInstrumentation();val c=ins.targetContext;val d=UiDevice.getInstance(ins)
  val repo=(c.applicationContext as MusicApplication).collections
  val oldEnabled=repo.state.value.pref("for_you_enabled","true")
  ins.uiAutomation.grantRuntimePermission(c.packageName,Manifest.permission.READ_MEDIA_AUDIO)
  ins.uiAutomation.grantRuntimePermission(c.packageName,Manifest.permission.POST_NOTIFICATIONS)
  fun launch() {d.pressHome();ins.startActivitySync(c.packageManager.getLaunchIntentForPackage(c.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));assertTrue(d.wait(Until.hasObject(By.desc("Open menu")),60000))}
  fun click(s:String) {assertTrue("Visible $s",d.wait(Until.hasObject(By.text(s)),20000));d.findObject(By.text(s)).click()}
  fun scroll(s:String) {
   repeat(24) {
    val target=runCatching {d.findObject(By.text(s))?.visibleBounds}.getOrNull()
    if(target!=null && target.height()>8 && target.centerY()<d.displayHeight-150) return
    val bounds=d.findObjects(By.scrollable(true)).mapNotNull {runCatching {it.visibleBounds}.getOrNull()}.maxByOrNull {it.height()}
    if(bounds!=null && bounds.height()>100) d.swipe(bounds.centerX(),bounds.bottom-50,bounds.centerX(),bounds.bottom-bounds.height()/4,100)
    d.waitForIdle()
   }
   d.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/for-you-scroll-failure.png")
   d.dumpWindowHierarchy(java.io.File(c.getExternalFilesDir(null),"for-you-scroll-failure.xml"))
   fail("Scroll to $s")
  }
  fun await(check:()->Boolean) {val end=System.currentTimeMillis()+10000;while(!check() && System.currentTimeMillis()<end) Thread.sleep(100);assertTrue(check())}
  fun settings() {assertTrue(d.wait(Until.hasObject(By.desc("Settings")),10000));d.findObject(By.desc("Settings")).click();assertTrue(d.wait(Until.hasObject(By.text("General")),10000))}
  try {
   d.wakeUp();d.executeShellCommand("wm dismiss-keyguard");repo.setPreference("for_you_enabled","true");launch()
   assertTrue("Songs is default",d.wait(Until.hasObject(By.desc("Sort songs")),20000));assertTrue(d.hasObject(By.text("For you")))
   val tracks=(MediaLibrary(c).rescan() as OperationResult.Success).value;assertTrue(tracks.isNotEmpty())
   repo.recordPlay(tracks.first().id.uri);repo.recordPlay(tracks.first().id.uri)
   click("For you");assertTrue("Top picks shown",d.wait(Until.hasObject(By.text("Top picks")),20000));assertTrue("Recently played shown",d.wait(Until.hasObject(By.text("Recently Played")),20000));d.executeShellCommand("mkdir -p /sdcard/Download/MusicPlayerTestOutput");d.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/for-you-dashboard.png")
   scroll("Daily discover");click("Daily discover");assertTrue(d.wait(Until.hasObject(By.text("Play")),10000));d.findObjects(By.text("Play")).first().click()
   await {repo.state.value.recommendationPlays.sumOf {it.count}>=3}
   assertTrue(d.wait(Until.hasObject(By.desc("Open player")),10000));d.findObject(By.desc("Open player")).click();assertTrue(d.wait(Until.hasObject(By.desc("Pause")),20000));d.findObject(By.desc("Pause")).click();d.findObject(By.desc("Collapse player")).click();d.pressBack()
   assertTrue(d.wait(Until.hasObject(By.text("Top picks")),10000));settings();scroll("For you");click("For you");await {!repo.state.value.flag("for_you_enabled",true)}
   val saved=repo.state.value.recommendationPlays;val months=repo.state.value.monthPlays
   repo.recordPlay(tracks.last().id.uri);assertEquals(saved,repo.state.value.recommendationPlays);assertEquals(months,repo.state.value.monthPlays)
   d.pressBack();assertTrue(d.wait(Until.hasObject(By.desc("Sort songs")),10000));assertFalse(d.hasObject(By.text("For you")))
   settings();scroll("For you");click("For you");await {repo.state.value.flag("for_you_enabled",true)};d.pressBack();click("For you");assertEquals(saved,repo.state.value.recommendationPlays)
   settings();scroll("Clear recommendation data");click("Clear recommendation data");assertTrue(d.wait(Until.hasObject(By.text("Clear recommendation data?")),10000));click("Cancel");assertEquals(saved,repo.state.value.recommendationPlays)
   click("Clear recommendation data");assertTrue(d.wait(Until.hasObject(By.text("Clear data")),10000));click("Clear data");await {repo.state.value.recommendationPlays.isEmpty() && repo.state.value.monthPlays.isEmpty()}
  } finally {repo.setPreference("for_you_enabled",oldEnabled)}
 }
}
