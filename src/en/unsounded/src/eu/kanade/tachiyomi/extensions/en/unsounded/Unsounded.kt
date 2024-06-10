package eu.kanade.tachiyomi.extension.en.unsounded

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale

class Unsounded : HttpSource() {
    private val chapterNumberRegex = """Chapter (\d+):""".toRegex()
    private val chapterBoxSelector = """div#main_content > div#chapter_box"""

    override val name = "Dark Science"
    override val baseUrl = "https://www.casualvillain.com/Unsounded"
    override val lang = "en"
    override val supportsLatest = false

    private val archiveUrl = "$baseUrl/comic+index/"
    private val authorName = "Ashley Cope"
    private val seriesDescription = "" +
        "Some dead men tell tales, and some little girls have tails…\n" +
        "Daughter of the Lord of Thieves, Sette Frummagem is on a mission, and she'll lie, " +
        "cheat, and steal to make sure it's a success (she'll lie, cheat, and steal anyway). " +
        "Condemned to aid her in her rotten endeavours is a rotten corpse who seems oddly " +
        "talented with the supernatural, and oddly not laying motionless in the dirt.\n" +
        "The road is long and no one is what they seem. Never trust a thief, and never trust " +
        "anyone who won't let you look into their eyes.\n" +
        "\nSupport this masterpiece on Patreon: https://www.patreon.com/unsounded"

    private fun initTheManga(manga: SManga): SManga = manga.apply {
        url = archiveUrl
        thumbnail_url = "$baseUrl/comic/ch01/pageart/ch01_01.jpg"
        title = name
        author = authorName
        artist = authorName
        description = seriesDescription
        genre = "Epic Fantasy, Adventure, Horror"
        status = SManga.ONGOING
        initialized = true
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> = Observable.just(
        MangasPage(
            listOf(initTheManga(SManga.create())),
            false,
        ),
    )

    // We still (re-)initialise all properties here, for this method also gets called on a
    // backup restore. And in a backup, only `url` and `title` are preserved.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = Observable.just(initTheManga(manga))

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val chapters = mutableListOf<SChapter>()
        val archivePage = client.newCall(GET(archiveUrl, headers)).execute().asJsoup()

        archivePage.select(chapterBoxSelector).forEach { ch ->
            val chTitle = ch.selectFirst("h2")!!.text()
            val chNumber = chapterNumberRegex.find(chTitle)!!.groupValues!!.getOrNull(1)!!.toFloat()

            ch.select("a").forEach { pg ->
                val pgNumber = pg.text()!!

                chapters.add(SChapter.create().apply {
                    name = "$chTitle :: $pgNumber"
                    chapter_number = chNumber + (pgNumber.toFloat() / 1000.0F)
                    url = pg.attr("href")!!.replaceFirst("..", baseUrl)
                    date_upload = 0L
                })
            }
        }

        return Observable.just(chapters)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val pages = mutableListOf<Page>()
        val chapterDoc = client.newCall(GET(chapter.url, headers)).execute().asJsoup()

        return Observable.just(pages)
    }

    override fun imageUrlParse(response: Response): String =
    response.asJsoup().selectFirst("article.post img.aligncenter")!!.attr("src")

    override fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException()

    override fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> = throw UnsupportedOperationException()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException()

    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()

    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun chapterListRequest(manga: SManga): Request = throw UnsupportedOperationException()

    override fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()

    override fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()

    companion object {
        private val DATE_FMT = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    }
}
