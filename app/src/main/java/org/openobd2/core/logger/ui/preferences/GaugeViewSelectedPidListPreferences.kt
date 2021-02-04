package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.DataLoggerService
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GaugeViewSelectedPidListPreferences @Inject constructor(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {

    @Inject
    lateinit var dataLogger: DataLogger

    init {
        if (false) {
            val entries: MutableList<CharSequence> =
                LinkedList()
            val entriesValues: MutableList<CharSequence> =
                LinkedList()

            dataLogger.pids().definitions.sortedBy { pidDefinition -> pidDefinition.description }
                .forEach { p ->
                    entries.add(p.description)
                    entriesValues.add(p.pid)
                }

            val default = hashSetOf<String>().apply {
                add("05")//Engine coolant temperature
                add("0B") //Intake manifold absolute pressure
                add("0C") //Engine RPM
                add("0F") //Intake air temperature
                add("11") //Throttle position
            }

            setDefaultValue(default)

            setEntries(entries.toTypedArray())
            entryValues = entriesValues.toTypedArray()
        }
    }
}
