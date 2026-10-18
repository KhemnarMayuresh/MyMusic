package app.local.musicplayer

import android.app.Application
import app.local.musicplayer.collections.CollectionRepository
import app.local.musicplayer.library.MediaLibrary
import kotlinx.coroutines.*

class MusicApplication: Application() {
    val applicationScope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    val collections by lazy { CollectionRepository(this) }
    val library by lazy { MediaLibrary(this) }
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { collections.load() }
    }
}
