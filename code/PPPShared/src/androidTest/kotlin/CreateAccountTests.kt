package no.bakkenbaeck.pppshared

import no.bakkenbaeck.pppshared.presenter.*
import no.bakkenbaeck.pppshared.view.CreateAccountView
import no.bakkenbaeck.pppshared.validator.*
import kotlin.test.*
import kotlinx.coroutines.*
import no.bakkenbaeck.pppshared.api.Api
import no.bakkenbaeck.pppshared.manager.TokenManager
import org.junit.Before

class CreateAccountTests {

    class TestCreateAccountView : CreateAccountView {

        override var email: String? = null
        override var password: String? = null
        override var confirmPassword: String? = null

        var apiError: String? = null
        override fun apiErrorUpdated(toString: String?) {
            apiError = toString
        }

        var emailError: String? = null
        override fun emailErrorUpdated(toString: String?) {
            emailError = toString
        }

        var passwordError: String? = null
        override fun passwordErrorUpdated(toString: String?) {
            passwordError = toString
        }

        var confirmPasswordError: String? = null
        override fun confirmPasswordErrorUpdated(toString: String?) {
            confirmPasswordError = toString
        }

        var accountCreationSucceeded = false
        override fun accountSuccessfullyCreated() {
            accountCreationSucceeded = true
        }

        var loadingSpinnerGoing = false

        var loadingSpinnerWasStarted = false
        override fun startLoadingIndicator() {
            loadingSpinnerGoing = true
            loadingSpinnerWasStarted = true
        }

        var loadingSpinnerWasStopped = false
        override fun stopLoadingIndicator() {
            loadingSpinnerGoing = false
            loadingSpinnerWasStopped = true
        }

    }

    @Before
    fun setup() {
        TokenManager.clearToken()
    }

    @Test
    fun validationSetsProperErrorsThenClearsThemAfterChangesMade() {
        val view = TestCreateAccountView()
        val presenter = CreateAccountPresenter(view)

        presenter.validateAllInput()

        val expectedEmailError = ValidationResult.Invalid.WasNull("email")
        val expectedPasswordError = ValidationResult.Invalid.WasNull("password")
        val initialConfirmPasswordError = ValidationResult.Invalid.WasNull("confirm password")

        // Should kick off all validations
        assertFalse(presenter.isCurrentInputValid())

        assertEquals(expectedEmailError.reason, view.emailError)
        assertEquals(expectedPasswordError.reason, view.passwordError)
        assertEquals(initialConfirmPasswordError.reason, view.confirmPasswordError)

        view.confirmPassword = "aaaa"
        presenter.validateConfirmPassword()
        val secondConfirmError = ValidationResult.Invalid.WasNull("password")
        assertEquals(expectedEmailError.reason, view.emailError)
        assertEquals(expectedPasswordError.reason, view.passwordError)
        assertEquals(secondConfirmError.reason, view.confirmPasswordError)

        assertFalse(presenter.isCurrentInputValid())

        view.password = "aaaaaa"
        presenter.validatePassword()
        assertEquals(expectedEmailError.reason, view.emailError)
        assertNull(view.passwordError)
        assertEquals(secondConfirmError.reason, view.confirmPasswordError)

        assertFalse(presenter.isCurrentInputValid())
        // Third error will be triggered by checking valid input for all fields.
        val thirdConfirmError = ValidationResult.Invalid.InputMismatch("password", "confirm password")
        assertEquals(thirdConfirmError.reason, view.confirmPasswordError)

        view.email = "not@real.biz"

        presenter.validateEmail()
        assertNull(view.emailError)
        assertNull(view.passwordError)
        assertEquals(thirdConfirmError.reason, view.confirmPasswordError)
        assertFalse(presenter.isCurrentInputValid())

        view.confirmPassword = view.password
        presenter.validateConfirmPassword()
        assertNull(view.emailError)
        assertNull(view.passwordError)
        assertNull(view.confirmPasswordError)

        // And let's make sure checking everything, it's all still null.
        assertTrue(presenter.isCurrentInputValid())
        assertNull(view.emailError)
        assertNull(view.passwordError)
        assertNull(view.confirmPasswordError)
    }

    @Test
    fun checkingValidityWithoutChangesTriggersErrors() = runBlocking {
        val view = TestCreateAccountView()
        val presenter = CreateAccountPresenter(view)
        val expectedEmailError = ValidationResult.Invalid.WasNull("email")
        val expectedPasswordError = ValidationResult.Invalid.WasNull("password")
        val expectedConfirmPasswordError = ValidationResult.Invalid.WasNull("confirm password")

        val result = presenter.createAccountAsync()
        assertFalse(result)

        assertFalse(presenter.isCurrentInputValid())
        // Actual attempt to login should never have been made since
        // the input was invalid
        assertFalse(view.loadingSpinnerWasStarted)
        assertFalse(view.loadingSpinnerWasStopped)
        assertFalse(view.loadingSpinnerGoing)
        assertNull(view.apiError)
        assertFalse(view.accountCreationSucceeded)

        assertEquals(expectedEmailError.reason, view.emailError)
        assertEquals(expectedPasswordError.reason, view.passwordError)
        assertEquals(expectedConfirmPasswordError.reason, view.confirmPasswordError)
    }

    @Test
    fun attemptingToCreateAccountWithoutChangesTriggersErrorsAndFails() = runBlocking {
        val view = TestCreateAccountView()
        val presenter = CreateAccountPresenter(view)

        val expectedEmailError = ValidationResult.Invalid.WasNull("email")
        val expectedPasswordError = ValidationResult.Invalid.WasNull("password")
        val expectedConfirmError = ValidationResult.Invalid.WasNull("confirm password")

        val result = presenter.createAccountAsync()
        assertFalse(result)

        assertFalse(presenter.isCurrentInputValid())
        // Actual attempt to create an account should never have
        // been made since the input was invalid
        assertFalse(view.loadingSpinnerWasStarted)
        assertFalse(view.loadingSpinnerWasStopped)
        assertFalse(view.loadingSpinnerGoing)
        assertNull(view.apiError)
        assertFalse(view.accountCreationSucceeded)

        assertEquals(expectedEmailError.reason, view.emailError)
        assertEquals(expectedPasswordError.reason, view.passwordError)
        assertEquals(expectedConfirmError.reason, view.confirmPasswordError)
    }

    @Test
    fun attemptingToCreateAccountWithValidCredsSucceeds() = runBlocking {
        val view = TestCreateAccountView()
        val presenter = CreateAccountPresenter(view)
        Api.client = MockNetworkClient()

        view.email = MockNetworkClient.validUsername
        view.password = "password"
        view.confirmPassword = view.password

        val result = presenter.createAccountAsync()
        assertTrue(result)

        assertTrue(presenter.isCurrentInputValid())

        assertNull(view.emailError)
        assertNull(view.passwordError)
        assertNull(view.confirmPasswordError)
        assertNull(view.apiError)
        assertTrue(view.loadingSpinnerWasStarted)
        assertTrue(view.loadingSpinnerWasStopped)
        assertFalse(view.loadingSpinnerGoing)
        assertTrue(view.accountCreationSucceeded)

        assertNotNull(TokenManager.currentToken())
        assertEquals(MockNetworkClient.mockToken, TokenManager.currentToken()?.token)
    }

    @Test
    fun attemptingToCreateAccountWithExistingCredsFails() = runBlocking {
        val view = TestCreateAccountView()
        val presenter = CreateAccountPresenter(view)
        Api.client = MockNetworkClient()

        view.email = MockNetworkClient.takenUsername
        view.password = "password"
        view.confirmPassword = view.password

        val result = presenter.createAccountAsync()
        assertFalse(result)

        assertTrue(presenter.isCurrentInputValid())
        assertNull(view.emailError)
        assertNull(view.passwordError)
        assertNull(view.confirmPasswordError)

        // The request should have been kicked off, but should have failed.
        assertTrue(view.loadingSpinnerWasStarted)
        assertTrue(view.loadingSpinnerWasStopped)
        assertFalse(view.loadingSpinnerGoing)
        assertEquals("Account already exists", view.apiError)
        assertFalse(view.accountCreationSucceeded)

        assertNull(TokenManager.currentToken())
    }
}