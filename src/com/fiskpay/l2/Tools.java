/*
* Copyright (c) 2025 FiskPay
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
* IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.fiskpay.l2;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * @author Scrab
 */
public class Tools
{
    
    /**
     * Generates a QR code image with a link to your blockchain panel and overlays a logo image in its center.
     * @param wallet the ethereum wallet the QR code will point to
     * @return The QR code as a BufferedImage.
     * @throws Exception If QR code generation or image processing fails.
     */
    public static BufferedImage generateQRCodeImage(String wallet) throws Exception
    {
        int width = 256;
        int height = 256;
        
        // --- 1. Get the Logo as BufferedImage ---
        BufferedImage logoImage = ImageIO.read(Tools.class.getResourceAsStream("/images/FiskPayLogo.png"));
        
        // --- 2. Generate the QR Code matrix from text ---
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        
        BitMatrix bitMatrix = qrCodeWriter.encode("https://l2.fiskpay.com/" + wallet + "/", BarcodeFormat.QR_CODE, width, height, hints);
        
        // --- 3. Paint the QR Code on a BufferedImage ---
        BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        
        // --- 4. Overlay the logo on the QR Code ---
        int qrWidth = qrImage.getWidth();
        int qrHeight = qrImage.getHeight();
        int logoWidth = qrWidth / 4;
        int logoHeight = qrHeight / 4;

        Image scaledLogo = logoImage.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);
        BufferedImage combinedImage = new BufferedImage(qrWidth, qrHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = combinedImage.createGraphics();

        g.drawImage(qrImage, 0, 0, null);

        int centerX = (qrWidth - logoWidth) / 2;
        int centerY = (qrHeight - logoHeight) / 2;

        g.drawImage(scaledLogo, centerX, centerY, null);
        g.dispose();
        
        return combinedImage;
    }
}