package dev.esteki.kmos.sync.storage

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun sqliteDriver(): androidx.sqlite.driver.SQLiteDriver = BundledSQLiteDriver()
