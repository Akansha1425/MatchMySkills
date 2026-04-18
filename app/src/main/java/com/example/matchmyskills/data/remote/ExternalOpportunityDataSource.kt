package com.example.matchmyskills.data.remote

import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.model.Job
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.Locale
import java.util.Calendar

object ExternalOpportunityDataSource {

    private val greenhouseBoards = listOf("airbnb", "shopify", "stripe")

    private interface ArbeitnowApi {
        @GET("api/job-board-api")
        suspend fun getJobs(): JsonObject
    }

    private interface GreenhouseApi {
        @GET("v1/boards/{board}/jobs")
        suspend fun getJobs(
            @retrofit2.http.Path("board") board: String
        ): JsonObject
    }

    private val arbeitnowApi: ArbeitnowApi by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.arbeitnow.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ArbeitnowApi::class.java)
    }

    private val greenhouseApi: GreenhouseApi by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()

        Retrofit.Builder()
            .baseUrl("https://boards-api.greenhouse.io/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GreenhouseApi::class.java)
    }

    suspend fun fetchJobs(keyword: String, type: String): List<Job> = withContext(Dispatchers.IO) {
        val arbeitnow = runCatching {
            parseArbeitnowJobs(arbeitnowApi.getJobs(), keyword, type)
        }.getOrDefault(emptyList())

        val greenhouse = runCatching {
            greenhouseBoards.flatMap { board ->
                parseGreenhouseJobs(greenhouseApi.getJobs(board), keyword, type, board)
            }
        }.getOrDefault(emptyList())

        (arbeitnow + greenhouse).distinctBy { it.id }
    }

    suspend fun fetchHackathons(limit: Int = 20): List<Hackathon> = withContext(Dispatchers.IO) {
        val devpost = runCatching { scrapeDevpostHackathons() }.getOrDefault(emptyList())
        val mlh = runCatching { scrapeMlhHackathons() }.getOrDefault(emptyList())
        val devfolio = runCatching { scrapeDevfolioHackathons() }.getOrDefault(emptyList())

        (devpost + mlh + devfolio).distinctBy { it.id }.take(limit)
    }

    private fun parseGreenhouseJobs(root: JsonObject, keyword: String, type: String, board: String): List<Job> {
        val jobsArray = root.getAsJsonArray("jobs") ?: return emptyList()

        return jobsArray.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val item = element.asJsonObject

            val title = getString(item, "title") ?: return@mapNotNull null
            val content = getString(item, "content").orEmpty()
            val location = getString(item, "location") ?: "Remote / Flexible"
            val url = getString(item, "absolute_url")
            val company = board.replaceFirstChar { it.titlecase(Locale.ROOT) }

            val haystack = "$title $content $location".lowercase(Locale.ROOT)
            if (keyword.isNotBlank() && !haystack.contains(keyword.lowercase(Locale.ROOT))) {
                return@mapNotNull null
            }

            Job(
                id = "ext_${type.lowercase(Locale.ROOT)}_greenhouse_${board}_${slugify(title)}",
                recruiterId = "external_greenhouse_$board",
                title = title,
                companyName = company,
                description = content.ifBlank { "Open the apply link for full details." },
                coreSkills = guessSkills(title, content),
                location = location,
                stipend = "Not disclosed",
                status = "Active",
                opportunityType = type,
                source = "EXTERNAL",
                applyUrl = url
            )
        }
    }

    private fun scrapeDevpostHackathons(): List<Hackathon> {
        val doc = Jsoup.connect("https://devpost.com/hackathons")
            .userAgent("Mozilla/5.0 (Android) MatchMySkills")
            .timeout(15000)
            .get()

        val cards = doc.select("a.challenge-listing, .challenge-listing, .hackathon-tile, .challenge-listing-item")
        val parsedFromCards = cards.mapNotNull { card ->
            val title = textBySelectors(card, listOf(".title", "h3", "h2", ".challenge-listing-title")).ifBlank {
                card.attr("title")
            }

            if (title.isBlank()) {
                return@mapNotNull null
            }

            val organizer = textBySelectors(
                card,
                listOf(".challenge-company", ".hosted-by", ".caption", ".subtitle")
            ).ifBlank { "Devpost" }

            val bodyText = card.text()
            val description = bodyText.takeIf { it.isNotBlank() }
                ?: "Open link to view complete hackathon details."

            val href = when {
                card.tagName().equals("a", ignoreCase = true) -> card.absUrl("href")
                else -> card.selectFirst("a[href]")?.absUrl("href").orEmpty()
            }

            val mode = when {
                bodyText.contains("online", ignoreCase = true) -> "Online"
                bodyText.contains("offline", ignoreCase = true) -> "Offline"
                else -> "Online"
            }

            val prize = Regex("\\$[\\d,]+(?:\\s?[kKmM])?")
                .find(bodyText)
                ?.value
                ?: "Not specified"

            Hackathon(
                id = "ext_hackathon_devpost_${slugify("$title|$organizer|$href")}",
                recruiterId = "external_devpost",
                title = title,
                organizer = organizer,
                description = description,
                themes = emptyList(),
                eligibility = "Open to all",
                mode = mode,
                platformOrLocation = if (mode == "Online") "Online" else "Venue TBA",
                prizePool = prize,
                teamSize = "As per rules",
                status = "Active",
                opportunityType = "HACKATHON",
                source = "EXTERNAL",
                applyUrl = href.ifBlank { null }
            )
        }

        val parsedFromJsonLd = parseHackathonsFromJsonLd(
            doc = doc,
            idPrefix = "ext_hackathon_devpost_ld",
            recruiterId = "external_devpost",
            organizer = "Devpost",
            platformOrLocation = "Devpost"
        )

        return (parsedFromCards + parsedFromJsonLd).distinctBy { it.id }
    }

    private fun scrapeMlhHackathons(): List<Hackathon> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val doc = fetchFirstAvailableDocument(
            listOf(
                "https://mlh.io/seasons/$currentYear/events",
                "https://mlh.io/seasons/${currentYear - 1}/events",
                "https://mlh.io/seasons"
            )
        ) ?: return emptyList()

        val cards = doc.select(".event-wrapper, .event-card, a.event-link")
        val parsedFromCards = cards.mapNotNull { card ->
            val title = textBySelectors(card, listOf("h3", ".event-name", ".title")).ifBlank { card.attr("title") }
            if (title.isBlank()) return@mapNotNull null

            val organizer = "Major League Hacking"
            val description = textBySelectors(card, listOf("p", ".event-date", ".info")).ifBlank {
                "Open link to view complete hackathon details."
            }
            val href = card.selectFirst("a[href]")?.absUrl("href").orEmpty().ifBlank { card.absUrl("href") }

            Hackathon(
                id = "ext_hackathon_mlh_${slugify("$title|$href")}",
                recruiterId = "external_mlh",
                title = title,
                organizer = organizer,
                description = description,
                themes = emptyList(),
                eligibility = "Student focused",
                mode = if (description.contains("online", ignoreCase = true)) "Online" else "Offline",
                platformOrLocation = "MLH",
                prizePool = "Not specified",
                teamSize = "As per rules",
                status = "Active",
                opportunityType = "HACKATHON",
                source = "EXTERNAL",
                applyUrl = href.ifBlank { null }
            )
        }

        val parsedFromJsonLd = parseHackathonsFromJsonLd(
            doc = doc,
            idPrefix = "ext_hackathon_mlh_ld",
            recruiterId = "external_mlh",
            organizer = "Major League Hacking",
            platformOrLocation = "MLH"
        )

        return (parsedFromCards + parsedFromJsonLd).distinctBy { it.id }
    }

    private fun scrapeDevfolioHackathons(): List<Hackathon> {
        val doc = fetchFirstAvailableDocument(
            listOf(
                "https://devfolio.co/hackathons",
                "https://devfolio.co/"
            )
        ) ?: return emptyList()

        val cards = doc.select("a[href*='/hackathons/'], .hackathon-card, .event-card")
        val parsedFromCards = cards.mapNotNull { card ->
            val title = textBySelectors(card, listOf("h3", "h2", ".title")).ifBlank { card.attr("title") }
            if (title.isBlank()) return@mapNotNull null

            val description = textBySelectors(card, listOf("p", ".description", ".summary")).ifBlank {
                "Open link to view complete hackathon details."
            }
            val href = card.selectFirst("a[href]")?.absUrl("href").orEmpty().ifBlank { card.absUrl("href") }

            Hackathon(
                id = "ext_hackathon_devfolio_${slugify("$title|$href")}",
                recruiterId = "external_devfolio",
                title = title,
                organizer = "Devfolio",
                description = description,
                themes = emptyList(),
                eligibility = "Open to all",
                mode = if (description.contains("online", ignoreCase = true)) "Online" else "Offline",
                platformOrLocation = "Devfolio",
                prizePool = "Not specified",
                teamSize = "As per rules",
                status = "Active",
                opportunityType = "HACKATHON",
                source = "EXTERNAL",
                applyUrl = href.ifBlank { null }
            )
        }

        val parsedFromJsonLd = parseHackathonsFromJsonLd(
            doc = doc,
            idPrefix = "ext_hackathon_devfolio_ld",
            recruiterId = "external_devfolio",
            organizer = "Devfolio",
            platformOrLocation = "Devfolio"
        )

        return (parsedFromCards + parsedFromJsonLd).distinctBy { it.id }
    }

    private fun fetchFirstAvailableDocument(urls: List<String>): org.jsoup.nodes.Document? {
        urls.forEach { url ->
            try {
                return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Android) MatchMySkills")
                    .timeout(15000)
                    .get()
            } catch (_: Exception) {
                // Try next URL.
            }
        }
        return null
    }

    private fun parseHackathonsFromJsonLd(
        doc: org.jsoup.nodes.Document,
        idPrefix: String,
        recruiterId: String,
        organizer: String,
        platformOrLocation: String
    ): List<Hackathon> {
        val scripts = doc.select("script[type=application/ld+json]")
        if (scripts.isEmpty()) return emptyList()

        val items = mutableListOf<Hackathon>()

        scripts.forEach { script ->
            val raw = script.data().trim()
            if (raw.isBlank()) return@forEach

            val root = runCatching { com.google.gson.JsonParser.parseString(raw) }.getOrNull() ?: return@forEach
            val objects = extractJsonObjects(root)

            objects.forEach { obj ->
                val type = getJsonString(obj, "@type").orEmpty().lowercase(Locale.ROOT)
                val title = getJsonString(obj, "name").orEmpty()
                if (title.isBlank()) return@forEach

                if (!(type.contains("event") || type.contains("hackathon") || title.lowercase(Locale.ROOT).contains("hack"))) {
                    return@forEach
                }

                val description = getJsonString(obj, "description")
                    .orEmpty()
                    .ifBlank { "Open link to view complete hackathon details." }

                val url = getJsonString(obj, "url")
                val locationBlob = obj.get("location")
                val modeOrLocation = locationBlob?.toString().orEmpty().lowercase(Locale.ROOT)
                val mode = when {
                    modeOrLocation.contains("online") -> "Online"
                    modeOrLocation.contains("offline") -> "Offline"
                    else -> "Online"
                }

                val item = Hackathon(
                    id = "${idPrefix}_${slugify("$title|$url")}",
                    recruiterId = recruiterId,
                    title = title,
                    organizer = organizer,
                    description = description,
                    themes = emptyList(),
                    eligibility = "Open to all",
                    mode = mode,
                    platformOrLocation = platformOrLocation,
                    prizePool = "Not specified",
                    teamSize = "As per rules",
                    status = "Active",
                    opportunityType = "HACKATHON",
                    source = "EXTERNAL",
                    applyUrl = url
                )

                items.add(item)
            }
        }

        return items.distinctBy { it.id }
    }

    private fun extractJsonObjects(element: JsonElement): List<JsonObject> {
        return when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val graph = obj.get("@graph")
                if (graph != null && graph.isJsonArray) {
                    graph.asJsonArray.mapNotNull { if (it.isJsonObject) it.asJsonObject else null }
                } else {
                    listOf(obj)
                }
            }

            element.isJsonArray -> {
                element.asJsonArray.mapNotNull { if (it.isJsonObject) it.asJsonObject else null }
            }

            else -> emptyList()
        }
    }

    private fun getJsonString(obj: JsonObject, key: String): String? {
        if (!obj.has(key)) return null
        val value = obj.get(key)
        if (value == null || !value.isJsonPrimitive) return null
        val text = value.asString
        return text.takeIf { it.isNotBlank() }
    }

    private fun parseArbeitnowJobs(root: JsonObject, keyword: String, type: String): List<Job> {
        val jobsArray = root.getAsJsonArray("data") ?: return emptyList()

        return jobsArray.mapIndexedNotNull { index, element ->
            if (!element.isJsonObject) return@mapIndexedNotNull null
            val item = element.asJsonObject

            val title = getString(item, "title", "job_title", "name")
            if (title.isNullOrBlank()) return@mapIndexedNotNull null

            val company = when {
                item.has("company") && item.get("company").isJsonObject -> {
                    val companyObj = item.getAsJsonObject("company")
                    getString(companyObj, "name", "company_name", "title")
                }
                else -> getString(item, "company_name", "company")
            }.orEmpty().ifBlank { "Unknown Company" }

            val description = getString(item, "description", "summary", "content")
                .orEmpty()
                .ifBlank { "Open the apply link for full details." }

            val tags = getString(item, "tags")
                .orEmpty()

            val haystack = "$title $description $tags".lowercase(Locale.ROOT)
            if (keyword.isNotBlank() && !haystack.contains(keyword.lowercase(Locale.ROOT))) {
                return@mapIndexedNotNull null
            }

            val location = getString(item, "location", "workplace", "region")
                .orEmpty()
                .ifBlank { "Remote / Flexible" }

            val stipend = getString(item, "salary", "salary_range", "compensation")
                .orEmpty()
                .ifBlank { "Not disclosed" }

            val applyUrl = getString(item, "url", "apply_url", "job_url", "external_url")
            val rawId = getString(item, "id", "_id", "job_id", "slug")
                ?: "${title}_${company}_$index"

            Job(
                id = "ext_${type.lowercase(Locale.ROOT)}_arbeitnow_${slugify(rawId)}",
                recruiterId = "external_arbeitnow",
                title = title,
                companyName = company,
                description = description,
                coreSkills = guessSkills(title, description),
                location = location,
                stipend = stipend,
                status = "Active",
                opportunityType = type,
                source = "EXTERNAL",
                applyUrl = applyUrl
            )
        }
    }

    private fun guessSkills(title: String, description: String): List<String> {
        val text = "$title $description".lowercase(Locale.ROOT)
        val knownSkills = listOf(
            "android", "kotlin", "java", "python", "react", "node", "sql", "firebase", "machine learning"
        )
        return knownSkills.filter { text.contains(it) }.map { skill ->
            if (skill == "machine learning") "Machine Learning" else skill.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }

    private fun getString(obj: JsonObject, vararg keys: String): String? {
        keys.forEach { key ->
            if (obj.has(key) && obj.get(key).isJsonPrimitive) {
                val value = obj.get(key).asString
                if (value.isNotBlank()) {
                    return value
                }
            }
        }
        return null
    }

    private fun textBySelectors(element: org.jsoup.nodes.Element, selectors: List<String>): String {
        selectors.forEach { selector ->
            val value = element.selectFirst(selector)?.text().orEmpty().trim()
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun slugify(raw: String): String {
        return raw.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
            .ifBlank { raw.hashCode().toString() }
    }
}