package my.music.files

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

/** Format-aware ID3v2.3/v2.4 writer. Untouched frames and audio payload are preserved. */
object Id3Editor {
    private const val MAX_TAG=8*1024*1024
    private fun sync(bytes: ByteArray,start: Int): Int {
        var value=0;for(i in start until start+4) {val n=bytes[i].toInt() and 255;require(n<128) {"Invalid ID3 size"};value=(value shl 7) or n};return value
    }
    private fun size(value: Int,sync: Boolean)=ByteArray(4) {i->((value ushr ((3-i)*(if(sync) 7 else 8))) and (if(sync) 127 else 255)).toByte()}
    fun rewrite(source: File,destination: File,title: String,artist: String,album: String) {
        require(source.canonicalPath!=destination.canonicalPath)
        require(listOf(title,artist,album).all {it.length<=10_000}) {"Tag text is too long"}
        RandomAccessFile(source,"r").use {input->
            require(input.length()>=10) {"Audio file is too short"}
            val header=ByteArray(10);input.readFully(header)
            val tagged=String(header,0,3,Charsets.ISO_8859_1)=="ID3"
            val version=if(tagged) header[3].toInt() else 3
            require(version==3 || version==4) {"Only ID3v2.3 and ID3v2.4 MP3 tags can be edited"}
            require(!tagged || header[5]==0.toByte()) {"Extended or unsynchronised ID3 tags are preserved; editing is unsupported"}
            require(tagged || ((header[0].toInt() and 255)==255 && (header[1].toInt() and 224)==224)) {"File tag editing supports MP3 audio"}
            val tagLength=if(tagged) sync(header,6) else 0
            require(tagLength<=MAX_TAG && (!tagged || tagLength.toLong()+10<=input.length())) {"Invalid or oversized ID3 tag"}
            val tag=ByteArray(tagLength);if(tagged) input.readFully(tag)
            val retained=ByteArrayOutputStream();var offset=0
            while(offset<tag.size) {
                if(tag[offset]==0.toByte()) {require(tag.copyOfRange(offset,tag.size).all {it==0.toByte()}) {"Invalid ID3 padding"};break}
                require(tag.size-offset>=10) {"Truncated ID3 frame"}
                val name=String(tag,offset,4,Charsets.ISO_8859_1);require(name.matches(Regex("[A-Z0-9]{4}"))) {"Invalid ID3 frame identifier"}
                val length=if(version==4) sync(tag,offset+4) else (0..3).fold(0L) {n,i->(n shl 8) or (tag[offset+4+i].toLong() and 255)}.also {require(it<=MAX_TAG)}.toInt()
                require(length<=tag.size-offset-10) {"Truncated ID3 frame payload"}
                if(name !in setOf("TIT2","TPE1","TALB")) retained.write(tag,offset,length+10)
                offset+=length+10
            }
            val result=ByteArrayOutputStream()
            listOf("TIT2" to title,"TPE1" to artist,"TALB" to album).forEach { (id,text)->
                val payload=byteArrayOf(1)+text.toByteArray(Charsets.UTF_16)
                result.write(id.toByteArray(Charsets.ISO_8859_1));result.write(size(payload.size,version==4));result.write(byteArrayOf(0,0));result.write(payload)
            }
            retained.writeTo(result);result.write(ByteArray(256));require(result.size()<=MAX_TAG) {"Updated ID3 tag is too large"}
            input.seek(if(tagged) tagLength+10L else 0L)
            destination.outputStream().use {output->
                output.write(byteArrayOf(73,68,51,version.toByte(),0,0));output.write(size(result.size(),true));result.writeTo(output)
                val buffer=ByteArray(65536);while(true) {val count=input.read(buffer);if(count<0) break;output.write(buffer,0,count)}
            }
        }
    }
}
