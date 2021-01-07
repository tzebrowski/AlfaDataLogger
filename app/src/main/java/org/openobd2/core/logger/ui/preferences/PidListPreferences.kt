package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.pid.PidRegistry
import java.util.*

class PidListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {
    init {
        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        Thread.currentThread().contextClassLoader
            .getResourceAsStream("generic.json").use { source ->
                val registry = PidRegistry.builder().source(source).build()
                registry.definitions. sortedBy { pidDefinition ->  pidDefinition.description}
                    .forEach {p ->
                        entries.add(p.description)
                        entriesValues.add(p.pid)
                    }
            }
        setEntries(entries.toTypedArray())
        setEntryValues(entriesValues.toTypedArray())
    }
}