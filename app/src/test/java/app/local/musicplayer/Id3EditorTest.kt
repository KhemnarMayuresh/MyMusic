package app.local.musicplayer

import app.local.musicplayer.files.Id3Editor
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class Id3EditorTest {
    @Test fun writingTagsPreservesAudioAndUnknownFramesAndOriginalFile() {
        val directory=Files.createTempDirectory("id3-test").toFile()
        try {
            val unknown=byteArrayOf(84,88,88,88,0,0,0,4,0,0,0,65,66,67)
            val audio=byteArrayOf(-1,-5,-112,100,1,2,3,4,5)
            val header=byteArrayOf(73,68,51,3,0,0,0,0,0,14)
            val bytes=header+unknown+audio
            val source=File(directory,"source.mp3").apply {writeBytes(bytes)};val output=File(directory,"output.mp3")
            Id3Editor.rewrite(source,output,"New title","New artist","New album")
            assertArrayEquals(bytes,source.readBytes())
            val result=output.readBytes()
            assertArrayEquals(audio,result.takeLast(audio.size).toByteArray())
            assertTrue(result.toList().windowed(unknown.size).any {it==unknown.toList()})
            val frameSize=((result[14].toInt() and 255) shl 24) or ((result[15].toInt() and 255) shl 16) or ((result[16].toInt() and 255) shl 8) or (result[17].toInt() and 255)
            assertEquals("TIT2",String(result,10,4,Charsets.ISO_8859_1))
            assertEquals("New title",String(result,21,frameSize-1,Charsets.UTF_16))
        } finally {directory.deleteRecursively()}
    }
    @Test fun malformedTagLengthFailsBeforeDestinationIsWritten() {
        val directory=Files.createTempDirectory("id3-bad").toFile()
        try {
            val source=File(directory,"bad.mp3").apply {writeBytes(byteArrayOf(73,68,51,3,0,0,127,127,127,127))}
            val destination=File(directory,"output.mp3")
            assertThrows(IllegalArgumentException::class.java) {Id3Editor.rewrite(source,destination,"Title","Artist","Album")}
            assertFalse(destination.exists())
        } finally {directory.deleteRecursively()}
    }
}
