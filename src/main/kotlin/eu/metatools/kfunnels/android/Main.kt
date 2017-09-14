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
import eu.metatools.kfunnels.android.rx.ReceiverModule
import eu.metatools.kfunnels.android.rx.stream
import eu.metatools.kfunnels.base.ServiceModule
import eu.metatools.kfunnels.base.std
import eu.metatools.kfunnels.then
import eu.metatools.kfunnels.tools.json.JsonSource
import eu.metatools.kfunnels.tools.json.JsonSourceConfig
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.net.URL

val url = "https://github.com/lukashaertel/kfunnels-android/raw/data/bigevents"

class Main : AppCompatActivity() {
    /**
     * Lazy initializer for the list view.
     */
    val dataOut by lazy { findViewById(R.id.data_out) as ListView }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Compose modules and add standards support.
        val module = (ReceiverModule then ServiceModule).std

        // Create an observable of events
        val observable = module.stream<Event> {
            // A new source is a JSON source for a parser on a URL, original labels are converted.
            JsonSource(JsonFactory().createParser(URL(url)),
                    JsonSourceConfig.upperToLower)
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
                val description by lazy { view.findViewById<TextView>(R.id.description) }

                top.text = event.title
                bottom.text = event.subTitle
                description.text = event.description

                return view
            }
        }

        // Assign data adapter
        dataOut.adapter = arrayAdapter

        // Put some things in the adapter
        observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .window(5)
                .subscribe {
                    // Use windowed feed so that dataset change is not announced too oftern
                    it.subscribe(
                            { arrayAdapter.add(it) },
                            {},
                            { arrayAdapter.notifyDataSetChanged() })
                }

    }
}
