package nz.t1d.ns

import android.content.Context
import nz.t1d.clients.ns.NightscoutClient
import nz.t1d.datamodels.Data
import nz.t1d.datamodels.IOBTotals

import androidx.preference.PreferenceManager

import dagger.hilt.android.qualifiers.ApplicationContext


import java.time.LocalDateTime

import java.util.*

import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NightscoutClient @Inject constructor(@ApplicationContext context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val nightscoutClient: NightscoutClient by lazy {
        val url = prefs.getString("nightscout_url", "")!!

        NightscoutClient(url = url)
    }


    suspend fun getIOB(): IOBTotals {
        return nightscoutClient.getIOB()
    }

    suspend fun getCurrentBasal(): Float {
        return nightscoutClient.getCurrentBasal()
    }

    suspend fun getPatientData(date_from: LocalDateTime, date_to: LocalDateTime): Data {

        var entries = nightscoutClient.getEntries(date_from, date_to)
        var treatments = nightscoutClient.getTreatments(date_from, date_to)
        treatments.merge(entries)
        return treatments
    }
}