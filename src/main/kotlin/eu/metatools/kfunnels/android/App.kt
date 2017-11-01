package eu.metatools.kfunnels.android

import android.app.Application
import eu.metatools.kfunnels.Funnelable
import eu.metatools.kfunnels.base.ServiceModule

@Funnelable
data class Event(
        val lastChangeDateTimeUtc: String,
        val id: String,
        val slug: String?,
        val title: String?,
        val subTitle: String?,
        val abstract: String?,
        val conferenceDayId: String?,
        val conferenceTrackId: String?,
        val conferenceRoomId: String?,
        val description: String?,
        val duration: String?,
        val startTime: String?,
        val endTime: String?,
        val startDateTimeUtc: String?,
        val endDateTimeUtc: String?,
        val panelHosts: String?,
        val isDeviatingFromConBook: Boolean?,
        val bannerImageId: String?,
        val posterImageId: String?) {
    override fun toString() = title ?: id
}

@Funnelable
data class Image(
        val lastChangeDateTimeUtc: String,
        val id: String,
        val internalReference: String?,
        val width: Int,
        val height: Int,
        val sizeInBytes: Long,
        val mimeType: String?,
        val contentHashSha1: String?) {
    override fun toString() = internalReference ?: id
}

@Funnelable
data class Room(
        val lastChangeDateTimeUtc: String,
        val id: String,
        val name: String?) {
    override fun toString() = name ?: id
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceModule.extra += AndroidModule
    }
}

