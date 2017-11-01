package eu.metatools.kfunnels.android

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.AnkoContext

/**
 * Binding composer, see [from] and [into].
 * @param delegate The context to create the view with.
 */
class AutoBehaviour<E>(delegate: AnkoContext<ViewGroup>) : AnkoContext<ViewGroup> by delegate {

    /**
     * Backing for the resetter.
     */
    private var resetterBacking: () -> Unit = {}

    /**
     * Backing for the binder.
     */
    private var binderBacking: (E) -> Unit = {}

    /**
     * Gets the current binder, a composed method setting the properties in the given view.
     */
    val binder: (E) -> Unit
        get() = {
            resetterBacking()
            binderBacking(it)
        }

    /**
     * Gets the current resetter, a composed method detaching the given view.
     */
    val resetter: () -> Unit get() = resetterBacking

    /**
     * In the context of an auto binder receiver, helps shorten the definition of a generator.
     */
    fun <V> from(f: E.() -> V) = f

    /**
     * Binds a generator to a receiver.
     */
    infix fun <V> (E.() -> V).into(receiver: (V) -> Unit) {
        val current = binderBacking
        binderBacking = {
            current(it)
            receiver(this(it))
        }
    }

    /**
     * Adds a resetter.
     */
    fun reset(block: () -> Unit) {
        val current = resetterBacking
        resetterBacking = {
            current()
            block()
        }
    }
}

/**
 * View holder binding via an auto binder.
 * @property autoBehaviour The auto binder used to bind the view.
 * @property view The view that is bound and held.
 */
class AutoViewHolder<E>(val autoBehaviour: AutoBehaviour<E>, view: View) : RecyclerView.ViewHolder(view)

/**
 * Base class for auto adapters.
 * @property id The identity computation for the element.
 * @property create The view creation and binding composition function.
 */
abstract class AutoAdapter<E>(val id: E.() -> Any?, val create: AutoBehaviour<E>.() -> View) : RecyclerView.Adapter<AutoViewHolder<E>>() {
    init {
        super.setHasStableIds(true)
    }

    override final fun setHasStableIds(hasStableIds: Boolean) {
        throw IllegalStateException("Trying to reset stable IDs.")
    }

    abstract fun getItem(position: Int): E

    override fun getItemId(position: Int): Long {
        val from = getItem(position).id()

        return when (from) {
            null -> 0L
            is Long -> from
            is Number -> from.toLong()
            is String -> from.substring(0, from.length / 2).hashCode().toLong() shl 32 or
                    from.substring(from.length / 2).hashCode().toLong()
            else -> from.hashCode().toLong()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutoViewHolder<E> {
        val context = AnkoContext.createReusable(parent.context, parent, false)
        val autoBinder = AutoBehaviour<E>(context)
        val view = autoBinder.create()
        return AutoViewHolder(autoBinder, view)
    }

    override fun onBindViewHolder(holder: AutoViewHolder<E>, position: Int) {
        holder.autoBehaviour.binder(getItem(position))
    }

    override fun onViewDetachedFromWindow(holder: AutoViewHolder<E>?) {
        if (holder != null)
            holder.autoBehaviour.resetter()
    }
}

/**
 * A list auto adapter.
 * @param id The identity computation for the element.
 * @param create The view creation and binding composition function.
 */
class ListAutoAdapter<E>(id: E.() -> Any?, create: AutoBehaviour<E>.() -> View) : AutoAdapter<E>(id, create) {
    val list = mutableListOf<E>()

    override fun getItemCount() = list.size

    override fun getItem(position: Int) = list[position]
}