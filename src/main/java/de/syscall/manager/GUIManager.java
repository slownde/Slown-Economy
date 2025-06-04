package de.syscall.manager;

import de.syscall.gui.BankGUI;
import de.syscall.gui.CoinsGUI;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager {

    private final Map<UUID, CoinsGUI> openCoinsGUIs = new ConcurrentHashMap<>();
    private final Map<UUID, BankGUI> openBankGUIs = new ConcurrentHashMap<>();

    public void registerCoinsGUI(UUID playerUUID, CoinsGUI gui) {
        openCoinsGUIs.put(playerUUID, gui);
    }

    public void registerBankGUI(UUID playerUUID, BankGUI gui) {
        openBankGUIs.put(playerUUID, gui);
    }

    public void unregisterCoinsGUI(UUID playerUUID) {
        openCoinsGUIs.remove(playerUUID);
    }

    public void unregisterBankGUI(UUID playerUUID) {
        openBankGUIs.remove(playerUUID);
    }

    public void updatePlayerGUIs(UUID playerUUID) {
        CoinsGUI coinsGUI = openCoinsGUIs.get(playerUUID);
        if (coinsGUI != null) {
            coinsGUI.updateGUI();
        }

        BankGUI bankGUI = openBankGUIs.get(playerUUID);
        if (bankGUI != null) {
            bankGUI.updateGUI();
        }
    }

    public void clearPlayerGUIs(UUID playerUUID) {
        openCoinsGUIs.remove(playerUUID);
        openBankGUIs.remove(playerUUID);
    }
}