package com.byan.securekitwp.ui.viewmodel

import android.content.Context
import com.byan.securekitwp.data.repository.NetworkSecurityState
import com.byan.securekitwp.data.repository.SecurityRepository
import com.byan.securekitwp.data.repository.SecurityStatusState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mock(Context::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkSecurity updates state correctly when environment is safe`() = runTest {
        // Arrange
        val mockRepository = object : SecurityRepository(mockContext) {
            override suspend fun checkSecurityEnvironment(): SecurityStatusState {
                return SecurityStatusState(isRooted = false, isHooked = false, isEmulator = false, isDebuggerAttached = false)
            }
            override suspend fun checkNetworkSecurity(): NetworkSecurityState {
                return NetworkSecurityState(isProxyEnabled = false, isVpnEnabled = false)
            }
        }
        val viewModel = SecurityViewModel(mockRepository)

        // Act
        viewModel.checkSecurity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertTrue(state.isAppSafe)
    }

    @Test
    fun `checkSecurity updates state correctly when environment is compromised`() = runTest {
        // Arrange
        val mockRepository = object : SecurityRepository(mockContext) {
            override suspend fun checkSecurityEnvironment(): SecurityStatusState {
                return SecurityStatusState(isRooted = true, isHooked = false, isEmulator = false, isDebuggerAttached = false)
            }
            override suspend fun checkNetworkSecurity(): NetworkSecurityState {
                return NetworkSecurityState(isProxyEnabled = false, isVpnEnabled = false)
            }
        }
        val viewModel = SecurityViewModel(mockRepository)

        // Act
        viewModel.checkSecurity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertFalse(state.isAppSafe)
        assertEquals(true, state.securityStatus?.isRooted)
    }
}
