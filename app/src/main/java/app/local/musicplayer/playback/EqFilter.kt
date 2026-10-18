package app.local.musicplayer.playback

import kotlin.math.*

/** Ten octave-spaced RBJ peaking filters; histories are independent per channel. */
class EqFilter(private val sampleRate: Int,private val channels: Int) {
    private data class Coefficients(val b0: Double,val b1: Double,val b2: Double,val a1: Double,val a2: Double)
    private var coefficients=emptyList<Coefficients>()
    private var first=emptyArray<DoubleArray>();private var second=emptyArray<DoubleArray>()
    init {require(sampleRate>0 && channels in 1..8)}
    fun configure(gains: List<Float>) {
        require(gains.size==10 && gains.all {it.isFinite() && it in -12f..12f})
        val centers=listOf(31.25,62.5,125.0,250.0,500.0,1000.0,2000.0,4000.0,8000.0,16000.0)
        coefficients=gains.mapIndexedNotNull {index,gain->
            val frequency=centers[index]
            if(gain==0f || frequency>=sampleRate*.45) null else {
                val a=10.0.pow(gain/40.0);val omega=2*PI*frequency/sampleRate;val alpha=sin(omega)/(2*1.4)
                val a0=1+alpha/a
                Coefficients((1+alpha*a)/a0,-2*cos(omega)/a0,(1-alpha*a)/a0,-2*cos(omega)/a0,(1-alpha/a)/a0)
            }
        }
        first=Array(coefficients.size) {DoubleArray(channels)};second=Array(coefficients.size) {DoubleArray(channels)}
    }
    fun process(samples: ShortArray) {
        if(coefficients.isEmpty()) return
        require(samples.size%channels==0)
        for(index in samples.indices) {
            val channel=index%channels;var sample=samples[index].toDouble()
            coefficients.forEachIndexed {band,c->
                val out=c.b0*sample+first[band][channel]
                first[band][channel]=c.b1*sample-c.a1*out+second[band][channel]
                second[band][channel]=c.b2*sample-c.a2*out
                sample=out
            }
            samples[index]=sample.roundToInt().coerceIn(-32768,32767).toShort()
        }
    }
}
