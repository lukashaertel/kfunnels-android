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
import eu.metatools.kfunnels.base.ServiceModule
import eu.metatools.kfunnels.base.std
import eu.metatools.kfunnels.read
import eu.metatools.kfunnels.tools.json.JsonSource
import eu.metatools.kfunnels.tools.json.JsonSourceConfig
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.URL
import kotlin.concurrent.thread

class MainJson : AppCompatActivity() {
    /**
     * Lazy initializer for the list view.
     */
    val dataOut by lazy { findViewById(R.id.data_out) as ListView }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Make array adapter feeding into the thing
        val arrayAdapter = object : ArrayAdapter<Event>(this, 0) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val event = getItem(position)
                val view = convertView ?: LayoutInflater.from(getContext())
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


        doAsync {
            JsonFactory().createParser(URL(url)).use {
                JsonSource(it, JsonSourceConfig.upperToLower).let {
                    ServiceModule.std.read<List<Event>>(it)
                }
            }.let { items ->
                uiThread {
                    arrayAdapter.addAll(items)
                    arrayAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}