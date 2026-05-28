package com.solr98.beyondintegration.client;
import java.util.HashMap;
import java.util.Map;

public enum SuperbAmmoCache {
    INSTANCE;

    // Player data
    private final Map<String, Long> ammoMap = new HashMap<>();
    private boolean hasData = false;
    private long lastUpdateTime = 0;
    private int netId = -1;
    private long networkEnergy = -1;
    private String networkName = "";
    private boolean enchantSeparation = true;

    // Vehicle data
    private final Map<String, Long> vehicleAmmoMap = new HashMap<>();
    private boolean vehicleHasData = false;
    private long vehicleLastUpdate = 0;
    private int vehicleNetId = -1;
    private long vehicleEnergy = -1;
    private String vehicleNetworkName = "";

    private static final long TTL = Long.MAX_VALUE;
    private static final long VEHICLE_TTL = 120000;

    // ── Player ──
    public void update(int id, Map<String, Long> map, long energy, String name) {
        netId = id; networkName = name != null ? name : ""; networkEnergy = energy;
        ammoMap.clear(); if (map != null) ammoMap.putAll(map);
        hasData = true; lastUpdateTime = System.currentTimeMillis();
    }

    public void setPlayerData(int id, Map<String, Long> map, long energy, String name) { update(id, map, energy, name); }

    public long getCount(String t) { return ammoMap.getOrDefault(t, 0L); }
    public boolean hasInfinite() { return ammoMap.getOrDefault("__infinite__", 0L) > 0; }
    public boolean hasData() { return hasData && System.currentTimeMillis() - lastUpdateTime < TTL; }
    public Map<String, Long> getAll() { return new HashMap<>(ammoMap); }
    public int getNetId() { return netId; }
    public long getNetworkEnergy() { return networkEnergy; }
    public String getNetworkName() { return networkName; }
    public boolean getEnchantSeparation() { return enchantSeparation; }
    public void setEnchantSeparation(boolean v) { enchantSeparation = v; }

    // ── Vehicle ──
    public void updateVehicle(int id, Map<String, Long> map, long energy, String name) {
        vehicleNetId = id; vehicleNetworkName = name != null ? name : ""; vehicleEnergy = energy;
        vehicleAmmoMap.clear(); if (map != null) vehicleAmmoMap.putAll(map);
        vehicleHasData = true; vehicleLastUpdate = System.currentTimeMillis();
    }

    public long getVehicleCount(String t) { return vehicleAmmoMap.getOrDefault(t, 0L); }
    public boolean vehicleHasData() { return vehicleHasData && System.currentTimeMillis() - vehicleLastUpdate < VEHICLE_TTL; }
    public int getVehicleNetId() { return vehicleNetId; }
    public long getVehicleEnergy() { return vehicleEnergy; }
    public String getVehicleNetName() { return vehicleNetworkName; }

    public void resetVehicle() {
        vehicleNetId = -1; vehicleAmmoMap.clear(); vehicleEnergy = -1; vehicleHasData = false; vehicleNetworkName = "";
    }

    // ── Shared ──
    public void clear() {
        netId = -1; ammoMap.clear(); networkEnergy = -1; hasData = false; networkName = "";
        vehicleNetId = -1; vehicleAmmoMap.clear(); vehicleEnergy = -1; vehicleHasData = false; vehicleNetworkName = "";
    }
}
