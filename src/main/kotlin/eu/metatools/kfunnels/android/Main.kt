package eu.metatools.kfunnels.android

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import com.fasterxml.jackson.core.JsonFactory
import eu.metatools.kfunnels.base.ServiceModule
import eu.metatools.kfunnels.read
import eu.metatools.kfunnels.std
import eu.metatools.kfunnels.tools.json.JsonSource
import java.net.URL
import kotlin.concurrent.thread
import android.view.LayoutInflater
import android.widget.TextView
import eu.metatools.kfunnels.android.rx.ReceiverModule
import eu.metatools.kfunnels.android.rx.stream
import eu.metatools.kfunnels.then
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class Main : AppCompatActivity() {
    val dataOut by lazy { findViewById(R.id.data_out) as ListView }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val module = (ReceiverModule then ServiceModule).std

        // Create an observable of events
        val observable = module.stream<Event> {
            // A new source is a JSON source for a parser on a URL, original labels are converted.
            JsonSource(JsonFactory().createParser(URL("https://app.eurofurence.org/Api/v2/Events")))
        }

        // Make array adapter feeding into the thing
        val arrayAdapter = object : ArrayAdapter<Event>(this, 0) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val event = getItem(position)
                val view = convertView ?: LayoutInflater
                        .from(getContext())
                        .inflate(R.layout.event, parent, false);
                val top by lazy { view.findViewById<TextView>(R.id.top) }
                val bottom by lazy { view.findViewById<TextView>(R.id.bottom) }

                top.text = event.title
                bottom.text = event.subTitle

                return view
            }
        }

        // Assign data adapter
        dataOut.adapter = arrayAdapter

        // Put some things in the adapter
        observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    arrayAdapter.add(it)
                    arrayAdapter.notifyDataSetChanged()
                }

    }
}
