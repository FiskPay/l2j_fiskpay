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

package org.l2jmobius.gameserver.blockchain;

/**
 * @author Scrab
 */
public class Configuration
{
    private static boolean _set = false;
    private static int _rewardId;
    private static String _wallet;
    private static String _symbol;
    private static byte[] _qrCodeData;
    
    protected static boolean setConfiguration(int rewardId, String wallet, String symbol, byte[] qrCodeData)
    {
        if (!isSet())
        {
            _rewardId = rewardId;
            _wallet = wallet;
            _symbol = symbol;
            
            try
            {
                _qrCodeData = qrCodeData;
                _set = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        
        return _set;
    }
    
    public static boolean isSet()
    {
        return _set;
    }
    
    public static int getRewardId()
    {
        return _rewardId;
    }
    
    public static String getWallet()
    {
        return _wallet;
    }
    
    public static String getSymbol()
    {
        return _symbol;
    }
    
    public static byte[] getQRCodeData()
    {
        return _qrCodeData;
    }
    
    public static String getLink()
    {
        return "https://l2.fiskpay.com/" + _wallet + "/";
    }
}