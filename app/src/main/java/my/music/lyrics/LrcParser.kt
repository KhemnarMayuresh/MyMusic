package my.music.lyrics

data class LyricLine(val timeMs: Long?, val text: String)
data class LyricsDocument(val lines: List<LyricLine>) {
    val timed get() = lines.any { it.timeMs != null }
    fun activeLine(positionMs: Long) = lines.lastOrNull { it.timeMs != null && it.timeMs <= positionMs }
}
object LrcParser {
    private val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val metadata = Regex("^\\[(?:ar|al|ti|by|length|re|ve|offset):.*]$",RegexOption.IGNORE_CASE)
    fun parse(text: String): LyricsDocument {
        require(text.length <= 1_000_000) { "Lyrics file is too large" }
        val offset = Regex("\\[offset:([+-]?\\d+)]",RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val lines = text.removePrefix("\uFEFF").lineSequence().flatMap { line ->
            val stamps = timestamp.findAll(line).filter { it.groupValues[2].toInt() < 60 }.toList()
            if(stamps.isEmpty()) {
                if(line.isBlank() || metadata.matches(line)) emptySequence() else sequenceOf(LyricLine(null,line))
            } else {
                val words = timestamp.replace(line,"").trim()
                stamps.asSequence().map { stamp ->
                    val fractional = stamp.groupValues[3].padEnd(3,'0').take(3).toLongOrNull() ?: 0
                    LyricLine((stamp.groupValues[1].toLong()*60_000+stamp.groupValues[2].toLong()*1000+fractional+offset).coerceAtLeast(0),words)
                }
            }
        }.toList()
        return LyricsDocument(if(lines.any { it.timeMs != null }) lines.sortedBy { it.timeMs ?: Long.MAX_VALUE } else lines)
    }
}
