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
    override val name = "Unsounded"
    override val baseUrl = "https://www.casualvillain.com/Unsounded/"
    override val lang = "en"
    override val supportsLatest = false

    private fun initTheManga(manga: SManga): SManga = manga.apply {
        url = "/comic+index/"
        thumbnail_url = "$baseUrl/comic/ch01/pageart/ch01_01.jpg"
        title = name
        author = "Ashley Cope"
        artist = "Ashley Cope"
        description = """Some dead men tell tales, and some little girls have tails…\n
            | Daughter of the Lord of Thieves, Sette Frummagem is on a mission, and she'll lie,
            | cheat, and steal to make sure it's a success (she'll lie, cheat, and steal anyway).
            | Condemned to aid her in her rotten endeavours is a rotten corpse who seems oddly
            | talented with the supernatural, and oddly not laying motionless in the dirt.\n
            | The road is long and no one is what they seem. Never trust a thief, and never trust
            | anyone who won't let you look into their eyes.\n
            | \nI highly recommend reading this comic on its original website. Ashley does
            | phenomenal and often subtle things with the website background surrounding the
            | actual comic pages.\n
            | \nSupport this masterpiece on Patreon: https://www.patreon.com/unsounded
            """.trimMargin()
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

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = Observable.just(initTheManga(manga))

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = mutableListOf<SChapter>()
        val archivePage = response.asJsoup()

        archivePage.select("""div#main_content > div#chapter_box""").forEach { ch ->
            val chTitle = ch.selectFirst("h2")!!.text()
            val chNumber = CH_NUM.find(chTitle)!!.groupValues!!.getOrNull(1)!!.toFloat()

            ch.select("a").forEach { pg ->
                val pgNumber = pg.text()!!

                chapters.add(SChapter.create().apply {
                    name = "$chTitle :: $pgNumber"
                    chapter_number = chNumber + (pgNumber.toFloat() / 1000.0F)
                    url = pg.attr("href")!!.replaceFirst("..", "")
                    date_upload = 0L
                })
            }
        }

        return chapters
    }

    override fun pageListParse(response: Response): List<Page> {
        val chDoc = response.asJsoup()
        val chUrl = response.request.url
        var pages = mutableListOf<Page>()
        var pgIx = 0

        chDoc.select("#comic>img").forEach { pgImg ->
            val pgUrl = chUrl.resolve(pgImg.attr("src")!!)!!.encodedPath

            pages.add(Page(index = pgIx++, imageUrl = pgUrl))
        }
        chDoc.select("a>img").forEach { pgImg ->
            val pgUrl = chUrl.resolve(pgImg.attr("src")!!)!!.encodedPath

            pages.add(Page(index = pgIx++, imageUrl = pgUrl))
        }
        chDoc.select("""a>div[style*="background-image:"]""").forEach { pgImg ->
            val pgUrl = PG_URL.find(pgImg.attr("style")!!)?.groupValues?.getOrNull(1)
            if (pgUrl != null) {
                pages.add(Page(index = pgIx++, imageUrl = pgUrl))
            }
        }
        chDoc.select("""div[position][style*="background-image:"]""").forEach { pgImg ->
            val pgUrl = PG_URL.find(pgImg.attr("style")!!)?.groupValues?.getOrNull(1)
            if (pgUrl != null) {
                pages.add(Page(index = pgIx++, imageUrl = pgUrl))
            }
        }

        throw UnsupportedOperationException()

        return pages
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

    companion object {
        private val CH_NUM = """Chapter (\d+):""".toRegex()
        private val PG_URL = """background\-image\:\s*url\((.+)\)""".toRegex()
    }
}
