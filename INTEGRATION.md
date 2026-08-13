# InfiniteInvo Integrations

## 背包整理模组

InfiniteInvo 为整理模组提供公共 API，不需要依赖或复制 Inventory Profiles Next 的专用兼容代码。

整理器应在每次准备整理时调用：

```java
var view = InfiniteInvoSortingApi.findView(player, menu);
if (view.isPresent() && !InfiniteInvoClientSortingApi.isPageChangePending(menu)) {
    for (InfiniteInvoSortingSlot slot : view.get().visibleStorageSlots()) {
        if (!slot.isSortable()) {
            continue;
        }
        // slot.menuSlot() 是当前菜单点击使用的槽位编号。
        // slot.storageSlot() 是稳定的 InfiniteInvo 存储索引。
    }
}
```

规则：

- 仅整理 `visibleStorageSlots()` 返回的槽位，不要包含合成、结果、盔甲、副手或快捷栏。
- 跳过 `isSortable()` 为 `false` 的锁定槽位。
- 分页切换期间 `isPageChangePending(menu)` 为 `true`，此时不要读取槽位或发送点击。
- 不要跨帧、跨页面缓存 `menuSlot()` 或 `storageSlot()` 映射。
- 使用原版菜单点击和服务端确认流程；不要直接替换、重排或完整回写 `Inventory.items`。

API 不要求整理模组成为硬依赖。仅在 InfiniteInvo 已加载时，以可选依赖方式调用即可。
