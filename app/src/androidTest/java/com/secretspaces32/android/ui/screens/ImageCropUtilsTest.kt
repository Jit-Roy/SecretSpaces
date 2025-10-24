package com.secretspaces32.android.ui.screens

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageCropUtilsTest {

    private fun makeBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(color)
        return bmp
    }

    @Test
    fun cropBitmapToLandscapeBitmap_returns_1920x1080() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val src = makeBitmap(3000, 2000, android.graphics.Color.GREEN)
        val crop = CropState(
            imageScale = 1f,
            frameScale = 0.6f,
            offsetX = 0f,
            offsetY = 0f,
            canvasWidthPx = 1000f,
            canvasHeightPx = 1600f
        )

        val result = cropBitmapToLandscapeBitmap(
            bitmap = src,
            crop = crop,
            rotationAngle = 0f,
            isFlippedHorizontally = false,
            targetWidth = 1920,
            targetHeight = 1080
        )

        assertNotNull("Cropped bitmap should not be null", result)
        assertEquals(1920, result!!.width)
        assertEquals(1080, result.height)

        // Cleanup
        result.recycle()
        src.recycle()
    }

    @Test
    fun cropBitmapToLandscapeBitmap_respects_rotation_and_flip() {
        val src = makeBitmap(2500, 2500, android.graphics.Color.RED)
        val crop = CropState(
            imageScale = 1.2f,
            frameScale = 0.5f,
            offsetX = 0f,
            offsetY = 0f,
            canvasWidthPx = 1200f,
            canvasHeightPx = 2000f
        )

        val result = cropBitmapToLandscapeBitmap(
            bitmap = src,
            crop = crop,
            rotationAngle = 90f,
            isFlippedHorizontally = true,
            targetWidth = 1920,
            targetHeight = 1080
        )

        assertNotNull(result)
        assertEquals(1920, result!!.width)
        assertEquals(1080, result.height)

        result.recycle()
        src.recycle()
    }
}
