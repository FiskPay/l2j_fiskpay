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
package org.l2jmobius.gameserver;

import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.util.DDSConverter;

import com.fiskpay.l2.Tools;

/**
 * @author Scrab
 */
public class BlockchainEndpoint
{
    private static final Logger LOGGER = Logger.getLogger(BlockchainEndpoint.class.getName());
    
    private static byte[] _qrCodeDataUpper; // Upper half of the generated QR code image (DDS-encoded) for the server panel wallet
    private static byte[] _qrCodeDataLower; // Lower half of the generated QR code image (DDS-encoded) for the server panel wallet
    
    private static String _wallet; // Blockchain wallet address owned by the game server (destination for deposits)
    private static String _symbol; // Token symbol used for blockchain transactions (e.g. USDT0)
    private static int _rewardId; // In-game item ID used to represent blockchain rewards (e.g. 4037)
    private static String _rewardName; // In-game display name of the blockchain reward item (e.g. Coin of Luck)
    
    private static boolean _isSet = false;
    
    /**
    * Indicates whether the configuration has been initialized and is valid.
    * @return true if configuration is set, false otherwise
    */
    public static boolean isSet()
    {
        return _isSet;
    }
    
    /**
    * Returns the upper part of the QR code binary data.<br>
    * Used for splitting large QR codes into multiple packets.
    * @return byte array containing upper QR code data
    */
    public static byte[] getQRCodeDataUpper()
    {
        return _qrCodeDataUpper;
    }
    
    /**
    * Returns the lower part of the QR code binary data.<br>
    * Used for splitting large QR codes into multiple packets.
    * @return byte array containing lower QR code data
    */
    public static byte[] getQRCodeDataLower()
    {
        return _qrCodeDataLower;
    }
    
    /**
    * Returns the blockchain wallet address associated with the game server.
    * @return wallet address
    */
    public static String getWalletAddress()
    {
        return _wallet;
    }
    
    /**
    * Returns the token symbol used for deposits and rewards.
    * @return token symbol
    */
    public static String getTokenSymbol()
    {
        return _symbol;
    }
    
    /**
    * Returns the in-game item ID used as a reward, for blockchain transactions.
    * @return reward item ID
    */
    public static int getRewardItemId()
    {
        return _rewardId;
    }
    
    /**
    * Returns the in-game name of the reward item, for blockchain transactions.
    * @return reward item ID
    */
    public static String getRewardItemName()
    {
        return _rewardName;
    }
        
    /**
    * Builds and returns the web panel URL for the configured wallet.
    * @return full panel link URL
    */
    public static String getPanelLink()
    {
        return "https://l2.fiskpay.com/" + _wallet + "/";
    }
    
    /**
    * Handles and routes incoming JSON requests to the appropriate game server logic.<br>
    * <br>
    * The input string must be a valid JSON object containing:<br>
    * - "subject": a string that defines the requested operation<br>
    * - "info": a JSON array with the parameters required by that operation<br>
    * <br>
    * Based on the value of "subject", this method dispatches the request to the corresponding handler method and returns its response as a JSON-formatted string.<br>
    * <br>
    * If the subject is not recognized, an error response is returned.<br>
    * @param requestString JSON-formatted request payload
    * @return JSON-formatted response string
    */
    public static String processRequest(String requestString)
    {
        JSONObject requestObject = new JSONObject(requestString);
        JSONObject responseObject = new JSONObject();
        
        String subject = requestObject.getString("subject");
        JSONArray info = requestObject.getJSONArray("info");
        
        switch (subject)
        {
            case "addToCharacter":
            {
                final String character = info.getString(0);
                final String amount = info.getString(1);
                final String deposit = info.getString(2);
                
                responseObject = addToCharacter(character, amount, deposit);
                break;
            }
            case "getAccountCharacters":
            {
                final String username = info.getString(0);
                
                responseObject = getAccountCharacters(username);
                break;
            }
            case "getCharacterBalance":
            {
                final String character = info.getString(0);
                
                responseObject = getCharacterBalance(character);
                break;
            }
            case "getCharacterUsername":
            {
                final String character = info.getString(0);
                
                responseObject = getCharacterUsername(character);
                break;
            }
            case "getGameServerBalance":
            {
                responseObject = getGameServerBalance();
                break;
            }
            case "getGameServerMode":
            {
                responseObject = getGameServerMode();
                break;
            }
            case "isCharacterOffline":
            {
                final String character = info.getString(0);
                
                responseObject = isCharacterOffline(character);
                break;
            }
            case "removeFromCharacter":
            {
                final String character = info.getString(0);
                final String amount = info.getString(1);
                
                responseObject = removeFromCharacter(character, amount);
                break;
            }
            case "setConfig":
            {
                final String wallet = info.getString(0);
                final String symbol = info.getString(1);
                final String rwdId = info.getString(2);
                
                responseObject = setConfig(wallet, symbol, rwdId);
                break;
            }
            default:
            {
                responseObject = new JSONObject().put("ok", false).put("error", "Unknown request to Game Server. Subject: " + subject);
                break;
            }
        }
        
        return responseObject.toString();
    }
    
    private static JSONObject getAccountCharacters(String username)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            final JSONArray characters = new JSONArray();
            
            try (PreparedStatement ps = con.prepareStatement("SELECT char_name FROM characters WHERE account_name = ?;"))
            {
                ps.setString(1, username);
                
                try (ResultSet rs = ps.executeQuery())
                {
                    while (rs.next())
                    {
                        characters.put(rs.getString("char_name"));
                    }
                }
                
                return new JSONObject().put("ok", true).put("data", characters);
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("ok", false).put("error", "getCharacters SQL error");
        }
    }
    
    private static JSONObject getCharacterBalance(String character)
    {
        if (!isSet())
        {
            return new JSONObject().put("ok", false).put("error", "Blockchain configuration is not set");
        }
        
        final int playerId = CharInfoTable.getInstance().getIdByName(character);
        
        if (playerId == -1)
        {
            return new JSONObject().put("ok", false).put("error", "Character not found");
        }
        
        final Player player = World.getInstance().getPlayer(playerId);
        
        if (player != null)
        {
            final Item inventoryItem = player.getInventory().getItemByItemId(getRewardItemId());
            
            if (inventoryItem != null)
            {
                return new JSONObject().put("ok", true).put("data", Integer.toString(inventoryItem.getCount()));
            }
            
            return new JSONObject().put("ok", true).put("data", "0");
        }
        
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT SUM(i.count) AS balance FROM items AS i, characters AS c WHERE c.charId = i.owner_id AND c.char_name = ? AND i.item_id = ? AND i.loc = 'INVENTORY';"))
            {
                ps.setString(1, character);
                ps.setInt(2, getRewardItemId());
                
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next() && rs.getString("balance") != null)
                    {
                        return new JSONObject().put("ok", true).put("data", rs.getString("balance"));
                    }
                    
                    return new JSONObject().put("ok", true).put("data", "0");
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("ok", false).put("error", "getCharacterBalance SQL error");
        }
    }
    
    private static JSONObject getCharacterUsername(String character)
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT account_name FROM characters WHERE char_name = ? LIMIT 1;"))
            {
                ps.setString(1, character);
                
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        return new JSONObject().put("ok", true).put("data", rs.getString("account_name"));
                    }
                    
                    return new JSONObject().put("ok", false).put("error", "Character " + character + " account username not found");
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            
            return new JSONObject().put("ok", false).put("error", "getCharacterUsername SQL error");
        }
    }
    
    private static JSONObject isCharacterOffline(String character)
    {
        final int playerId = CharInfoTable.getInstance().getIdByName(character);
        
        if (playerId == -1)
        {
            return new JSONObject().put("ok", false).put("error", "Character not found");
        }
        
        final Player player = World.getInstance().getPlayer(playerId);
        
        if (player == null)
        {
            return new JSONObject().put("ok", true).put("data", false);
        }
        
        return new JSONObject().put("ok", true).put("data", true);
    }
    
    private static JSONObject addToCharacter(String character, String amount, String isDeposit)
    {
        if (!isSet())
        {
            return new JSONObject().put("ok", false).put("error", "Blockchain configuration is not set");
        }
        
        final int playerId = CharInfoTable.getInstance().getIdByName(character);
        
        if (playerId == -1)
        {
            return new JSONObject().put("ok", false).put("error", "Character not found");
        }
        
        final Player player = World.getInstance().getPlayer(playerId);
        final int rewardAmount = Integer.parseInt(amount);
        
        if (player != null)
        {
            final PlayerInventory inventory = player.getInventory();
            final Item item = inventory.addItem(ItemProcessType.REWARD, getRewardItemId(), rewardAmount, player, null);
            
            inventory.updateDatabase();
            
            final InventoryUpdate iu = new InventoryUpdate();
            
            if (item.getCount() == rewardAmount)
            {
                iu.addNewItem(item);
            }
            else
            {
                iu.addModifiedItem(item);
            }
            
            player.sendInventoryUpdate(iu);
            
            if (isDeposit.equals("1"))
            {
                player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", "-------------- New Blockchain Deposit --------------"));
                player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", rewardAmount + " " + _rewardName + " have been deposited."));
            }
            else
            {
                player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", "--------------- New Blockchain Refund --------------"));
                player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", rewardAmount + " " + _rewardName + " have been refunded."));
            }
            
            return new JSONObject().put("ok", true);
        }
        
        Connection con = null;
        
        try
        {
            con = DatabaseFactory.getConnection();
            con.setAutoCommit(false);
            
            try (PreparedStatement ps = con.prepareStatement("UPDATE items SET count = count + ? WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY';"))
            {
                ps.setInt(1, rewardAmount);
                ps.setInt(2, playerId);
                ps.setInt(3, getRewardItemId());
                
                if (ps.executeUpdate() == 0)
                {
                    try (PreparedStatement ps1 = con.prepareStatement("INSERT INTO items (owner_id, object_id, item_id, count, loc) VALUES (?, ?, ?, ?, 'INVENTORY');"))
                    {
                        final IdManager im = IdManager.getInstance();
                        final int nextObjectId = im.getNextId();
                        
                        ps1.setInt(1, playerId);
                        ps1.setInt(2, nextObjectId);
                        ps1.setInt(3, getRewardItemId());
                        ps1.setInt(4, rewardAmount);
                        
                        if (ps1.executeUpdate() == 0)
                        {
                            im.releaseId(nextObjectId);
                            con.rollback();
                            
                            return new JSONObject().put("ok", false).put("error", "Offline deposit was not successful");
                        }
                    }
                }
                
                con.commit();
                
                return new JSONObject().put("ok", true);
            }
        }
        catch (Exception e)
        {
            if (con != null)
            {
                try
                {
                    con.rollback();
                }
                catch (SQLException ex)
                {
                    LOGGER.log(Level.WARNING, "Rollback failed: " + ex.getMessage(), ex);
                }
            }
            
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            
            return new JSONObject().put("ok", false).put("error", "addToCharacter SQL error");
        }
        finally
        {
            if (con != null)
            {
                try
                {
                    con.close();
                }
                catch (SQLException ex)
                {
                    LOGGER.log(Level.WARNING, "Failed to close connection: " + ex.getMessage(), ex);
                }
            }
        }
    }
    
    private static JSONObject removeFromCharacter(String character, String amount)
    {
        if (!isSet())
        {
            return new JSONObject().put("ok", false).put("error", "Blockchain configuration is not set");
        }
        
        final int playerId = CharInfoTable.getInstance().getIdByName(character);
        
        if (playerId == -1)
        {
            return new JSONObject().put("ok", false).put("error", "Character not found");
        }
        
        final Player player = World.getInstance().getPlayer(playerId);
        final int removeAmount = Integer.parseInt(amount);
        
        if (player != null)
        {
            final PlayerInventory inventory = player.getInventory();
            final Item item = inventory.getItemByItemId(getRewardItemId());
            
            if (item == null)
            {
                return new JSONObject().put("ok", false).put("error", "There are no " + _rewardName + " found in your character's inventory");
            }
            
            final int inventoryAmount = item.getCount();
            
            if (inventoryAmount < removeAmount)
            {
                return new JSONObject().put("ok", false).put("error", "Insufficient " + _rewardName + " balance (Having: " + inventoryAmount + ", Need: " + removeAmount + ")");
            }
            
            inventory.destroyItem(ItemProcessType.DESTROY, item, removeAmount, player, null);
            inventory.updateDatabase();
            
            final InventoryUpdate iu = new InventoryUpdate();
            
            if (inventoryAmount == removeAmount)
            {
                iu.addRemovedItem(item);
            }
            else
            {
                iu.addModifiedItem(item);
            }
            
            player.sendInventoryUpdate(iu);
            
            player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", "------------- New Blockchain Withdrawal ------------"));
            player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Info", removeAmount + " " + _rewardName + " have been withdrawn."));
            
            return new JSONObject().put("ok", true);
        }
        
        Connection con = null;
        
        try
        {
            con = DatabaseFactory.getConnection();
            con.setAutoCommit(false);
            
            try (PreparedStatement ps = con.prepareStatement("SELECT count FROM items WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY' LIMIT 1;"))
            {
                ps.setInt(1, playerId);
                ps.setInt(2, getRewardItemId());
                
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        final int inventoryAmount = rs.getInt("count");
                        
                        if (inventoryAmount < removeAmount)
                        {
                            con.rollback();
                            
                            return new JSONObject().put("ok", false).put("error", "Insufficient " + _rewardName + " balance (Having: " + String.valueOf(inventoryAmount) + ", Need: " + String.valueOf(removeAmount) + ")");
                        }
                        
                        if (inventoryAmount == removeAmount)
                        {
                            try (PreparedStatement ps1 = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY';"))
                            {
                                ps1.setInt(1, playerId);
                                ps1.setInt(2, getRewardItemId());
                                
                                if (ps1.executeUpdate() == 0)
                                {
                                    con.rollback();
                                    
                                    return new JSONObject().put("ok", false).put("error", "Offline withdrawal (delete) was not successful");
                                }
                            }
                        }
                        else
                        {
                            try (PreparedStatement ps1 = con.prepareStatement("UPDATE items SET count = count - ? WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY';"))
                            {
                                ps1.setInt(1, removeAmount);
                                ps1.setInt(2, playerId);
                                ps1.setInt(3, getRewardItemId());
                                
                                if (ps1.executeUpdate() == 0)
                                {
                                    con.rollback();
                                    
                                    return new JSONObject().put("ok", false).put("error", "Offline withdrawal (update) was not successful");
                                }
                            }
                        }
                        
                        con.commit();
                        
                        return new JSONObject().put("ok", true);
                    }
                    
                    con.rollback();
                    
                    return new JSONObject().put("ok", false).put("error", "There are no " + _rewardName + " found in your character's inventory");
                }
            }
        }
        catch (Exception e)
        {
            if (con != null)
            {
                try
                {
                    con.rollback();
                }
                catch (SQLException ex)
                {
                    LOGGER.log(Level.WARNING, "Rollback failed: " + ex.getMessage(), ex);
                }
            }
            
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            
            return new JSONObject().put("ok", false).put("error", "removeFromCharacter SQL error");
        }
        finally
        {
            if (con != null)
            {
                try
                {
                    con.close();
                }
                catch (SQLException ex)
                {
                    LOGGER.log(Level.WARNING, "Failed to close connection: " + ex.getMessage(), ex);
                }
            }
        }
    }
    
    private static JSONObject getGameServerMode()
    {
        final Shutdown sd = Shutdown.getInstance();
        
        if (sd == null)
        {
            return new JSONObject().put("ok", false).put("error", "No Shutdown instance");
        }
        
        return new JSONObject().put("ok", true).put("data", sd.getMode());
    }
    
    private static JSONObject getGameServerBalance()
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT SUM(count) AS balance FROM items WHERE item_id = ?;"))
            {
                ps.setInt(1, getRewardItemId());
                
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next() && rs.getString("balance") != null)
                    {
                        return new JSONObject().put("ok", true).put("data", rs.getString("balance"));
                    }
                    
                    return new JSONObject().put("ok", true).put("data", "0");
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            
            return new JSONObject().put("ok", false).put("error", "getGameServerBalance SQL error");
        }
    }
    
    private static JSONObject setConfig(String wallet, String symbol, String rwdId)
    {
        final int rewardId = Integer.parseInt(rwdId);
        final ItemTemplate template = ItemData.getInstance().getTemplate(rewardId);
        
        if (template == null)
        {
            return new JSONObject().put("ok", false).put("error", "Blockchain in-game reward (item ID: " + rwdId + ") does not exist");
        }
        
        if (!template.isStackable())
        {
            return new JSONObject().put("ok", false).put("error", "Blockchain in-game reward (item ID: " + rwdId + ") should be stackable");
        }
                
        try
        {
            final BufferedImage qrCodeImage = Tools.generateQRCodeImage(wallet);
            final int width = qrCodeImage.getWidth();
            final int height = qrCodeImage.getHeight();
            final int half = height / 2;
            final BufferedImage upperHalf = qrCodeImage.getSubimage(0, 0, width, half);
            final BufferedImage lowerHalf = qrCodeImage.getSubimage(0, half, width, height - half);
            
            _qrCodeDataUpper = DDSConverter.convertToDDS(upperHalf).array();
            _qrCodeDataLower = DDSConverter.convertToDDS(lowerHalf).array();
            
            _wallet = wallet;
            _symbol = symbol;
            _rewardId = rewardId;
            _rewardName = template.getName();
            
            _isSet = true;
            
            return new JSONObject().put("ok", true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return new JSONObject().put("ok", false).put("error", "Blockchain configuration could not be set");
    }
}