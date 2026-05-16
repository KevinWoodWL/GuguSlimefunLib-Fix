package me.ddggdd135.guguslimefunlib.items;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import me.ddggdd135.guguslimefunlib.utils.ItemUtils;
import me.matl114.matlib.nmsMirror.impl.CraftBukkit;
import me.matl114.matlib.nmsMirror.impl.NMSItem;
import org.bukkit.inventory.ItemStack;

/**
 * 物品的内容指纹键, 用作 ItemHashMap / ItemHashSet 等容器的 key.
 *
 * <p>性能关键点:
 * <ul>
 *   <li>SF 物品 (非 DistinctiveItem): equals/hash 只比较 ItemType (= SF ID + Material), 完全跳过 NMS matchItem 与 customHashcode.</li>
 *   <li>DistinctiveItem / 含 meta 的原版物品: 走 matlib 的 NMS 内容比较, 行为与旧版一致.</li>
 *   <li>SF 规范化 ItemStack 按 SF ID 缓存到静态 map, 避免每次 new ItemKey 都重新做
 *       SlimefunItem.getById().getItem().asOne().</li>
 * </ul>
 */
public class ItemKey {
    /**
     * SF ID -> 规范化 ItemStack 缓存. 命中后跳过 SlimefunItem.getById().getItem().asOne().
     * DistinctiveItem 与未注册 ID 不放入此 map, 走 NMS 路径.
     */
    private static final ConcurrentHashMap<String, ItemStack> SF_CANONICAL_CACHE = new ConcurrentHashMap<>();

    private final ItemStack itemStack;
    private final ItemType type;

    /** SF 非 distinctive 路径下为 null, 否则为对应的 NMS ItemStack 实例 (用作 matchItem 比较的输入). */
    private final Object nms;

    /** true 表示 equals 必须落到 NMS matchItem; false 表示 type 相等即对象相等. */
    private final boolean nmsCompare;

    private final int hash;

    public ItemKey(ItemStack itemStack) {
        try {
            itemStack = itemStack.asOne();
            this.type = ItemUtils.getItemType(itemStack);

            // 快路径: 非 DistinctiveItem 的 SF 物品 -> 用 ItemType 作为身份, 不再下沉到 NMS.
            if (this.type.getIsSlimefun() && this.type.getId() != null) {
                ItemStack canonical = getOrCacheCanonical(this.type.getId());
                if (canonical != null) {
                    this.itemStack = canonical;
                    this.nms = null;
                    this.nmsCompare = false;
                    this.hash = this.type.hashCode();
                    return;
                }
                // canonical == null 表示是 DistinctiveItem 或 ID 找不到对应物品, 退到默认 NMS 路径.
            }

            // 默认路径: DistinctiveItem / 原版物品, 必须用 NMS 做内容比较以区分 meta 差异.
            Object nmsStack = CraftBukkit.ITEMSTACK.unwrapToNMS(itemStack);
            this.nms = nmsStack;
            this.itemStack = CraftBukkit.ITEMSTACK.asCraftMirror(nmsStack);
            this.nmsCompare = true;
            this.hash = NMSItem.ITEMSTACK.customHashcode(nmsStack);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    /**
     * 返回 SF 物品的规范化 ItemStack (asOne).
     * 若 ID 未注册或是 DistinctiveItem, 返回 null 表示调用方应走 NMS 路径.
     */
    private static ItemStack getOrCacheCanonical(String sfId) {
        ItemStack cached = SF_CANONICAL_CACHE.get(sfId);
        if (cached != null) return cached;

        SlimefunItem sfItem = SlimefunItem.getById(sfId);
        if (sfItem == null || sfItem instanceof DistinctiveItem) return null;

        ItemStack canonical = sfItem.getItem().asOne();
        ItemStack existing = SF_CANONICAL_CACHE.putIfAbsent(sfId, canonical);
        return existing != null ? existing : canonical;
    }

    /**
     * 重新注册 / reload 时可以调用此方法清空 SF 规范化缓存.
     * 当前没有自动触发点; 如果观察到 reload 后 getItemStack() 显示陈旧, 在 plugin reload 钩子里调用.
     */
    public static void clearSlimefunCanonicalCache() {
        SF_CANONICAL_CACHE.clear();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public ItemType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey that)) return false;

        // 第一道屏障: ItemType 不同直接判否, 避免任何 NMS 调用.
        if (!Objects.equals(this.type, that.type)) return false;

        // 两侧都走 SF 快路径, type 相等就足够判等.
        if (!this.nmsCompare && !that.nmsCompare) return true;

        // 兜底: 任一侧 nms 为 null 但 type 已相等, 按相等处理.
        // 正常情况下不会触发, 因为相同 SF ID 应当两侧都走快路径.
        if (this.nms == null || that.nms == null) return true;

        try {
            return NMSItem.ITEMSTACK.matchItem(nms, that.nms, false, false);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "ItemKey{" + "itemStack=" + itemStack + '}';
    }
}
