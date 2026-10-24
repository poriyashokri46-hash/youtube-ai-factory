package com.example.youtube

import com.example.data.VideoProject
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class YouTubeChannelProfile(val title: String, val handle: String)
data class YouTubeChannelStats(val title: String, val views: Long, val subscribers: Long, val videos: Long)
data class YouTubeAnalyticsReport(val views: Long, val watchHours: Double, val subscribersGained: Long, val averageViewPercentage: Double, val dailyViews: List<Pair<String, Long>>, val trafficSources: List<Pair<String, Long>>)

class YouTubeApiClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()

    fun getChannelProfile(accessToken: String): YouTubeChannelProfile {
        val json = getJson(
            "https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true",
            accessToken
        )
        val channel = json.optJSONArray("items")?.optJSONObject(0)
            ?: throw IllegalStateException("No YouTube channel was returned for this Google account.")
        val snippet = channel.optJSONObject("snippet")
        val title = snippet?.optString("title").orEmpty().ifBlank { "YouTube channel" }
        val handle = snippet?.optString("customUrl").orEmpty()
        return YouTubeChannelProfile(title, handle)
    }

    fun getChannelStats(accessToken: String): YouTubeChannelStats {
        val json = getJson(
            "https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics&mine=true",
            accessToken
        )
        val channel = json.optJSONArray("items")?.optJSONObject(0)
            ?: throw IllegalStateException("No YouTube channel statistics were returned.")
        val snippet = channel.optJSONObject("snippet")
        val stats = channel.optJSONObject("statistics")
        return YouTubeChannelStats(
            title = snippet?.optString("title").orEmpty().ifBlank { "YouTube channel" },
            views = stats?.optString("viewCount")?.toLongOrNull() ?: 0L,
            subscribers = stats?.optString("subscriberCount")?.toLongOrNull() ?: 0L,
            videos = stats?.optString("videoCount")?.toLongOrNull() ?: 0L
        )
    }

    fun getAnalytics(accessToken: String): YouTubeAnalyticsReport {
        val end = Calendar.getInstance()
        val start = end.clone() as Calendar
        start.add(Calendar.DAY_OF_YEAR, -27)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDate = dateFormat.format(start.time)
        val endDate = dateFormat.format(end.time)
        val daily = getJson("https://youtubeanalytics.googleapis.com/v2/reports?ids=channel%3D%3DMINE&startDate=$startDate&endDate=$endDate&metrics=views,estimatedMinutesWatched,subscribersGained,averageViewPercentage&dimensions=day&sort=day", accessToken)
        val rows = daily.optJSONArray("rows")
        var views = 0L
        var watchMinutes = 0.0
        var subscribersGained = 0L
        var weightedViewPercentage = 0.0
        var percentageWeight = 0L
        val dailyViews = mutableListOf<Pair<String, Long>>()
        if (rows != null) {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONArray(i) ?: continue
                if (row.length() < 5) continue
                val day = row.optString(0)
                val dayViews = row.optLong(1, 0L)
                views += dayViews
                watchMinutes += row.optDouble(2, 0.0)
                subscribersGained += row.optLong(3, 0L)
                val percent = row.optDouble(4, Double.NaN)
                if (!percent.isNaN() && dayViews > 0L) {
                    weightedViewPercentage += percent * dayViews
                    percentageWeight += dayViews
                }
                dailyViews += day to dayViews
            }
        }
        val trafficJson = getJson("https://youtubeanalytics.googleapis.com/v2/reports?ids=channel%3D%3DMINE&startDate=$startDate&endDate=$endDate&metrics=views&dimensions=insightTrafficSourceType&sort=-views", accessToken)
        val trafficRows = trafficJson.optJSONArray("rows")
        val trafficSources = mutableListOf<Pair<String, Long>>()
        if (trafficRows != null) {
            for (i in 0 until trafficRows.length()) {
                val row = trafficRows.optJSONArray(i) ?: continue
                if (row.length() < 2) continue
                trafficSources += row.optString(0) to row.optLong(1, 0L)
            }
        }
        return YouTubeAnalyticsReport(
            views = views,
            watchHours = watchMinutes / 60.0,
            subscribersGained = subscribersGained,
            averageViewPercentage = if (percentageWeight > 0L) weightedViewPercentage / percentageWeight else 0.0,
            dailyViews = dailyViews,
            trafficSources = trafficSources
        )
    }

    fun uploadVideo(
        accessToken: String,
        project: VideoProject,
        videoFile: File,
        privacyStatus: String,
        categoryId: String,
        onProgress: (Int) -> Unit
    ): String {
        require(videoFile.isFile && videoFile.length() > 0L) { "The rendered MP4 file is missing or empty." }
        val privacy = privacyStatus.lowercase().takeIf { it in setOf("public", "private", "unlisted") }
            ?: throw IllegalArgumentException("Choose a valid YouTube visibility setting.")
        val tags = project.tags.split(",", "\n", ";").map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val snippet = JSONObject()
            .put("title", project.title.take(100).ifBlank { "Untitled video" })
            .put("description", project.description.take(5000))
            .put("categoryId", categoryId)
            .put("tags", JSONArray(tags))
        val status = JSONObject().put("privacyStatus", privacy)
        val metadata = JSONObject().put("snippet", snippet).put("status", status)
            .toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val initRequest = Request.Builder()
            .url("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status")
            .header("Authorization", "Bearer $accessToken")
            .header("X-Upload-Content-Type", "video/mp4")
            .header("X-Upload-Content-Length", videoFile.length().toString())
            .post(metadata)
            .build()
        val uploadUrl = http.newCall(initRequest).execute().use { response ->
            if (!response.isSuccessful) throw apiError("Could not start YouTube upload", response)
            response.header("Location")
                ?: throw IllegalStateException("YouTube did not return a resumable upload URL.")
        }

        onProgress(5)
        val mediaBody = ProgressFileRequestBody(videoFile, "video/mp4".toMediaType()) { fraction ->
            onProgress(5 + (fraction * 90).toInt())
        }
        val uploadRequest = Request.Builder()
            .url(uploadUrl)
            .header("Authorization", "Bearer $accessToken")
            .put(mediaBody)
            .build()
        val result = http.newCall(uploadRequest).execute().use { response ->
            if (!response.isSuccessful) throw apiError("YouTube upload failed", response)
            JSONObject(response.body?.string().orEmpty()).optString("id").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("YouTube accepted the upload but returned no video ID.")
        }
        onProgress(100)
        return result
    }

    private fun getJson(url: String, accessToken: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        return http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw apiError("YouTube API request failed", response)
            JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun apiError(prefix: String, response: Response): IllegalStateException {
        val body = response.body?.string().orEmpty()
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty().ifBlank { body.take(300) }
        return IllegalStateException("$prefix (HTTP ${response.code}): $detail")
    }

    private class ProgressFileRequestBody(
        private val file: File,
        private val mediaType: MediaType,
        private val progress: (Float) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType = mediaType
        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val total = contentLength().coerceAtLeast(1L)
            var sent = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
            file.inputStream().use { input ->
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    sink.write(buffer, 0, count)
                    sent += count
                    progress((sent.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
    }
}
