package eu.kanade.tachiyomi.extension.en.tenearthshatteringblows

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
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale

class TenEarthShatteringBlows : HttpSource() {
    override val name = "Ten Earth Shattering Blows"
    override val baseUrl = "https://tenearthshatteringblows.com/"
    override val lang = "en"
    override val supportsLatest = false

    private fun initTheManga(manga: SManga): SManga = manga.apply {
        url = "/archives/comic"
        thumbnail_url = "https://store.tenearthshatteringblows.com/wp-content/uploads/2023/01/00_cover_ISBN-300x433.jpg"
        author = "Tommaso Devitofrancesco, aka. Nuclearpasta"
        artist = "Tommaso Devitofrancesco, aka. Nuclearpasta"
        // Quoted from TV Tropes, combined with the author’s description.
        // While the author’s description is funnier, this one gives
        // new readers a better idea of what to expect.
        description = """Lady Landabella Trastan, a noblewoman from the rich city of Vezenia,
            | travels across the desert with a skimpy escort. Her quest: to kill the "evil witch"
            | that resides in the fortress of Saltfall. Her motives: unknown. What is certain is
            | that she hugely underestimates the task. Caught between ruthless slavers and
            | lizard-riding marauders, she soon discovers that the authority of her noble
            | status is worth nothing in a lawless, unforgiving land.\n\n
            | Ten Earth Shattering Blows is a fantasy graphic novel you can read online.
            | It contains violence and, occasionally, tits.
            | This makes you a bad person if you enjoy it.\n\n
            | Support the creator on Patreon: https://www.patreon.com/nuclearpasta
            | \nOr on Ko-fi: https://ko-fi.com/G2G68CW3
        """.trimMargin()
        genre = "Fantasy, Sword and Sorcery"
        status = SManga.ONGOING
        initialized = true
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> =
        Observable.just(
            MangasPage(
                listOf(initTheManga(SManga.create())),
                false,
            ),
        )

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = Observable.just(initTheManga(manga))

    override fun chapterListParse(response: Response): List<SChapter> {
        val rootArchive = response.asJsoup()
        var chNum = 0

        return (
            sequenceOf(rootArchive) + rootArchive
                .select("""#paginav a[title]""")
                .map { client.newCall(GET(it.attr("href")!!, headers)).execute().asJsoup() }
            )
            .flatMap { archivePage ->
                archivePage.select(""".archivecomicthumbwrap""").map { pgEnt ->
                    val chDate = DATE_FMT.parse(
                        pgEnt
                            .select(""".archivecomicthumbdate""")
                            .first()!!
                            .text()!!
                            .replace(ORD_RGX, ""),
                    )
                    val chAnchor = pgEnt.select(""".archivecomicframe a""").first()!!
                    val chName = if (chAnchor.hasAttr("title")) {
                        chAnchor.attr("title")
                    } else {
                        chNum.toString()
                    }

                    SChapter.create().apply {
                        name = chName
                        date_upload = chDate?.time ?: 0L
                        chapter_number = (chNum++).toFloat()
                        setUrlWithoutDomain(chAnchor.attr("href"))
                    }
                }
            }
            .asIterable()
            .reversed()
    }

    override fun pageListParse(response: Response): List<Page> =
        response
            .asJsoup()
            .select("""#comic img""")
            .map { Page(index = 0, imageUrl = it.attr("src")) }

    override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()

    override fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException()

    override fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException()

    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()

    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    companion object {
        private val ORD_RGX = """st|nd|rd|th""".toRegex()
        private val DATE_FMT = SimpleDateFormat("MMM d, yyyy", Locale.US)
    }
}
