package nz.t1d.di

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import nz.t1d.datamodels.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisplayDataRepository @Inject constructor(@ApplicationContext context: Context) {

    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val TAG = "DisplayDataRepository"

    // store of listeners for changes in the display data
    val listeners = mutableListOf<() -> Unit>()

    private val t1d = T1DModel.Builder().build()

    val bglReadings: SortedSet<GlucoseReading>
      get() = t1d.bglReadings()

    val recentEvents: SortedSet<BaseDataClass>
      get() = t1d.recentEvents()

    val timeInRange: Float
      get() = t1d.timeInRange()

    val meanBGL: Float
      get() = t1d.meanBGL()

    val stdBGL: Float
      get() = t1d.stdBGL()

    // Update the body BGL via notification from Dexcom notif
    fun newNotificationAvailable(nd: NotificationData) {
        t1d.addBGlReading(nd.reading, nd.time, nd.unit, DATA_SOURCE.CAMAPS_NOTIF) // TODO add dexcom as datasource
        // Update listeners something has changes
        updatedListeners()
    }

    fun updatedListeners() {
        listeners.forEach { it() }
    }

    var insulinOnBoardBolus: Float = 0f
    var insulinOnBoardBasal: Float = 0f
    var insulinCurrentBasal: Float = 0f

    fun addPatientData(patientData: Data) {
        t1d.addData(patientData)
        t1d.removeOldData()
        t1d.processData()
        updatedListeners()
    }


}


