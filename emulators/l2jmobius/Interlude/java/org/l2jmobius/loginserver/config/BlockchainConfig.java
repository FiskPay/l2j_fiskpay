/*
 * Copyright (c) 2026 FiskPay
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
package org.l2jmobius.loginserver.config;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * @author Scrab
 */
public class BlockchainConfig
{
    // File
    private static final String BLOCKCHAIN_CONFIG_FILE = "./config/Blockchain.ini";
    
    public static boolean ENABLE;
    public static String SYMBOL;
    public static String WALLET;
    public static String PASSWORD;
    
    public static void load()
    {
        final ConfigReader blockchainConfig = new ConfigReader(BLOCKCHAIN_CONFIG_FILE);
        
        ENABLE = blockchainConfig.getBoolean("Enable", false);
        SYMBOL = blockchainConfig.getString("Symbol", "USDT0");
        WALLET = blockchainConfig.getString("Wallet", "0x0000000000000000000000000000000000000000");
        PASSWORD = blockchainConfig.getString("Password", "");
    }
}