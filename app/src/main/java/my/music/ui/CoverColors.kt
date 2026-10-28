package my.music.ui

/** Alpha-weighted average; dark background and a light accent retain the cover hue. */
internal fun coverColors(pixels:IntArray):LongArray {
    var r=0L;var g=0L;var b=0L;var weight=0L
    for(c in pixels) {val a=c ushr 24;weight+=a;r+=(c ushr 16 and 255)*a.toLong();g+=(c ushr 8 and 255)*a.toLong();b+=(c and 255)*a.toLong()}
    if(weight==0L) return longArrayOf(0xFF090B22,0xFF35134D,0xFFFFBF00)
    fun color(scale:Double,white:Int=0):Long = 0xFF000000L or (((r/weight*scale).toInt()+white).coerceIn(0,255).toLong() shl 16) or (((g/weight*scale).toInt()+white).coerceIn(0,255).toLong() shl 8) or ((b/weight*scale).toInt()+white).coerceIn(0,255).toLong()
    return longArrayOf(color(.2),color(.45),color(.4,153))
}
