package my.music
import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import my.music.ui.CustomThemes
import my.music.ui.PlayerThemes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
@RunWith(AndroidJUnit4::class)
class IntroThemesWorkflowTest {
 @Test fun brandedIntroAndCustomThemeCreateEditReloadDelete():Unit=runBlocking {
  val ins=InstrumentationRegistry.getInstrumentation();val context=ins.targetContext;val device=UiDevice.getInstance(ins)
  val repository=(context.applicationContext as MusicApplication).collections
  val oldCustom=repository.state.value.pref(CustomThemes.KEY);val oldSelected=repository.state.value.pref("player_theme","Artwork")
  ins.uiAutomation.grantRuntimePermission(context.packageName,if(Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
  if(Build.VERSION.SDK_INT>=33) ins.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
  device.wakeUp();device.executeShellCommand("wm dismiss-keyguard")
  fun launch() {context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))}
  fun text(value:String) {assertTrue("Visible: $value",device.wait(Until.hasObject(By.text(value)),30000));device.findObject(By.text(value)).click()}
  fun waitFor(condition:()->Boolean) {val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(!condition() && System.nanoTime()<until) Thread.sleep(50);assertTrue(condition())}
  try {
   val config=Configurator.getInstance();val idle=config.waitForIdleTimeout;config.waitForIdleTimeout=0
   try {
    launch();assertTrue("Branded intro is shown",device.wait(Until.hasObject(By.descContains("Music Player intro")),20000))
    device.executeShellCommand("mkdir -p /sdcard/Download/MusicPlayerTestOutput");device.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/intro-20261021.png")
   } finally {config.waitForIdleTimeout=idle}
   assertTrue(device.wait(Until.hasObject(By.desc("Open menu")),60000))
   assertEquals(15,PlayerThemes.all.size)
   device.findObject(By.desc("Themes")).click();text("Player");text("Create theme")
   assertTrue(device.wait(Until.hasObject(By.clazz("android.widget.EditText")),10000))
   device.findObject(By.clazz("android.widget.EditText")).setText("My test violet")
   device.findObject(By.desc("Choose top color")).click()
   assertTrue(device.wait(Until.hasObject(By.desc("Blue")),10000));device.findObject(By.desc("Blue")).click();text("Use color")
   text("Save and apply")
   waitFor {CustomThemes.decode(repository.state.value.pref(CustomThemes.KEY)).any {it.name=="My test violet"}}
   val value=CustomThemes.decode(repository.state.value.pref(CustomThemes.KEY)).first {it.name=="My test violet"}
   assertEquals(value.id,repository.state.value.pref("player_theme"));assertFalse(value.coverColors);assertEquals(0xFF102B52L,value.top)
   text("Edit");assertTrue(device.wait(Until.hasObject(By.clazz("android.widget.EditText")),10000));device.findObject(By.clazz("android.widget.EditText")).setText("Edited violet");text("Save and apply")
   waitFor {CustomThemes.decode(repository.state.value.pref(CustomThemes.KEY)).any {it.id==value.id && it.name=="Edited violet"}}
   launch();assertTrue(device.wait(Until.hasObject(By.desc("Open menu")),60000));device.findObject(By.desc("Themes")).click();text("Player")
   assertTrue(device.wait(Until.hasObject(By.text("Edited violet")),30000));assertEquals(value.id,repository.state.value.pref("player_theme"))
   device.executeShellCommand("screencap -p /sdcard/Download/MusicPlayerTestOutput/custom-theme-20261021.png")
   text("Delete")
   assertTrue(device.wait(Until.hasObject(By.text("Delete theme?")),10000))
   val deleteButtons=device.findObjects(By.text("Delete"));deleteButtons.last().click()
   waitFor {CustomThemes.decode(repository.state.value.pref(CustomThemes.KEY)).none {it.id==value.id}}
   assertEquals("Artwork",repository.state.value.pref("player_theme"))
  } finally {repository.setPreferences(mapOf(CustomThemes.KEY to oldCustom,"player_theme" to oldSelected))}
 }
}
