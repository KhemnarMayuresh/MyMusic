package app.local.musicplayer.model

data class TrackId(val uri: String)
data class Track(
    val id: TrackId, val title: String, val artist: String, val album: String,
    val albumId: Long?, val folder: String, val durationMs: Long,
    val mimeType: String?, val dateAddedSeconds: Long, val artworkUri: String?,
)
data class QueueEntry(val entryId: String, val trackId: TrackId)
sealed interface OperationResult<out T> {
    data class Success<T>(val value: T): OperationResult<T>
    data class Failure(val reason: FailureReason, val message: String): OperationResult<Nothing>
}
enum class FailureReason { PERMISSION_REQUIRED, MEDIA_UNAVAILABLE, UNSUPPORTED, MALFORMED_INPUT, CANCELLED, IO }
