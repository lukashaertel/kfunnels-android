package eu.metatools.kfunnels.android

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import com.fasterxml.jackson.core.JsonFactory
import com.google.common.util.concurrent.Futures
import com.squareup.picasso.Picasso
import eu.metatools.kfunnels.base.ServiceModule
import eu.metatools.kfunnels.base.std
import eu.metatools.kfunnels.read
import eu.metatools.kfunnels.tools.iraw
import eu.metatools.kfunnels.tools.json.JsonSource
import eu.metatools.kfunnels.tools.json.JsonSourceConfig
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.recyclerview.v7.recyclerView
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

/**
 * The relevant information to display an event in the list.
 */
data class EventStub(val id: String,
                     val title: String?,
                     val subTitle: String?,
                     val startDateTimeUTC: String?,
                     val start: String?,
                     val endDateTimeUTC: String?,
                     val end: String?,
                     val description: String?,
                     val bannerImageId: String?,
                     val conferenceRoomId: String?)

/**
 * Converts the event to a stub.
 */
fun Event.toStub() = EventStub(id, title, subTitle, startDateTimeUtc, startTime, endDateTimeUtc, endTime, description, bannerImageId, conferenceRoomId)

fun View.gridLparams(
        rowStart: Int = GridLayout.UNDEFINED,
        rowSpan: Int = 1,
        rowAlign: GridLayout.Alignment = GridLayout.BASELINE,
        rowWeight: Float = 0.0f,
        colStart: Int = GridLayout.UNDEFINED,
        colSpan: Int = 1,
        colAlign: GridLayout.Alignment = GridLayout.START,
        colWeight: Float = 0.0f) {
    layoutParams = GridLayout.LayoutParams(
            GridLayout.spec(rowStart, rowSpan, rowAlign, rowWeight),
            GridLayout.spec(colStart, colSpan, colAlign, colWeight))
}

class Main : AppCompatActivity() {
    private val boot = Date().time
    private val events by lazy { iraw(File(filesDir, "events"), Event::id) }
    private val images by lazy { iraw(File(filesDir, "images"), Image::id) }
    private val rooms by lazy { iraw(File(filesDir, "rooms"), Room::id) }

    val eventAdapter = ListAutoAdapter(EventStub::title) {
        frameLayout {

            lparams(matchParent, wrapContent)

            cardView {
                layoutParams = ViewGroup.MarginLayoutParams(matchParent, wrapContent).apply {
                    horizontalMargin = dip(8)
                    verticalMargin = dip(4)
                }

                verticalLayout {
                    layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)

                    imageView {
                        // Cancel request for detached views
                        reset {
                            Picasso.with(ctx).cancelRequest(this)
                        }

                        layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)

                        visibility = View.GONE
                        scaleType = ImageView.ScaleType.CENTER_CROP

                        from { bannerImageId } into {
                            // Cancel existing requests
                            Picasso.with(ctx)
                                    .cancelRequest(this)

                            // Start new requests
                            val image = if (it == null) null else images.find(it)
                            if (image != null) {
                                visibility = View.VISIBLE
                                Picasso.with(ctx)
                                        .load("https://app.eurofurence.org/Api/v2/Images/${image.id}/Content")
                                        .into(this)
                            } else {
                                visibility = View.GONE
                                setImageDrawable(null)
                            }
                        }
                    }


                    gridLayout {
                        layoutParams = ViewGroup.LayoutParams(matchParent, matchParent)
                        columnCount = 3
                        padding = dip(8)


                        textView {
                            gridLparams(colSpan = 3)
                            textSize = 18f
                            textColor = Color.BLACK
                            setTypeface(typeface, Typeface.BOLD)
                            from { title } into { text = it }
                        }

                        textView {
                            gridLparams(colSpan = 3)
                            textSize = 12f
                            from { subTitle } into { text = it }
                        }


                        textView {
                            gridLparams(colWeight = 1f)
                            textSize = 12f
                            setTypeface(typeface, Typeface.ITALIC)
                            from { start } into { text = it }
                        }

                        textView {
                            gridLparams(colWeight = 1f)
                            textSize = 12f
                            setTypeface(typeface, Typeface.ITALIC)
                            from { end } into { text = it }
                        }

                        textView {
                            gridLparams(colWeight = 2f)
                            textSize = 12f
                            setTypeface(typeface, Typeface.ITALIC)
                            from { conferenceRoomId } into {
                                val room = if (it == null) null else rooms.find(it)
                                text = room?.name
                            }
                        }
                        textView {
                            gridLparams(colSpan = 3)
                            reset {
                                (tag as? Timer)?.cancel()
                                tag = null
                            }
                            from { startDateTimeUTC to endDateTimeUTC } into { (s, e) ->
                                if (s != null || e != null) {
                                    val timer = Timer()
                                    tag = timer
                                    timer.scheduleAtFixedRate(0L, 1000L) {
                                        val dp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US)
                                        val now = dp.parse("2017-08-18T16:00:00.000Z").time + Date().time - boot
                                        val start = dp.parse(s).time
                                        val end = dp.parse(e).time
                                        val fraction = (now - start).toDouble() / (end - start)
                                        runOnUiThread {
                                            if (fraction < 0)
                                                text = "Starting in a bit"
                                            else if (fraction < 1)
                                                text = "Going on ${Math.round(fraction * 1000.0) / 10.0}%"
                                            else
                                                text = "Over"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        verticalLayout {
            backgroundColor = Color.GRAY
            lparams(matchParent, matchParent)
            recyclerView {
                layoutManager = LinearLayoutManager(ctx)
                adapter = eventAdapter
            }.lparams(matchParent, matchParent)
        }

        // Get supplemental data
        val imagesFuture = if (!images.exists())
            doAsync {
                // Otherwise, load from API
                val url = "https://app.eurofurence.org/Api/v2/Images"
                val items = JsonFactory().createParser(URL(url)).use {
                    ServiceModule.std.read<List<Image>>(JsonSource(it, JsonSourceConfig.upperToLower))
                }
                images.save(items)
            }
        else
            Futures.immediateFuture(Unit)

        val roomsFuture = if (!rooms.exists())
            doAsync {
                // Otherwise, load from API
                val url = "https://app.eurofurence.org/Api/v2/EventConferenceRooms"
                val items = JsonFactory().createParser(URL(url)).use {
                    ServiceModule.std.read<List<Room>>(JsonSource(it, JsonSourceConfig.upperToLower))
                }
                rooms.save(items)
            }
        else
            Futures.immediateFuture(Unit)

        // Wait for both
        imagesFuture.get()
        roomsFuture.get()

        // Check if events are loaded
        if (events.exists()) {
            // If loaded, stream them into the adapter
            events.stream()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        val p = eventAdapter.list.binarySearchBy(it.startDateTimeUtc) { it.startDateTimeUTC }
                        if (p < 0) {
                            eventAdapter.list.add(-p - 1, it.toStub())
                            eventAdapter.notifyItemInserted(-p - 1)
                        } else {
                            eventAdapter.list.add(p + 1, it.toStub())
                            eventAdapter.notifyItemInserted(p + 1)
                        }
                    }
        } else {
            doAsync {

                // Otherwise, load from API
                val url = "https://app.eurofurence.org/Api/v2/Events"
                val items = JsonFactory().createParser(URL(url)).use {
                    ServiceModule.std.read<List<Event>>(JsonSource(it, JsonSourceConfig.upperToLower))
                }

                // Add to the adapter
                uiThread {
                    // TODO: Assuming clear list
                    eventAdapter.list.addAll(items.sortedBy { it.startDateTimeUtc }.map(Event::toStub))
                    eventAdapter.notifyDataSetChanged()
                }

                // Also save to the store
                events.save(items)
            }
        }
    }
}
