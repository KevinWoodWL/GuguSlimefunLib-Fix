# GuguSlimefunLib Fix

基于 TimetownDev/GuguSlimefunLib 的性能修复版。

## 改动

- 优化 `ItemKey`：普通 Slimefun 物品按 SF ID 和材质判等，避免频繁 NMS 比较。
- 缓存 Slimefun 规范化物品，减少重复 `getItem().asOne()` 开销。
- `getSFId` 对无 ItemMeta 物品快速返回，减少反序列化成本。
- 更新 Maven 编译插件并显式配置 Lombok 注解处理器。

验证：`./mvnw.cmd -q test` 通过。
