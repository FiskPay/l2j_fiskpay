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

import com.fiskpay.l2.Tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.instancemanager.IdManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.util.DDSConverter;

public class GSMethods
{
    private static final Logger LOGGER = Logger.getLogger(GSMethods.class.getName());
    
    protected static JSONObject getAccountCharacters(String username)
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
    
    protected static JSONObject getCharacterBalance(String character)
    {
        final int playerId = CharInfoTable.getInstance().getIdByName(character);
        
        if (playerId == -1)
        {
            return new JSONObject().put("ok", false).put("error", "Character not found");
        }
        
        final Player player = World.getInstance().getPlayer(playerId);
        
        if (player != null)
        {
            final Item inventoryItem = player.getInventory().getItemByItemId(Configuration.getRewardId());
            
            if (inventoryItem != null)
            {
                return new JSONObject().put("ok", true).put("data", Long.toString(inventoryItem.getCount()));
            }
            
            return new JSONObject().put("ok", true).put("data", "0");
        }
        
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT SUM(i.count) AS balance FROM items AS i, characters AS c WHERE c.charId = i.owner_id AND c.char_name = ? AND i.item_id = ? AND i.loc = 'INVENTORY';"))
            {
                ps.setString(1, character);
                ps.setInt(2, Configuration.getRewardId());
                
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
    
    protected static JSONObject isCharacterOffline(String character)
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
    
    protected static JSONObject getCharacterUsername(String character)
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
    
    protected static JSONObject addToCharacter(String character, String amount)
    {
        if (!Configuration.isSet())
        {
            return new JSONObject().put("ok", false).put("error", "Blockchain in-game reward item is not set");
        }
        
        int playerId = CharInfoTable.getInstance().getIdByName(character);
        
        if (playerId == -1)
        {
            return new JSONObject().put("ok", false).put("error", "Character not found");
        }
        
        final long itemAmount = Long.parseLong(amount);
        final String itemName = ItemData.getInstance().getTemplate(Configuration.getRewardId()).getName();
        final Player player = World.getInstance().getPlayer(playerId);
        
        if (player != null)
        {
            final PlayerInventory inventory = player.getInventory();
            final Item inventoryItem = inventory.getItemByItemId(Configuration.getRewardId());
            
            final InventoryUpdate iu = new InventoryUpdate();
            
            if (inventoryItem != null)
            {
                final long inventoryAmount = inventoryItem.getCount();
                inventoryItem.setCount(inventoryAmount + itemAmount);
                iu.addModifiedItem(inventoryItem);
            }
            else
            {
                final Item newItem = player.getInventory().addItem("Deposit", Configuration.getRewardId(), itemAmount, player, null);
                iu.addNewItem(newItem);
            }
            
            player.sendInventoryUpdate(iu);
            player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Blockchain", "----------- New " + Configuration.getSymbol() + " transaction -----------"));
            player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Blockchain", String.valueOf(itemAmount) + " " + itemName + " has been deposited"));
            
            inventory.updateDatabase();
            
            return new JSONObject().put("ok", true);
        }
        
        Connection con = null;
        
        try
        {
            con = DatabaseFactory.getConnection();
            con.setAutoCommit(false);
            
            try (PreparedStatement ps = con.prepareStatement("UPDATE items SET count = count + ? WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY';"))
            {
                ps.setLong(1, itemAmount);
                ps.setInt(2, playerId);
                ps.setInt(3, Configuration.getRewardId());
                
                if (ps.executeUpdate() == 0)
                {
                    try (PreparedStatement ps1 = con.prepareStatement("INSERT INTO items (owner_id, object_id, item_id, count, loc) VALUES (?, ?, ?, ?, 'INVENTORY');"))
                    {
                        final IdManager im = IdManager.getInstance();
                        final int nextId = im.getNextId();
                        
                        ps1.setInt(1, playerId);
                        ps1.setInt(2, nextId);
                        ps1.setInt(3, Configuration.getRewardId());
                        ps1.setLong(4, itemAmount);
                        
                        if (ps1.executeUpdate() == 0)
                        {
                            im.releaseId(nextId);
                            
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
    
    protected static JSONObject removeFromCharacter(String character, String amount)
    {
        if (!Configuration.isSet())
        {
            return new JSONObject().put("ok", false).put("error", "Blockchain in-game reward item is not set");
        }
        
        int playerId = CharInfoTable.getInstance().getIdByName(character);
        
        if (playerId == -1)
        {
            return new JSONObject().put("ok", false).put("error", "Character not found");
        }
        
        final long itemAmount = Long.parseLong(amount);
        final String itemName = ItemData.getInstance().getTemplate(Configuration.getRewardId()).getName();
        final Player player = World.getInstance().getPlayer(playerId);
        
        if (player != null)
        {
            final PlayerInventory inventory = player.getInventory();
            final Item inventoryItem = inventory.getItemByItemId(Configuration.getRewardId());
            
            final InventoryUpdate iu = new InventoryUpdate();
            
            if (inventoryItem == null)
            {
                return new JSONObject().put("ok", false).put("error", "Item not found in inventory");
            }
            
            final long inventoryAmount = inventoryItem.getCount();
            
            if (inventoryAmount < itemAmount)
            {
                return new JSONObject().put("ok", false).put("error", "Not enough items in inventory");
            }
            
            if (inventoryAmount == itemAmount)
            {
                player.getInventory().destroyItem("Withdraw", inventoryItem, inventoryAmount, player, null);
                iu.addRemovedItem(inventoryItem);
            }
            else
            {
                inventoryItem.setCount(inventoryAmount - itemAmount);
                iu.addModifiedItem(inventoryItem);
            }
            
            player.sendInventoryUpdate(iu);
            player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Blockchain", "----------- New " + Configuration.getSymbol() + " transaction -----------"));
            player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "Blockchain", String.valueOf(itemAmount) + " " + itemName + " has been withdrawn"));
            
            inventory.updateDatabase();
            
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
                ps.setInt(2, Configuration.getRewardId());
                
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        final long inventoryAmount = rs.getLong("count");
                        
                        if (inventoryAmount < itemAmount)
                        {
                            con.rollback();

                            return new JSONObject().put("ok", false).put("error", "Not enough items in inventory");
                        }
                        
                        if (inventoryAmount == itemAmount)
                        {
                            try (PreparedStatement ps1 = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY';"))
                            {
                                ps1.setInt(1, playerId);
                                ps1.setInt(2, Configuration.getRewardId());
                                
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
                                ps1.setLong(1, itemAmount);
                                ps1.setInt(2, playerId);
                                ps1.setInt(3, Configuration.getRewardId());
                                
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

                    return new JSONObject().put("ok", false).put("error", "Item not found in inventory");
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
    
    protected static JSONObject getGameServerMode()
    {
        final Shutdown sd = Shutdown.getInstance();
        
        if (sd == null)
        {
            return new JSONObject().put("ok", false).put("error", "No Shutdown instance");
        }
        
        return new JSONObject().put("ok", true).put("data", sd.getMode());
    }
    
    protected static JSONObject setConfig(String rwdId, String wallet, String symbol)
    {
        final int rewardId = Integer.parseInt(rwdId);
        
        byte[] qrCodeData = null;
        
        if (ItemData.getInstance().getTemplate(rewardId) == null)
        {
            return new JSONObject().put("ok", false).put("error", "Blockchain in-game reward item (" + rwdId + ") does not exist");
        }
        
        try
        {
            qrCodeData = DDSConverter.convertToDDS(Tools.generateQRCodeImage(wallet)).array();
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "QR Code error: " + e.getMessage(), e);
            LOGGER.log(Level.WARNING, "QR Code data could not be produced");
        }
        
        if (Configuration.setConfiguration(rewardId, wallet, symbol, qrCodeData))
        {
            return new JSONObject().put("ok", true);
        }
        
        return new JSONObject().put("ok", false).put("error", "Blockchain configuration could not be set (maybe already set?)");
    }
    
    protected static JSONObject getGameServerBalance()
    {
        try (Connection con = DatabaseFactory.getConnection())
        {
            try (PreparedStatement ps = con.prepareStatement("SELECT SUM(count) AS balance FROM items WHERE item_id = ?;"))
            {
                ps.setInt(1, Configuration.getRewardId());
                
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
}