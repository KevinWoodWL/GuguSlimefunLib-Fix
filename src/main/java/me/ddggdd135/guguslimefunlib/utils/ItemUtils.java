package me.ddggdd135.guguslimefunlib.utils;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import io.github.thebusybiscuit.slimefun4.core.debug.Debug;
import io.github.thebusybiscuit.slimefun4.core.debug.TestCase;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ddggdd135.guguslimefunlib.api.ItemHashMap;
import me.ddggdd135.guguslimefunlib.items.ItemKey;
import me.ddggdd135.guguslimefunlib.items.ItemType;
import me.matl114.matlib.nmsMirror.impl.CraftBukkit;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class ItemUtils {
    private static final String SOULBOUND_LORE = ChatColor.GRAY + "灵魂绑定";

    @Nonnull
    public static ItemStack[] createItems(@Nonnull ItemStack template, int amount) {
        List<ItemStack> itemStacks = new ArrayList<>();
        int rest = amount;
        while (true) {
            if (rest <= template.getMaxStackSize()) {
                ItemStack itemStack = template.asQuantity(rest);
                itemStacks.add(itemStack);
                break;
            } else {
                rest -= template.getMaxStackSize();
                ItemStack itemStack = template.asQuantity(template.getMaxStackSize());
                itemStacks.add(itemStack);
            }
        }
        return itemStacks.toArray(new ItemStack[0]);
    }

    @Nonnull
    public static ItemStack[] createItems(@Nonnull Map<ItemStack, Integer> storage) {
        List<ItemStack> itemStacks = new ArrayList<>();
        for (ItemStack itemStack : storage.keySet()) {
            itemStacks.addAll(List.of(createItems(itemStack, storage.get(itemStack))));
        }
        return itemStacks.toArray(new ItemStack[0]);
    }

    @Nonnull
    public static ItemStack[] trimItems(@Nonnull ItemStack[] itemStacks) {
        List<ItemStack> itemStackList = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack.getAmount() > 0) {
                itemStackList.add(itemStack);
            }
        }
        return itemStackList.toArray(new ItemStack[0]);
    }

    @Nonnull
    public static ItemHashMap<Integer> getAmounts(@Nonnull ItemStack[] itemStacks) {
        ItemHashMap<Integer> storage = new ItemHashMap<>();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack == null || itemStack.getType().isAir()) continue;
            ItemKey itemKey = new ItemKey(itemStack);
            if (storage.containsKey(itemKey)) {
                storage.putKey(itemKey, storage.getKey(itemKey) + itemStack.getAmount());
            } else {
                storage.putKey(itemKey, itemStack.getAmount());
            }
        }
        return storage;
    }

    @Nonnull
    public static Map<ItemStack, Integer> takeItems(
            @Nonnull Map<ItemStack, Integer> source, @Nonnull Map<ItemStack, Integer> toTake) {
        Map<ItemStack, Integer> storage = new ItemHashMap<>(source);
        for (ItemStack itemStack : toTake.keySet()) {
            if (storage.containsKey(itemStack)) {
                storage.put(itemStack, storage.get(itemStack) - toTake.get(itemStack));
            } else {
                storage.put(itemStack, -toTake.get(itemStack));
            }
        }
        return storage;
    }

    @Nonnull
    public static ItemHashMap<Integer> addItems(
            @Nonnull Map<ItemStack, Integer> source, @Nonnull Map<ItemStack, Integer> toAdd) {
        ItemHashMap<Integer> storage = new ItemHashMap<>(source);
        for (ItemStack itemStack : toAdd.keySet()) {
            if (storage.containsKey(itemStack)) {
                storage.put(itemStack, storage.get(itemStack) + toAdd.get(itemStack));
            } else {
                storage.put(itemStack, toAdd.get(itemStack));
            }
        }
        return storage;
    }

    public static void trim(@Nonnull Map<ItemStack, Integer> storage) {
        List<ItemStack> toRemove = new ArrayList<>();
        for (ItemStack itemStack : storage.keySet()) {
            if (itemStack == null || itemStack.getType().isAir() || storage.get(itemStack) <= 0)
                toRemove.add(itemStack);
        }
        for (ItemStack itemStack : toRemove) {
            storage.remove(itemStack);
        }
    }

    public static boolean contains(BlockMenu inv, int[] slots, ItemStack[] itemStacks) {
        Map<ItemStack, Integer> toTake = getAmounts(itemStacks);

        for (ItemStack itemStack : toTake.keySet()) {
            if (toTake.get(itemStack) > getItemAmount(inv, slots, itemStack)) {
                return false;
            }
        }
        return true;
    }

    public static int getItemAmount(BlockMenu inv, int[] slots, ItemStack itemStack) {
        int founded = 0;
        for (int slot : slots) {
            ItemStack item = inv.getItemInSlot(slot);
            if (item == null || item.getType().isAir()) continue;
            if (SlimefunUtils.isItemSimilar(item, itemStack, true, false)) {
                founded += item.getAmount();
            }
        }
        return founded;
    }

    @NotNull private static ItemStack[] getVanillaItemStacks(Block block) {
        Container container = (Container) block.getState();
        ItemStack[] items = new ItemStack[0];
        if (container instanceof Furnace furnace) {
            FurnaceInventory furnaceInventory = furnace.getInventory();
            items = new ItemStack[] {furnaceInventory.getResult()};
        } else if (container instanceof Chest chest) {
            Inventory inventory = chest.getBlockInventory();
            items = inventory.getContents();
        }
        return items;
    }

    public static <T extends SlimefunItem> T setRecipeOutput(@Nonnull T item, @Nonnull ItemStack output) {
        item.setRecipeOutput(output);
        return item;
    }

    public static ItemStack[] takeItem(
            @Nonnull BlockMenu blockMenu, @Nonnull ItemHashMap<Integer> items, int... slots) {
        ItemHashMap<Integer> amounts = new ItemHashMap<>(items);
        ItemHashMap<Integer> found = new ItemHashMap<>();

        for (ItemStack itemStack : amounts.keySet()) {
            for (int slot : slots) {
                ItemStack item = blockMenu.getItemInSlot(slot);
                if (item == null || item.getType().isAir()) continue;
                if (SlimefunUtils.isItemSimilar(item, itemStack, true, false)) {
                    if (item.getAmount() > amounts.get(itemStack)) {
                        found.put(itemStack, found.getOrDefault(itemStack, 0) + amounts.get(itemStack));
                        int rest = item.getAmount() - amounts.get(itemStack);
                        item.setAmount(rest);
                        blockMenu.markDirty();
                        break;
                    } else {
                        found.put(itemStack, found.getOrDefault(itemStack, 0) + item.getAmount());
                        blockMenu.replaceExistingItem(slot, null);
                        int rest = amounts.get(itemStack) - item.getAmount();
                        if (rest != 0) amounts.put(itemStack, rest);
                        else break;
                    }
                }
            }
        }

        return createItems(found);
    }

    @Nonnull
    public static ItemType getItemType(@Nonnull ItemStack itemStack) {
        if (itemStack.getType().isAir() || itemStack.getAmount() == 0) return new ItemType(false, null, Material.AIR);
        if (itemStack instanceof SlimefunItemStack sfis) {
            return new ItemType(true, sfis.getItemId(), sfis.getType());
        }

        String id = getSFId(itemStack);

        if (id != null) {
            return new ItemType(true, id, itemStack.getType());
        }

        return new ItemType(false, null, itemStack.getType());
    }

    @Nullable public static String getSFId(@Nonnull ItemStack itemStack) {
        PersistentDataContainer pdc;
        if (CraftBukkit.ITEMSTACK.isCraftItemStack(itemStack)) {
            pdc = me.matl114.matlib.nmsUtils.ItemUtils.getPersistentDataContainerView(itemStack, false);
        } else {
            // Bukkit 侧无 meta 的物品不可能挂 SF PDC, 直接短路, 避免触发昂贵的 getItemMeta() 反序列化.
            if (!itemStack.hasItemMeta()) return null;
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null) return null;
            pdc = meta.getPersistentDataContainer();
        }

        if (pdc == null) return null;

        return pdc.get(Slimefun.getItemDataService().getKey(), PersistentDataType.STRING);
    }

    public static @Nonnull Optional<DistinctiveItem> getDistinctiveItem(@Nonnull String id) {
        SlimefunItem slimefunItem = SlimefunItem.getById(id);
        if (slimefunItem instanceof DistinctiveItem distinctiveItem) {
            return Optional.of(distinctiveItem);
        }
        return Optional.empty();
    }

    public static boolean equalsItemMeta(
            @Nonnull ItemMeta itemMeta,
            @Nonnull ItemMeta sfitemMeta,
            boolean checkLore,
            boolean checkCustomModelCheck) {
        if (itemMeta.hasDisplayName() != sfitemMeta.hasDisplayName()) return false;
        else if (itemMeta.hasDisplayName()
                && sfitemMeta.hasDisplayName()
                && !itemMeta.getDisplayName().equals(sfitemMeta.getDisplayName())) return false;
        else if (checkLore) {
            boolean hasItemMetaLore = itemMeta.hasLore();
            boolean hasSfItemMetaLore = sfitemMeta.hasLore();

            if (hasItemMetaLore && hasSfItemMetaLore) {
                if (!equalsLore(itemMeta.getLore(), sfitemMeta.getLore())) return false;

            } else if (hasItemMetaLore != hasSfItemMetaLore) return false;
        }

        if (checkCustomModelCheck) {
            // Fixes #3133: name and lore are not enough
            boolean hasItemMetaCustomModelData = itemMeta.hasCustomModelData();
            boolean hasSfItemMetaCustomModelData = sfitemMeta.hasCustomModelData();
            if (hasItemMetaCustomModelData
                    && hasSfItemMetaCustomModelData
                    && itemMeta.getCustomModelData() != sfitemMeta.getCustomModelData()) return false;
            else if (hasItemMetaCustomModelData != hasSfItemMetaCustomModelData) return false;
        }

        if (itemMeta instanceof PotionMeta potionMeta && sfitemMeta instanceof PotionMeta sfPotionMeta) {
            if (!potionMeta.hasBasePotionType() && !sfPotionMeta.hasBasePotionType()) return true;

            return potionMeta.hasBasePotionType()
                    && sfPotionMeta.hasBasePotionType()
                    && potionMeta.getBasePotionType().equals(sfPotionMeta.getBasePotionType());
        }

        Debug.log(TestCase.CARGO_INPUT_TESTING, "  All meta checked.");

        return true;
    }

    /**
     * This checks if the two provided lores are equal.
     * This method will ignore any lines such as the soulbound one.
     *
     * @param lore1
     *            The first lore
     * @param lore2
     *            The second lore
     *
     * @return Whether the two lores are equal
     */
    public static boolean equalsLore(@Nonnull List<String> lore1, @Nonnull List<String> lore2) {
        Validate.notNull(lore1, "Cannot compare lore that is null!");
        Validate.notNull(lore2, "Cannot compare lore that is null!");

        List<String> longerList = lore1.size() > lore2.size() ? lore1 : lore2;
        List<String> shorterList = lore1.size() > lore2.size() ? lore2 : lore1;

        int a = 0;
        int b = 0;

        for (; a < longerList.size(); a++) {
            if (isLineIgnored(longerList.get(a))) continue;

            while (shorterList.size() > b && isLineIgnored(shorterList.get(b))) b++;

            if (b >= shorterList.size()) return false;
            else if (longerList.get(a).equals(shorterList.get(b))) b++;
            else return false;
        }

        while (shorterList.size() > b && isLineIgnored(shorterList.get(b))) b++;

        return b == shorterList.size();
    }

    private static boolean isLineIgnored(@Nonnull String line) {
        return line.equals(SOULBOUND_LORE);
    }
}
