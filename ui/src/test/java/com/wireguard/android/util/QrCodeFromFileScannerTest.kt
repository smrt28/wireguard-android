/*
 * Copyright © 2026 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class QrCodeFromFileScannerTest {
    @Test
    fun fileScannerReadsNormalAndInvertedQrCodes() {
        for ((foreground, background) in colors) {
            val source = qrCode(foreground, background)
            val result = try {
                QrCodeFromFileScanner.decodeLuminanceSource(source, QRCodeReader())
            } catch (e: Exception) {
                throw AssertionError("Cannot decode foreground ${foreground.toString(16)} on background ${background.toString(16)}", e)
            }
            assertEquals(content, result.text)
        }
    }

    @Test
    fun cameraMixedModeReadsNormalAndInvertedQrCodes() {
        val decoder = DefaultDecoderFactory(null, null, null, 2).createDecoder(emptyMap<DecodeHintType, Any>())
        // Reuse the decoder as the camera does, alternating polarity across successive frames.
        for ((foreground, background) in colors) {
            val source = qrCode(foreground, background)
            val result = decoder.decode(source) ?: decoder.decode(source)
            assertNotNull("Cannot decode foreground ${foreground.toString(16)} on background ${background.toString(16)}", result)
            assertEquals(content, result!!.text)
        }
    }

    @Test(expected = NotFoundException::class)
    fun fileScannerRejectsImageWithoutQrCode() {
        val source = RGBLuminanceSource(size, size, IntArray(size * size) { 0xffffff })
        QrCodeFromFileScanner.decodeLuminanceSource(source, QRCodeReader())
    }

    @Test(expected = NotFoundException::class)
    fun normalDecoderCannotReadBlueOnBlackQrCode() {
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(qrCode(0x006fff, 0x000000))))
    }

    private fun qrCode(foreground: Int, background: Int): RGBLuminanceSource {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size) { index ->
            if (matrix[index % size, index / size]) foreground else background
        }
        return RGBLuminanceSource(size, size, pixels)
    }

    companion object {
        private const val size = 384
        private const val content = "[Interface]\nAddress = 10.0.0.2/32\n[Peer]\nEndpoint = example.com:51820\n"
        private val colors = listOf(
            0x000000 to 0xffffff,
            0xffffff to 0x000000,
            0x006fff to 0x000000,
        )
    }
}
