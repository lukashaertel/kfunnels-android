package eu.metatools.kfunnels.android

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.fasterxml.jackson.core.JsonFactory
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.gson.GsonDeserializer
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpGet
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import eu.metatools.kfunnels.android.rx.ReceiverModule
import eu.metatools.kfunnels.base.ServiceModule
import eu.metatools.kfunnels.then
import eu.metatools.kfunnels.tools.json.JsonSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.net.URL

data class EventGson(
        var LastChangeDateTimeUtc: String,
        var Id: String,
        var Slug: String?,
        var Title: String?,
        var SubTitle: String?,
        var Abstract: String?,
        var ConferenceDayId: String?,
        var ConferenceTrackId: String?,
        var ConferenceRoomId: String?,
        var Description: String?,
        var Duration: String?,
        var StartTime: String?,
        var EndTime: String?,
        var StartDateTimeUtc: String?,
        var EndDateTimeUtc: String?,
        var PanelHosts: String?,
        var IsDeviatingFromConBook: Boolean?,
        var BannerImageId: String?,
        var PosterImageId: String?) { //User Deserializer
    companion object {
        val gson = Gson()
    }

    object ListDeserializer : ResponseDeserializable<List<EventGson>> {

        override fun deserialize(content: String) = gson.fromJson<List<EventGson>>(content,
                object : TypeToken<List<EventGson>>() {}.type)
    }

    override fun toString() = Title ?: Id
}

class MainGson : AppCompatActivity() {
    /**
     * Lazy initializer for the list view.
     */
    val dataOut by lazy { findViewById(R.id.data_out) as ListView }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Make array adapter feeding into the thing
        val arrayAdapter = object : ArrayAdapter<EventGson>(this, 0) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val event = getItem(position)
                val view = convertView ?: LayoutInflater.from(getContext())
                        .inflate(R.layout.event, parent, false);
                val top by lazy { view.findViewById<TextView>(R.id.top) }
                val bottom by lazy { view.findViewById<TextView>(R.id.bottom) }
                val description by lazy { view.findViewById<TextView>(R.id.description) }

                top.text = event.Title
                bottom.text = event.SubTitle
                description.text = event.Description

                return view
            }
        }

        // Assign data adapter
        dataOut.adapter = arrayAdapter

        Fuel.get(url)
                .responseObject(EventGson.ListDeserializer) { _, _, r ->
                    r.fold({
                        runOnUiThread {
                            arrayAdapter.addAll(it)
                            arrayAdapter.notifyDataSetChanged()
                        }
                    }, {})
                }
    }
}