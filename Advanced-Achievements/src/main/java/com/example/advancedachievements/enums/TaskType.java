package com.example.advancedachievements.enums;

public enum TaskType {
    BLOCK_BREAK("block_break", "Break specific blocks or any blocks"),
    ITEM_PICKUP("item_pickup", "Pick up and collect items from the ground"),
    MOB_KILL("mob_kill", "Defeat hostile mobs or specific creatures"),
    ITEM_CRAFT("item_craft", "Craft items using a crafting table or inventory"),
    FISHING("fishing", "Catch fish or treasures using a fishing rod"),
    EATING("eating", "Consume food items to restore hunger"),
    ENCHANTING("enchanting", "Enchant items using an enchantment table"),
    TRADING("trading", "Trade with villagers using emeralds"),
    MINING("mining", "Mine and collect ores from underground"),
    BREEDING("breeding", "Breed animals to create baby animals"),
    TAMING("taming", "Tame wild animals to make them your pets"),
    DEATH("death", "Die a certain number of times");
    
    private final String id;
    private final String displayName;
    
    TaskType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static TaskType fromId(String id) {
        for (TaskType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
    
    public static String getAvailableTypes() {
        StringBuilder sb = new StringBuilder();
        TaskType[] types = values();
        boolean first = true;
        
        for (TaskType type : types) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(type.getId());
            first = false;
        }
        return sb.toString();
    }
    
    public static java.util.List<String> getAvailableTypesList() {
        java.util.List<String> types = new java.util.ArrayList<>();
        for (TaskType type : values()) {
            types.add(type.getId());
        }
        return types;
    }
}