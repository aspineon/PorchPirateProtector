package no.bakkenbaeck.pppshared.presenter

import kotlinx.coroutines.*
import no.bakkenbaeck.pppshared.ApplicationDispatcher
import no.bakkenbaeck.pppshared.api.Api
import no.bakkenbaeck.pppshared.interfaces.SecureStorage
import kotlin.coroutines.CoroutineContext

// Ganked from https://github.com/JetBrains/kotlinconf-app/blob/master/common/src/commonMain/kotlin/org/jetbrains/kotlinconf/presentation/CoroutinePresenter.kt

open class BaseCoroutinePresenter(
    private val mainContext: CoroutineContext = ApplicationDispatcher
): CoroutineScope {

    val api = Api()

    private val job = Job()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        print(throwable)
    }

    override val coroutineContext: CoroutineContext
        get() = mainContext + job + exceptionHandler

    open fun onDestroy() {
        job.cancel()
    }

    fun throwingToken(secureStorage: SecureStorage): String {
        secureStorage.fetchTokenString()?.let {
            return it
        } ?: throw RuntimeException("Couldn't find token!")
    }
}