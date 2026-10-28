package my.music

import my.music.playback.EqFilter
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class EqualizerTest {
    @Test fun flatEqualizerPreservesSamplesExactly() {
        val samples=shortArrayOf(0,1000,-2000,32767,-32768,245)
        val actual=samples.copyOf();EqFilter(44100,2).process(actual)
        assertArrayEquals(samples,actual)
    }
    @Test fun gainAtOneKilohertzIncreasesActualSignalEnergy() {
        val samples=ShortArray(44100) {i->(1500*sin(2*PI*1000*i/44100)).toInt().toShort()}
        val eq=EqFilter(44100,1);eq.configure(listOf(0f,0f,0f,0f,0f,6f,0f,0f,0f,0f));eq.process(samples)
        val rms=sqrt(samples.drop(4000).sumOf {it.toDouble()*it}/(samples.size-4000))
        assertTrue("A +6dB 1kHz band should boost a 1kHz tone",rms>1600)
        assertTrue("Signal should remain finite and unclipped",rms<3500)
    }
    @Test fun stereoFiltersDoNotLeakLeftAudioIntoSilentRightChannel() {
        val samples=ShortArray(2000) {if(it%2==0) 2000 else 0}
        val eq=EqFilter(48000,2);eq.configure(List(10) {6f});eq.process(samples)
        assertTrue(samples.indices.filter {it%2==1}.all {samples[it]==0.toShort()})
    }
}
