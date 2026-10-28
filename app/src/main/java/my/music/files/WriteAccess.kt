package my.music.files

object WriteAccess {
    fun needsPermission(sdk: Int,granted: Boolean)=sdk<=28 && !granted
}
