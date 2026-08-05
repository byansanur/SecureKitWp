package com.byan.securekitwp.data.repository

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityRepositoryTest {

    @Test
    fun `repository initialization does not crash`() = runTest {
        // Arrange
        val mockContext = mock(Context::class.java)
        
        // Act
        val repository = SecurityRepository(mockContext)
        
        // Assert
        assertNotNull(repository)
    }
}
