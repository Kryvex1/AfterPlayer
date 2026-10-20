package dev.anilbeesetti.nextplayer.feature.player.service

import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CarConnectionProviderTest {

    @Test
    fun onCreate_returnsTrue() {
        val provider = CarConnectionProvider()
        assertTrue(provider.onCreate())
    }

    @Test
    fun query_returnsNotConnectedState() {
        val provider = CarConnectionProvider()
        val uri = "content://androidx.car.app.connection".toUri()
        val projection = arrayOf("CarConnectionState")

        val cursor = provider.query(uri, projection, null, null, null)
        assertNotNull(cursor)
        cursor!!.use {
            assertTrue(it.moveToFirst())
            val index = it.getColumnIndexOrThrow("CarConnectionState")
            val state = it.getInt(index)
            assertEquals(0, state)
        }
    }

    @Test
    fun query_withNullProjection_returnsCarConnectionStateColumn() {
        val provider = CarConnectionProvider()
        val uri = "content://androidx.car.app.connection".toUri()

        val cursor = provider.query(uri, null, null, null, null)
        assertNotNull(cursor)
        cursor!!.use {
            assertTrue(it.moveToFirst())
            val index = it.getColumnIndex("CarConnectionState")
            assertTrue(index >= 0)
            assertEquals(0, it.getInt(index))
        }
    }
}
