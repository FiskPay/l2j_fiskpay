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
package com.fiskpay.l2;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

/**
 * @author Scrab
 */
public class Signer
{
    private static final String WITHDRAW_SELECTOR = "0x6926be4e";
    
    private final long _chainId;
    private final String _tokenSymbol;
    private final Credentials _credentials;
    private final String _signerAddress;
    
    @SuppressWarnings("rawtypes")
    private static final List<TypeReference> WITHDRAW_PARAMETER_TYPES = Arrays.asList(new TypeReference<Address>()
    {
    }, new TypeReference<Address>()
    {
    }, new TypeReference<Utf8String>()
    {
    }, new TypeReference<Uint32>()
    {
    }, new TypeReference<Uint8>()
    {
    }, new TypeReference<Utf8String>()
    {
    }, new TypeReference<Uint32>()
    {
    });
    
    public Signer(String keystoreFilePath, String password, long chainId, String tokenSymbol) throws Exception
    {
        final File keystoreFile = new File(keystoreFilePath);
        
        if (!keystoreFile.isFile())
        {
            throw new IllegalArgumentException("Withdrawal keystore file not found: " + keystoreFilePath);
        }
        
        _chainId = chainId;
        _tokenSymbol = tokenSymbol;
        _credentials = WalletUtils.loadCredentials(password, keystoreFile);
        _signerAddress = _credentials.getAddress();
        
        if (!Pattern.matches("^0x[a-fA-F0-9]{40}$", _signerAddress))
        {
            throw new IllegalArgumentException("Withdrawal keystore signer address is invalid");
        }
    }
    
    public String getSignerAddress()
    {
        return _signerAddress;
    }
    
    public JSONObject validateWithdrawTransaction(JSONObject data, String srvId)
    {
        try
        {
            if (!data.has("tx") || !(data.get("tx") instanceof JSONObject))
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction object undefined");
            }
            
            final JSONObject tx = data.getJSONObject("tx");
            final String txData = tx.getString("data");
            
            if (!txData.startsWith(WITHDRAW_SELECTOR))
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction method must be Withdraw");
            }
            
            @SuppressWarnings("unchecked")
            final List<Type> decodedParameters = FunctionReturnDecoder.decode(txData.substring(WITHDRAW_SELECTOR.length()), (List<TypeReference<Type>>) (List<?>) WITHDRAW_PARAMETER_TYPES);
            
            if (decodedParameters.size() != 7)
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction data could not be decoded");
            }
            
            final String clientWalletAddress = data.getString("clientWalletAddress");
            final String playerWalletAddress = data.getString("playerWalletAddress");
            final BigInteger amount = new BigInteger(data.getString("amount"));
            final BigInteger server = new BigInteger(srvId);
            final String character = data.getString("character");
            final BigInteger refund = new BigInteger(data.getString("refund"));
            
            if (!((String) decodedParameters.get(0).getValue()).equalsIgnoreCase(clientWalletAddress))
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction from address mismatch");
            }
            
            if (!((String) decodedParameters.get(1).getValue()).equalsIgnoreCase(playerWalletAddress))
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction to address mismatch");
            }
            
            if (!_tokenSymbol.equals(decodedParameters.get(2).getValue()))
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction symbol mismatch");
            }
            
            if (!amount.equals(decodedParameters.get(3).getValue()))
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction amount mismatch");
            }
            
            if (!server.equals(decodedParameters.get(4).getValue()))
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction server mismatch");
            }
            
            if (!character.equals(decodedParameters.get(5).getValue()))
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction character mismatch");
            }
            
            if (!refund.equals(decodedParameters.get(6).getValue()))
            {
                return new JSONObject().put("ok", false).put("error", "Withdrawal transaction refund mismatch");
            }
            
            return new JSONObject().put("ok", true).put("data", tx);
        }
        catch (Exception e)
        {
            return new JSONObject().put("ok", false).put("error", e.getMessage());
        }
    }
    
    public JSONObject signWithdrawTransaction(JSONObject tx)
    {
        try
        {
            final BigInteger nonce = new BigInteger(tx.getString("nonce"));
            final BigInteger gasLimit = new BigInteger(tx.getString("gasLimit"));
            final String to = tx.getString("to");
            final BigInteger value = new BigInteger(tx.getString("value"));
            final String txData = tx.getString("data");
            final BigInteger maxPriorityFeePerGas = new BigInteger(tx.getString("maxPriorityFeePerGas"));
            final BigInteger maxFeePerGas = new BigInteger(tx.getString("maxFeePerGas"));
            final RawTransaction rawTransaction = RawTransaction.createTransaction(_chainId, nonce, gasLimit, to, value, txData, maxPriorityFeePerGas, maxFeePerGas);
            final String signedTransaction = Numeric.toHexString(TransactionEncoder.signMessage(rawTransaction, _credentials));
            
            return new JSONObject().put("ok", true).put("data", new JSONObject().put("rawTransaction", signedTransaction).put("chainId", Long.toString(_chainId)).put("nonce", nonce.toString()).put("to", to).put("value", value.toString()).put("data", txData));
        }
        catch (Exception e)
        {
            return new JSONObject().put("ok", false).put("error", "Withdrawal transaction signing failed");
        }
    }
}
