package dev.esteki.kmos.worker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

expect fun createSqlJsWorker(): WebWorkerSQLiteDriver