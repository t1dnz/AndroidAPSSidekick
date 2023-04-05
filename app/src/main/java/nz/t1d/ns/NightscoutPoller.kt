package nz.t1d.di

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nz.t1d.ns.NightscoutClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NightscoutPoller @Inject constructor(
    @ApplicationContext context: Context,
    val nightscoutClient: NightscoutClient,
    val ddr: DisplayDataRepository
) {
    private var TAG = "NightscoutPoller"
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    var scope = MainScope()

    private var poller: Job? = null
    private var updater: Job? = null
    private var from: LocalDateTime? = null
    /*
        Rules for fetching Nightscout Data
        1. Initially we fetch all of todays data (if any data with DIA time (about 10 hours) is in previous dat get that)
        3. fetch more recent data every 5 minutes
     */

    fun start_nightscout_poller() {
        // poll nightscout 5 mins

        Log.d(TAG, "Poller starting")

        if (from == null) {
            from = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
            // Either use midnight or 10 hours ago which ever is older
            var fromMinusDIA = LocalDateTime.now().minusHours(10)
            if (from!! > fromMinusDIA) {
                from = fromMinusDIA
            }
        }

        if (poller == null) {
            poller = scope.launch {
                while (isActive) {
                    if (!nightscoutEnabled()) {
                        Log.d(TAG, "Skipping loop Nightscout not enabled")
                        delay(1 * 60 * 1000) // wait a minute
                        continue
                    }

                    fetchAndPopulateData()

                    var waitTime: Long = 300  // 5 mins
                    if (ddr.bglReadings.size > 0) {
                        // try align the delay of the call with the previous bgl reading
                        val first = ddr.bglReadings.first()
                        waitTime = waitTime - first.secondsAgo().mod(300)
                    }
                    waitTime += 30 // Add 15 seconds to give time to sync up
                    Log.d(TAG, "WAITING $waitTime seconds until calling nightscout again")
                    delay(waitTime * 1000)

                }
            }
        }
        if (updater == null) {
            updater = scope.launch {
                while (isActive) {
                    Log.d(TAG, "Updating listeners")
                    ddr.updatedListeners()
                    delay(30 * 1000) // every 30 seconds update ui
                }
            }
        }

    }


    fun fetchData() : Job {
        return scope.launch {
            fetchAndPopulateData()
        }
    }

    suspend fun fetchAndPopulateData() {
        if (!nightscoutEnabled()) {
            return
        }

        try {
            val to = LocalDateTime.now().plusMinutes(10)

            val fromz = from!!.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
            val toz = to.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
            Log.d(TAG, "Fetch data from nightscout now=$to from=$from nowUTC=$toz fromUTC=$fromz")
            var patientData = nightscoutClient.getPatientData(
                fromz,
                toz,
            )

            ddr.addPatientData(patientData)

            var iob = nightscoutClient.getIOB()
            
            ddr.insulinOnBoardBolus = iob.bolusIOB
            ddr.insulinOnBoardBasal = iob.basalIOB
            ddr.insulinCurrentBasal = nightscoutClient.getCurrentBasal()

            // change from to be to 30 mins ago
            from = LocalDateTime.now().minusMinutes(30)
        } catch (e: Throwable) {
            Log.e(TAG, "Error", e)
        }
    }

    fun nightscoutEnabled(): Boolean {
        return prefs.getBoolean("nightscout_enable", false)
    }

    fun stop_nightscout_poller() {
        poller?.cancel()
        poller = null
        updater?.cancel()
        updater = null

        Log.d(TAG, "Poller stopping")
    }

}