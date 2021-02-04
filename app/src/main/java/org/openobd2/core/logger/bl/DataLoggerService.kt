package org.openobd2.core.logger.bl

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val ACTION_START = "org.openobd2.core.logger.ui.action.START"
private const val ACTION_STOP = "org.openobd2.core.logger.ui.action.STOP"

@AndroidEntryPoint
class DataLoggerService : IntentService("DataLoggerService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_START -> {
                dataLogger.start()
            }
            ACTION_STOP -> {
                Log.i(LOG_KEY, "Stop collecting process")
                dataLogger.stop()
            }
        }
    }

    @Inject
    lateinit var dataLogger: DataLogger

    companion object {

        @JvmStatic
        fun startAction(context: Context) {
            val intent = Intent(context, DataLoggerService::class.java).apply {
                action = ACTION_START
            }
            context.startService(intent)
        }

        @JvmStatic
        fun stopAction(context: Context) {
            val intent = Intent(context, DataLoggerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}