[![kfunnels](https://raw.githubusercontent.com/lukashaertel/kfunnels/resource/kfunnels_logo_small_text.png)](https://github.com/lukashaertel/kfunnels/)

This is a sister project of [kfunnels](https://github.com/lukashaertel/kfunnels/), demonstrating the use on Android. To build and install use `./gradlew installDevDebug` or `gradlew.bat installDevDebug` respective to your platform.

### Android SDK requirements
The project uses `buildToolsVersion "26.0.1"` and the latest platform (API version 26). It also uses the `appcompat` dependency, so the support should also be installed.

## Android and kfunnels
Android does not play as nice with the service registry as Java. To register modules, they are added to the extras on application boot
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceModule.extra += AndroidModule
    }
}
```

The activity prepares the module with Rx support.

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Compose modules and add standards support.
        val module = (ReceiverModule then ServiceModule).std
```

Then, the elements are prepared for observable deserialization using the previously defined module.

```kotlin
        // Create an observable of events
        val observable = module.stream<Event> {
            // A new source is a JSON source for a parser on a URL, original labels are converted.
            JsonSource(JsonFactory().createParser(URL("https://app.eurofurence.org/Api/v2/Events")))
        }
```

The arry adapter is set up to feed into the created (or recycled) view.

```kotlin
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
```

In the end, the observable is linked into the array adapter, feeding multiple elements at a time and marking a data change on completion (as to not overflow the list with change notifications).

```kotlin

        // Assign data adapter
        dataOut.adapter = arrayAdapter

        // Put some things in the adapter
        observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .window(10)
                .subscribe {
                    // Use windowed feed so that dataset change is not announced too oftern
                    it.subscribe(
                            { arrayAdapter.add(it) },
                            {},
                            { arrayAdapter.notifyDataSetChanged() })
                }
```


        
