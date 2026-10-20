package com.microbudget.wear

import android.app.Application
import com.microbudget.data.MicroBudgetGraph
import com.microbudget.domain.model.DeviceSource

class WearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MicroBudgetGraph.init(this, DeviceSource.WATCH)
    }
}
