package my.music
import my.music.library.Recommendations
import my.music.collections.RecommendationPlay
import my.music.collections.MonthPlay
import my.music.model.Track
import my.music.model.TrackId
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class RecommendationsTest {
 private fun track(id:String,artist:String="Artist",year:Int?=null,rate:Int?=null,mime:String?=null)=Track(TrackId(id),id,artist,"Album",null,"Music",10000,mime,0,null,year,rate)
 @Test fun recommendationsUseOnlyVisibleLibraryAndKnownMetadata() {
  val tracks=listOf(track("old",year=1979),track("new",year=1999),track("unknown"),track("hq",rate=320000),track("flac",mime="audio/flac"))
  val result=Recommendations.build(tracks,listOf(RecommendationPlay("hidden",9,99),RecommendationPlay("new",8,2)),emptyList(),LocalDate.of(2026,10,28).toString())
  assertEquals(listOf("old"),result.getValue("Before 1980").map {it.title})
  assertEquals(setOf("old","new"),result.getValue("Before 2000").map {it.title}.toSet())
  assertEquals(setOf("hq","flac"),result.getValue("High Quality").map {it.title}.toSet())
  assertFalse(result.values.flatten().any {it.title=="hidden"})
  assertFalse(result.getValue("Never Played").any {it.title=="new"})
 }
 @Test fun chartUsesCurrentMonthAndDailyDiscoverIsStable() {
  val tracks=(0..80).map {track("$it")};val date=LocalDate.of(2026,10,28)
  val months=listOf(MonthPlay("2026-09","0",100),MonthPlay("2026-10","1",2),MonthPlay("2026-10","2",5))
  val result=Recommendations.build(tracks,emptyList(),months,date.toString())
  assertEquals(listOf("2","1"),result.getValue("Month’s Top Chart").map {it.title})
  assertEquals(result,Recommendations.build(tracks,emptyList(),months,date.toString()))
  assertNotEquals(result.getValue("Daily discover"),Recommendations.build(tracks,emptyList(),months,date.plusDays(1).toString()).getValue("Daily discover"))
 }
 @Test fun artistMixNeedsListeningAndLowBitrateIsNotHighQuality() {
  val tracks=listOf(track("one",artist="A",rate=256000),track("two",artist="B"),track("three",artist="B"))
  val empty=Recommendations.build(tracks,emptyList(),emptyList(),"2026-10-28")
  assertTrue(empty.getValue("Top Artist mix").isEmpty());assertTrue(empty.getValue("High Quality").isEmpty())
  val played=Recommendations.build(tracks,listOf(RecommendationPlay("one",1,4)),emptyList(),"2026-10-28")
  assertEquals(listOf("one"),played.getValue("Top Artist mix").map {it.title})
 }

 @Test fun topPickMixCardsUseTrackedPlaysAndTheWholeWinningAlbum() {
  val a=track("a").copy(album="A",albumId=1)
  val b=track("b").copy(album="B",albumId=2)
  val c=track("c").copy(album="B",albumId=2)
  val tracks=listOf(a,b,c)
  val result=Recommendations.build(tracks,listOf(RecommendationPlay("a",1,2),RecommendationPlay("b",2,3),RecommendationPlay("hidden",3,100)),emptyList(),"2026-10-28")
  assertEquals(listOf("b","a"),result.getValue("Your Hits").map {it.title})
  assertEquals(listOf("b","c"),result.getValue("Top Album").map {it.title})
  val empty=Recommendations.build(tracks,emptyList(),emptyList(),"2026-10-28")
  assertTrue(empty.getValue("Top Album").isEmpty());assertTrue(empty.getValue("Your Hits").isEmpty())
 }

}
