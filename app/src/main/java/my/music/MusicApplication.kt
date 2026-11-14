package my.music

import android.app.Activity
import android.app.Application
import android.os.Bundle
import my.music.collections.CollectionRepository
import my.music.island.DynamicIslandBus
import my.music.library.MediaLibrary
import kotlinx.coroutines.*

class MusicApplication: Application() {
    val applicationScope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    val collections by lazy { CollectionRepository(this) }
    val library by lazy { MediaLibrary(this) }
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object: ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                if (activity is MainActivity) DynamicIslandBus.onActivityStarted(System.identityHashCode(activity))
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                if (activity is MainActivity) {
                    DynamicIslandBus.onActivityStopped(System.identityHashCode(activity), activity.isChangingConfigurations)
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (activity is MainActivity) {
                    DynamicIslandBus.onActivityDestroyed(System.identityHashCode(activity), activity.isChangingConfigurations)
                }
            }
        })
        applicationScope.launch { collections.load() }
    }
}
