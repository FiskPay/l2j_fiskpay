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
import org.l2jmobius.gameserver.instancemanager.IdManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class GSMethods {
    private static final Logger LOGGER = Logger.getLogger(GSMethods.class.getName());
    private static final int REWARD_ID = 4037; // Coin of Luck

    protected static JSONObject getAccountCharacters(String username) {
        try (Connection con = DatabaseFactory.getConnection()) {
            JSONArray characters = new JSONArray();

            try (PreparedStatement ps = con
                    .prepareStatement("SELECT char_name FROM characters WHERE account_name = ?;")) {
                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        characters.put(rs.getString("char_name"));
                    }
                }

                return new JSONObject().put("data", characters);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "getCharacters SQL error");
        }
    }

    protected static JSONObject getCharacterBalance(String character) {
        int playerId = CharInfoTable.getInstance().getIdByName(character);

        if (playerId == -1) {
            return new JSONObject().put("fail", "Character not found");
        }

        Player player = World.getInstance().getPlayer(playerId);

        if (player != null) {
            Item inventoryItem = player.getInventory().getItemByItemId(REWARD_ID);

            if (inventoryItem != null) {
                return new JSONObject().put("data", Long.toString(inventoryItem.getCount()));
            }

            return new JSONObject().put("data", "0");
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT SUM(i.count) AS balance FROM items AS i, characters AS c WHERE c.charId = i.owner_id AND c.char_name = ? AND i.item_id = ? AND i.loc = 'INVENTORY';")) {
                ps.setString(1, character);
                ps.setInt(2, REWARD_ID);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getString("balance") != null) {
                        return new JSONObject().put("data", rs.getString("balance"));
                    }

                    return new JSONObject().put("data", "0");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "getCharacterBalance SQL error");
        }
    }

    protected static JSONObject isCharacterOffline(String character) {
        int playerId = CharInfoTable.getInstance().getIdByName(character);

        if (playerId == -1) {
            return new JSONObject().put("fail", "Character not found");
        }

        Player player = World.getInstance().getPlayer(playerId);

        if (player == null) {
            return new JSONObject().put("data", 0);
        }

        return new JSONObject().put("data", 1);
    }

    protected static JSONObject getCharacterUsername(String character) {
        try (Connection con = DatabaseFactory.getConnection()) {
            try (PreparedStatement ps = con
                    .prepareStatement("SELECT account_name FROM characters WHERE char_name = ? LIMIT 1;")) {
                ps.setString(1, character);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new JSONObject().put("data", rs.getString("account_name"));
                    }

                    return new JSONObject().put("fail", "Character " + character + " account username not found");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "getCharacterUsername SQL error");
        }
    }

    protected static JSONObject addToCharacter(String character, String amount) {
        int playerId = CharInfoTable.getInstance().getIdByName(character);

        if (playerId == -1) {
            return new JSONObject().put("fail", "Character not found");
        }

        long itemAmount = Long.parseLong(amount);
        Player player = World.getInstance().getPlayer(playerId);

        if (player != null) {
            PlayerInventory inventory = player.getInventory();
            Item inventoryItem = inventory.getItemByItemId(REWARD_ID);

            InventoryUpdate iu = new InventoryUpdate();
            SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);

            if (inventoryItem != null) {
                long inventoryAmount = inventoryItem.getCount();
                inventoryItem.setCount(inventoryAmount + itemAmount);

                iu.addModifiedItem(inventoryItem);
                sm.addItemName(inventoryItem);
            } else {
                Item newItem = player.getInventory().addItem("Deposit", REWARD_ID, itemAmount, player, null);

                iu.addNewItem(newItem);
                sm.addItemName(newItem);
            }

            sm.addLong(itemAmount);

            player.sendInventoryUpdate(iu);
            player.sendPacket(sm);

            inventory.updateDatabase();

            return new JSONObject().put("data", true);
        }

        Connection con = null;

        try {
            con = DatabaseFactory.getConnection();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE items SET count = count + ? WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY';")) {
                ps.setLong(1, itemAmount);
                ps.setInt(2, playerId);
                ps.setInt(3, REWARD_ID);

                if (ps.executeUpdate() == 0) {
                    try (PreparedStatement ps1 = con.prepareStatement(
                            "INSERT INTO items (owner_id, object_id, item_id, count, loc) VALUES (?, ?, ?, ?, 'INVENTORY');")) {
                        IdManager im = IdManager.getInstance();
                        int nextId = im.getNextId();

                        ps1.setInt(1, playerId);
                        ps1.setInt(2, nextId);
                        ps1.setInt(3, REWARD_ID);
                        ps1.setLong(4, itemAmount);

                        if (ps1.executeUpdate() == 0) {
                            im.releaseId(nextId);

                            con.rollback();
                            return new JSONObject().put("fail", "Offline deposit was not successful");
                        }
                    }
                }

                con.commit();
                return new JSONObject().put("data", true);
            }
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Rollback failed: " + ex.getMessage(), ex);
                }
            }

            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "addToCharacter SQL error");
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Failed to close connection: " + ex.getMessage(), ex);
                }
            }
        }
    }

    protected static JSONObject removeFromCharacter(String character, String amount) {
        int playerId = CharInfoTable.getInstance().getIdByName(character);

        if (playerId == -1) {
            return new JSONObject().put("fail", "Character not found");
        }

        long itemAmount = Long.parseLong(amount);
        Player player = World.getInstance().getPlayer(playerId);

        if (player != null) {
            PlayerInventory inventory = player.getInventory();
            Item inventoryItem = inventory.getItemByItemId(REWARD_ID);

            InventoryUpdate iu = new InventoryUpdate();
            SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);

            if (inventoryItem == null) {
                return new JSONObject().put("fail", "Item not found in inventory");
            }

            long inventoryAmount = inventoryItem.getCount();

            if (inventoryAmount < itemAmount) {
                return new JSONObject().put("fail", "Not enough items in inventory");
            }

            if (inventoryAmount == itemAmount) {
                player.getInventory().destroyItem("Withdraw", inventoryItem, inventoryAmount, player, null);
                iu.addRemovedItem(inventoryItem);
            } else {
                inventoryItem.setCount(inventoryAmount - itemAmount);
                iu.addModifiedItem(inventoryItem);
            }

            sm.addItemName(inventoryItem);
            sm.addLong(itemAmount);

            player.sendInventoryUpdate(iu);
            player.sendPacket(sm);

            inventory.updateDatabase();

            return new JSONObject().put("data", true);
        }

        Connection con = null;

        try {
            con = DatabaseFactory.getConnection();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT count FROM items WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY' LIMIT 1;")) {
                ps.setInt(1, playerId);
                ps.setInt(2, REWARD_ID);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long inventoryAmount = rs.getLong("count");

                        if (inventoryAmount < itemAmount) {
                            con.rollback();
                            return new JSONObject().put("fail", "Not enough items in inventory");
                        }

                        if (inventoryAmount == itemAmount) {
                            try (PreparedStatement ps1 = con.prepareStatement(
                                    "DELETE FROM items WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY';")) {
                                ps1.setInt(1, playerId);
                                ps1.setInt(2, REWARD_ID);

                                if (ps1.executeUpdate() == 0) {
                                    con.rollback();
                                    return new JSONObject().put("fail",
                                            "Offline withdrawal (delete) was not successful");
                                }
                            }
                        } else {
                            try (PreparedStatement ps1 = con.prepareStatement(
                                    "UPDATE items SET count = count - ? WHERE owner_id = ? AND item_id = ? AND loc = 'INVENTORY';")) {
                                ps1.setLong(1, itemAmount);
                                ps1.setInt(2, playerId);
                                ps1.setInt(3, REWARD_ID);

                                if (ps1.executeUpdate() == 0) {
                                    con.rollback();
                                    return new JSONObject().put("fail",
                                            "Offline withdrawal (update) was not successful");
                                }
                            }
                        }

                        con.commit();
                        return new JSONObject().put("data", true);

                    }

                    con.rollback();
                    return new JSONObject().put("fail", "Item not found in inventory");
                }
            }
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Rollback failed: " + ex.getMessage(), ex);
                }
            }

            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "removeFromCharacter SQL error");
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Failed to close connection: " + ex.getMessage(), ex);
                }
            }
        }
    }

    protected static JSONObject isGameServerAvailable() {
        Shutdown sd = Shutdown.getInstance();

        if (sd == null) {
            return new JSONObject().put("fail", "No Shutdown instance");
        }

        return new JSONObject().put("data", sd.getMode() == 0);
    }

    protected static JSONObject fetchGameServerBalance() {
        try (Connection con = DatabaseFactory.getConnection()) {
            try (PreparedStatement ps = con
                    .prepareStatement("SELECT SUM(count) AS balance FROM items WHERE item_id = ?;")) {
                ps.setInt(1, REWARD_ID);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getString("balance") != null) {
                        return new JSONObject().put("data", rs.getString("balance"));
                    }

                    return new JSONObject().put("data", "0");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database error: " + e.getMessage(), e);
            return new JSONObject().put("fail", "fetchGameServerBalance SQL error");
        }
    }
}