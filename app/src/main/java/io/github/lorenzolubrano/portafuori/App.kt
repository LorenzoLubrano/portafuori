package io.github.lorenzolubrano.portafuori

import android.app.Application
import io.github.lorenzolubrano.portafuori.data.PortafuoriDb
import io.github.lorenzolubrano.portafuori.data.Repository
import io.github.lorenzolubrano.portafuori.data.Settings
import io.github.lorenzolubrano.portafuori.reminders.Engine
import io.github.lorenzolubrano.portafuori.reminders.Notifications
import io.github.lorenzolubrano.portafuori.reminders.WatchdogWorker

class App : Application() {
    lateinit var repository: Repository
        private set
    lateinit var settings: Settings
        private set

    override fun onCreate() {
        super.onCreate()
        settings = Settings(this)
        // Every data change re-plans the alarm chain and refreshes the widget
        repository = Repository(PortafuoriDb.create(this)) { Engine.run(this) }
        Notifications.createChannels(this)
        WatchdogWorker.enqueue(this)
    }
}
