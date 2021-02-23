package org.openobd2.core.logger.bl

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import org.obd.metrics.DeviceProperties
import org.obd.metrics.Lifecycle
import org.obd.metrics.ObdMetric
import org.obd.metrics.api.EcuSpecific
import org.obd.metrics.api.Workflow
import org.obd.metrics.api.WorkflowContext
import org.obd.metrics.api.WorkflowFactory
import org.obd.metrics.command.group.AlfaMed17CommandGroup
import org.obd.metrics.command.group.Mode1CommandGroup
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidRegistry
import org.obd.metrics.statistics.StatisticsAccumulator
import org.openobd2.core.logger.ui.preferences.PreferencesHelper

const val NOTIFICATION_CONNECTED = "data.logger.connected"
const val NOTIFICATION_CONNECTING = "data.logger.connecting"
const val NOTIFICATION_STOPPED = "data.logger.stopped"
const val NOTIFICATION_STOPPING = "data.logger.stopping"
const val NOTIFICATION_ERROR = "data.logger.error"
const val LOG_KEY = "DATA_LOGGER_DL"
const val GENERIC_MODE = "Generic mode"


class DataLogger internal constructor() {

    companion object {
        @JvmStatic
        var INSTANCE: DataLogger =
            DataLogger()
    }


    private lateinit var context: Context

    private var metricsAggregator = MetricsAggregator()

    private var lifecycle = object : Lifecycle {
        override fun onConnecting() {
            Log.i(LOG_KEY, "Start collecting process for the Device: $device")
            metricsAggregator.data.clear()
            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_CONNECTING
            })
        }

        override fun onConnected(deviceProperties: DeviceProperties) {
            Log.i(LOG_KEY, "We are connected to the device: $deviceProperties")
            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_CONNECTED
            })
        }

        override fun onError(msg: String, tr: Throwable?) {
            Log.e(
                LOG_KEY,
                "An error occurred during interaction with the device. Msg: $msg"
            )
            workflow().stop()
            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_ERROR
            })
        }

        override fun onStopped() {
            Log.i(
                LOG_KEY,
                "Collecting process completed for the Device: $device"
            )

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_STOPPED
            })
        }

        override fun onStopping() {
            Log.i(LOG_KEY, "Stop collecting process for the Device: $device")

            context.sendBroadcast(Intent().apply {
                action = NOTIFICATION_STOPPING
            })
        }
    }

    private var mode1: Workflow =
        WorkflowFactory.mode1().equationEngine("rhino")
            .ecuSpecific(
                EcuSpecific
                    .builder()
                    .initSequence(Mode1CommandGroup.INIT)
                    .pidFile("mode01.json").build()
            )
            .observer(metricsAggregator)
            .lifecycle(lifecycle)
            .commandFrequency(80)
            .initialize()

    private var mode22: Workflow = WorkflowFactory
        .generic()
        .ecuSpecific(
            EcuSpecific
                .builder()
                .initSequence(AlfaMed17CommandGroup.CAN_INIT)
                .pidFile("alfa.json").build()
        )
        .equationEngine("rhino")
        .observer(metricsAggregator)
        .commandFrequency(80)
        .lifecycle(lifecycle).initialize()

    private lateinit var device: String

    fun statistics(): StatisticsAccumulator {
        return workflow().statistics
    }

    fun buildMetricsBy(pids: Set<Long>): MutableList<ObdMetric> {
        var pidRegistry: PidRegistry = pids()
        var data: MutableList<ObdMetric> = arrayListOf()
        pids.forEach { s: Long? ->
            pidRegistry.findBy(s)?.apply {
                data.add(ObdMetric.builder().command(ObdCommand(this)).build())
            }
        }
        return data
    }

    fun pids(): PidRegistry {
        return workflow().pids
    }

    fun stop() {
        workflow().stop()
    }

    fun init(ctx: Context) {
        this.context = ctx
    }

    fun start() {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        var adapterName = pref.getString("pref.adapter.id", "OBDII")
        this.device = adapterName.toString()

        when (PreferencesHelper.getMode(context)) {
            GENERIC_MODE -> {
                var selectedPids = pref.getStringSet("pref.pids.generic", emptySet())!!
                Log.i(LOG_KEY, "Generic mode, selected pids: $selectedPids")

                var ctx = WorkflowContext.builder()
                    .filter(selectedPids.map { s -> s.toLong() }.toSet())
                    .batchEnabled(PreferencesHelper.isBatchEnabled(context))
                    .connection(BluetoothConnection(device.toString())).build()
                mode1.start(ctx)
            }

            else -> {
                var selectedPids = pref.getStringSet("pref.pids.mode22", emptySet())!!

                Log.i(LOG_KEY, "Mode 22, selected pids: $selectedPids")
                var ctx = WorkflowContext.builder()
                    .filter(selectedPids.map { s -> s.toLong() }.toSet())
                    .batchEnabled(PreferencesHelper.isBatchEnabled(context))
                    .connection(BluetoothConnection(device.toString())).build()
                mode22.start(ctx)

            }
        }

        Log.i(LOG_KEY, "Start collecting process for device $adapterName")
    }

    private fun workflow(): Workflow {
        context.let {

            return when (PreferencesHelper.getMode(context)) {
                GENERIC_MODE -> {
                    mode1
                }
                else -> {
                    mode22
                }
            }
        }

    }
}