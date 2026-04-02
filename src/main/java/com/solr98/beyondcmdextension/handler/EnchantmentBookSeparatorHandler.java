package com.solr98.beyondcmdextension.handler;

import com.mojang.logging.LogUtils;
import com.solr98.beyondcmdextension.Config;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.helper.UnifiedStorageBeforeInsertHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantmentBookSeparatorHandler implements UnifiedStorageBeforeInsertHandler.BeforeInsertHandler {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean typesLogged = false;
    
    @Override
    public @NotNull UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo beforeInsert(
            @NotNull KeyAmount originalInsert,
            @NotNull KeyAmount tryInsert,
            DimensionsNet net) {
        
        // 调试：记录所有附魔类型信息（仅第一次）
        if (!typesLogged && LOGGER.isDebugEnabled()) {
            logAllEnchantmentTypes();
            typesLogged = true;
        }
        
        if (!Config.enableEnchantmentSeparation) {
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        if (!(tryInsert.key() instanceof ItemStackKey itemStackKey)) {
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        ItemStack itemStack = itemStackKey.copyStackWithCount(1);
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof EnchantedBookItem)) {
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        if (net == null) {
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        List<EnchantmentInstance> enchantments = extractEnchantments(itemStack);
        if (enchantments.isEmpty()) {
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        // 如果只有一个附魔，直接通过，不进行处理
        if (enchantments.size() <= 1) {
            LOGGER.debug("Single-enchantment book detected ({} enchantments), skipping separation", enchantments.size());
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        long bookCount = tryInsert.amount();
        long totalExperienceFluidCost = calculateTotalExperienceCost(enchantments, bookCount);
        long totalExperiencePoints = totalExperienceFluidCost / 20;
        
        LOGGER.debug("Processing enchanted book: {} books with {} enchantments, total cost: {} XP ({} mb fluid)", 
                bookCount, enchantments.size(), totalExperiencePoints, totalExperienceFluidCost);
        
        // 检查是否有足够的普通书（空白书）来创建新的附魔书
        long totalBooksNeeded = enchantments.size() * bookCount;
        long availableBooks = getAvailableBooks(net);
        long availableExperience = getStoredExperience(net);
        
        LOGGER.debug("Resource check - Needed: {} books, {} XP ({} mb) | Available: {} books, {} mb", 
                totalBooksNeeded, totalExperiencePoints, totalExperienceFluidCost, availableBooks, availableExperience);
        
        // 检查存储空间是否足够存放分离后的附魔书
        boolean canStore = canStoreAllEnchantments(net, enchantments, bookCount);
        
        // 如果资源不足或存储空间不足，直接返回原物品，让系统正常插入
        if (availableBooks < totalBooksNeeded) {
            LOGGER.debug("Not enough regular books in network: needed {}, available {}. Allowing book to enter network without separation.", 
                    totalBooksNeeded, availableBooks);
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        if (availableExperience < totalExperienceFluidCost) {
            LOGGER.debug("Not enough experience in network: needed {} mb ({} XP), available {} mb. Allowing book to enter network without separation.", 
                    totalExperienceFluidCost, totalExperiencePoints, availableExperience);
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        if (!canStore) {
            LOGGER.debug("Not enough storage space for separated enchantments. Allowing book to enter network without separation.");
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        LOGGER.debug("All resource checks passed, proceeding with separation");
        
        // 消耗资源并执行分离
        if (!consumeExperience(net, totalExperienceFluidCost)) {
            LOGGER.debug("Failed to consume experience (unexpected)");
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        LOGGER.debug("Experience consumption successful");
        
        // 消耗普通书
        if (!consumeBooks(net, totalBooksNeeded)) {
            LOGGER.debug("Failed to consume regular books (unexpected)");
            return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(tryInsert, false);
        }
        
        LOGGER.debug("Book consumption successful");
        
        List<KeyAmount> outputs = new ArrayList<>();
        
        for (EnchantmentInstance enchantment : enchantments) {
            ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(enchantedBook, enchantment);
            ItemStackKey outputKey = new ItemStackKey(enchantedBook);
            outputs.add(new KeyAmount(outputKey, bookCount));
            LOGGER.debug("Will output: {} x {} (level {})", 
                    bookCount, BuiltInRegistries.ENCHANTMENT.getKey(enchantment.enchantment), enchantment.level);
        }
        
        for (KeyAmount output : outputs) {
            LOGGER.debug("Inserting separated book: {} x {}", output.amount(), output.key());
            KeyAmount remaining = net.getUnifiedStorage().insert(output.key(), output.amount(), false);
            LOGGER.debug("Insert result: {} remaining", remaining.amount());
        }
        

        
        // 尝试返回一个完全不同的空物品，看看系统如何处理
        // 这可能让系统消耗原书而不插入任何东西
        LOGGER.debug("Returning completely different empty item to hopefully consume original book");
        ItemStackKey emptyKey = new ItemStackKey(new ItemStack(Items.AIR));
        return new UnifiedStorageBeforeInsertHandler.BeforeInsertHandlerReturnInfo(
                new KeyAmount(emptyKey, 0), false);
    }
    
    private List<EnchantmentInstance> extractEnchantments(ItemStack itemStack) {
        List<EnchantmentInstance> enchantments = new ArrayList<>();
        CompoundTag tag = itemStack.getTag();
        
        if (tag != null && tag.contains("StoredEnchantments", 9)) {
            ListTag enchantmentsList = tag.getList("StoredEnchantments", 10);
            
            for (int i = 0; i < enchantmentsList.size(); i++) {
                CompoundTag enchantmentTag = enchantmentsList.getCompound(i);
                String id = enchantmentTag.getString("id");
                short level = enchantmentTag.getShort("lvl");
                
                Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(ResourceLocation.tryParse(id));
                if (enchantment != null) {
                    enchantments.add(new EnchantmentInstance(enchantment, level));
                }
            }
        }
        
        return enchantments;
    }
    
    private long calculateTotalExperienceCost(List<EnchantmentInstance> enchantments, long bookCount) {
        long totalExperiencePoints = 0;
        
        for (EnchantmentInstance enchantment : enchantments) {
            int level = enchantment.level;
            int baseCost = Config.enchantmentSeparationBaseCost;
            int levelMultiplier = Config.enchantmentSeparationLevelMultiplier;
            
            long enchantmentExperience;
            
            if (Config.useFormula && Config.costFormula != null && !Config.costFormula.trim().isEmpty()) {
                // 使用自定义公式计算
                enchantmentExperience = calculateWithFormula(baseCost, level, levelMultiplier, bookCount);
            } else {
                // 使用默认公式计算
                enchantmentExperience = (baseCost + (level - 1) * levelMultiplier) * bookCount;
            }
            
            // 使用等级名单系统获取乘数
            double multiplier = getEnchantmentMultiplierFromList(enchantment.enchantment);
            enchantmentExperience = (long) (enchantmentExperience * multiplier);
            
            totalExperiencePoints += enchantmentExperience;
        }
        
        // 转换比率: 1经验值 = 20mb 经验流体
        return totalExperiencePoints * 20;
    }
    
    private long calculateWithFormula(int baseCost, int level, int levelMultiplier, long bookCount) {
        try {
            Map<String, Double> variables = new HashMap<>();
            variables.put("base", (double) baseCost);
            variables.put("level", (double) level);
            variables.put("multiplier", (double) levelMultiplier);
            variables.put("books", (double) bookCount);
            
            // 添加常用变量别名
            variables.put("lvl", (double) level);
            variables.put("count", (double) bookCount);
            variables.put("cost", (double) baseCost);
            
            double result = FormulaParser.evaluate(Config.costFormula, variables);
            
            // 确保结果为非负数
            if (result < 0) {
                LOGGER.warn("Formula result is negative: {}, using 0 instead", result);
                result = 0;
            }
            
            return (long) result;
        } catch (Exception e) {
            LOGGER.error("Error calculating cost with formula '{}': {}", Config.costFormula, e.getMessage());
            // 公式错误时使用默认计算
            return (baseCost + (level - 1) * levelMultiplier) * bookCount;
        }
    }
    
    private double getEnchantmentMultiplierFromList(Enchantment enchantment) {
        ResourceLocation enchantmentId = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (enchantmentId == null) {
            return Config.defaultEnchantmentMultiplier;
        }
        
        String id = enchantmentId.toString();
        
        // 检查等级名单
        for (String entry : Config.highCostEnchantments) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                String entryId = parts[0] + ":" + parts[1];
                if (entryId.equals(id)) {
                    // 如果有指定乘数，使用它；否则使用默认乘数
                    if (parts.length >= 3) {
                        try {
                            return Double.parseDouble(parts[2]);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid multiplier format for enchantment {}: {}", entry, e.getMessage());
                        }
                    }
                    return Config.defaultEnchantmentMultiplier;
                }
            }
        }
        
        // 不在名单中，使用默认乘数
        return Config.defaultEnchantmentMultiplier;
    }
    
    // 辅助方法：获取附魔的类型信息（用于帮助构建等级名单）
    private String getEnchantmentTypeInfo(Enchantment enchantment) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (id == null) {
            return "unknown";
        }
        
        StringBuilder info = new StringBuilder();
        info.append(id.toString()).append(": ");
        
        try {
            // 尝试使用Minecraft Forge API的标准方法
            // 在Minecraft 1.20.1中，常见的方法名：
            
            // 方法1: 使用已知的方法名尝试
            java.lang.reflect.Method[] methods = enchantment.getClass().getMethods();
            for (java.lang.reflect.Method method : methods) {
                String methodName = method.getName();
                if (methodName.equals("isTreasure") || methodName.equals("isTreasureEnchantment")) {
                    if (method.getParameterCount() == 0 && method.getReturnType() == boolean.class) {
                        boolean isTreasure = (Boolean) method.invoke(enchantment);
                        if (isTreasure) info.append("treasure ");
                    }
                } else if (methodName.equals("isCurse")) {
                    if (method.getParameterCount() == 0 && method.getReturnType() == boolean.class) {
                        boolean isCurse = (Boolean) method.invoke(enchantment);
                        if (isCurse) info.append("curse ");
                    }
                } else if (methodName.equals("isTreasureOnly")) {
                    if (method.getParameterCount() == 0 && method.getReturnType() == boolean.class) {
                        boolean isTreasureOnly = (Boolean) method.invoke(enchantment);
                        if (isTreasureOnly) info.append("treasure-only ");
                    }
                }
            }
            
            // 如果没有找到类型信息，添加默认标签
            if (info.toString().endsWith(": ")) {
                info.append("normal");
            }
            
        } catch (Exception e) {
            info.append("error: ").append(e.getMessage());
        }
        
        return info.toString();
    }
    
    // 调试方法：记录所有附魔的类型信息（用于帮助配置等级名单）
    private void logAllEnchantmentTypes() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("=== Enchantment Type Information ===");
            for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
                String typeInfo = getEnchantmentTypeInfo(enchantment);
                LOGGER.debug(typeInfo);
            }
            LOGGER.debug("===================================");
        }
    }
    
    private boolean hasEnoughExperience(DimensionsNet net, long requiredExperience) {
        if (requiredExperience <= 0) {
            return true;
        }
        
        long currentExperience = getStoredExperience(net);
        return currentExperience >= requiredExperience;
    }
    
    private long getStoredExperience(DimensionsNet net) {
        FluidStackKey xpFluidKey = createXpFluidKey();
        KeyAmount experienceAmount = net.getUnifiedStorage().getStackByKey(xpFluidKey);
        return experienceAmount.amount();
    }
    
    private boolean consumeExperience(DimensionsNet net, long experienceAmount) {
        if (experienceAmount <= 0) {
            return true;
        }
        
        FluidStackKey xpFluidKey = createXpFluidKey();
        LOGGER.debug("Attempting to extract {} mB of experience fluid", experienceAmount);
        
        KeyAmount extracted = net.getUnifiedStorage().extract(xpFluidKey, experienceAmount, false, false);
        LOGGER.debug("Extracted {} mB of experience fluid", extracted.amount());
        
        boolean success = extracted.amount() >= experienceAmount;
        if (!success) {
            LOGGER.debug("Failed to extract enough experience: needed {}, got {}", experienceAmount, extracted.amount());
        }
        
        return success;
    }
    
    private boolean hasEnoughBooks(DimensionsNet net, long booksNeeded) {
        if (booksNeeded <= 0) {
            return true;
        }
        
        long availableBooks = getAvailableBooks(net);
        return availableBooks >= booksNeeded;
    }
    
    private long getAvailableBooks(DimensionsNet net) {
        ItemStackKey bookKey = new ItemStackKey(new ItemStack(Items.BOOK));
        KeyAmount bookAmount = net.getUnifiedStorage().getStackByKey(bookKey);
        return bookAmount.amount();
    }
    
    private boolean consumeBooks(DimensionsNet net, long booksNeeded) {
        if (booksNeeded <= 0) {
            return true;
        }
        
        ItemStackKey bookKey = new ItemStackKey(new ItemStack(Items.BOOK));
        LOGGER.debug("Attempting to extract {} books", booksNeeded);
        
        KeyAmount extracted = net.getUnifiedStorage().extract(bookKey, booksNeeded, false, false);
        LOGGER.debug("Extracted {} books", extracted.amount());
        
        boolean success = extracted.amount() >= booksNeeded;
        if (!success) {
            LOGGER.debug("Failed to extract enough books: needed {}, got {}", booksNeeded, extracted.amount());
        }
        
        return success;
    }
    
    private FluidStackKey createXpFluidKey() {
        if (BDFluids.XP_FLUID != null && BDFluids.XP_FLUID.source() != null) {
            FluidStack xpFluidStack = new FluidStack(BDFluids.XP_FLUID.source().get(), 1);
            FluidStackKey key = new FluidStackKey(xpFluidStack);
            LOGGER.debug("Created XP fluid key: {} (hash: {})", 
                    BDFluids.XP_FLUID.source().getId(), key.hashCode());
            return key;
        }
        
        LOGGER.warn("XP fluid not found, using empty fluid as fallback");
        return new FluidStackKey(new FluidStack(Fluids.EMPTY, 1));
    }
    
    private boolean canStoreAllEnchantments(DimensionsNet net, List<EnchantmentInstance> enchantments, long bookCount) {
        for (EnchantmentInstance enchantment : enchantments) {
            ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(enchantedBook, enchantment);
            ItemStackKey outputKey = new ItemStackKey(enchantedBook);
            
            KeyAmount currentStack = net.getUnifiedStorage().getStackByKey(outputKey);
            long currentAmount = currentStack.amount();
            
            long slotCapacity = net.getUnifiedStorage().getSlotCapacity(0);
            LOGGER.debug("Storage check: current={}, toAdd={}, slotCapacity={} for {} (key hash: {})", 
                    currentAmount, bookCount, slotCapacity, 
                    BuiltInRegistries.ENCHANTMENT.getKey(enchantment.enchantment),
                    outputKey.hashCode());
            
            if (slotCapacity <= 0) {
                slotCapacity = Long.MAX_VALUE;
                LOGGER.debug("Slot capacity was <= 0, using Long.MAX_VALUE");
            }
            
            if (currentAmount + bookCount > slotCapacity) {
                LOGGER.debug("Storage insufficient: {} + {} > {}", currentAmount, bookCount, slotCapacity);
                return false;
            }
            
            LOGGER.debug("Storage sufficient for this enchantment");
        }
        
        LOGGER.debug("All storage checks passed");
        return true;
    }
}