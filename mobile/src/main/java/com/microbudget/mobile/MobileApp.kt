package com.microbudget.mobile

import android.app.Application
import com.microbudget.data.MicroBudgetGraph
import com.microbudget.domain.model.DeviceSource

class MobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MicroBudgetGraph.init(this, DeviceSource.PHONE)
    }
}
