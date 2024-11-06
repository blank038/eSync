package com.aiyostudio.esync.internal.view

import com.aiyostudio.esync.internal.module.entity.PlayerInventoryEntity
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.util.TextUtil.colorify
import com.aiyostudio.esync.internal.util.TextUtil.getColorifyStringList
import com.aystudio.core.bukkit.util.common.CommonUtil
import com.aystudio.core.bukkit.util.inventory.GuiModel
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class InventoryView(
    val player: Player,
    val entity: PlayerInventoryEntity
) {
    private lateinit var gui: GuiModel

    init {
        EfficientSyncBukkit.instance.saveResource("view/inventory.yml", "view/inventory.yml", false) {
            val data = YamlConfiguration.loadConfiguration(it)
            this.gui = GuiModel(data.getString("title"), data.getInt("size"))
            this.gui.registerListener(EfficientSyncBukkit.instance)
            data.getConfigurationSection("items")?.getKeys(false)?.forEach { key ->
                val section = data.getConfigurationSection("items.$key")
                val item = ItemStack(
                    Material.valueOf(section.getString("type").uppercase()),
                    section.getInt("amount"),
                    section.getInt("data").toShort()
                )
                val meta = item.itemMeta
                meta.displayName = section.getString("name").colorify()
                meta.lore = section.getColorifyStringList("lore")
                item.setItemMeta(meta)
                CommonUtil.formatSlots(section.getString("slot")).forEach { slot -> this.gui.setItem(slot, item) }
            }
            val contentSlots = CommonUtil.formatSlots(data.getString("contents"))
            (0 until 36).forEach { slot ->
                if (slot < contentSlots.size && entity.inventory.containsKey(slot)) {
                    this.gui.setItem(contentSlots[slot], entity.inventory.get(slot))
                }
            }
            val armorSlots = CommonUtil.formatSlots(data.getString("armor"))
            (36 until 41).forEach { slot ->
                val finalIndex = slot - 36
                if (finalIndex < armorSlots.size && entity.inventory.containsKey(slot)) {
                    this.gui.setItem(armorSlots[finalIndex], entity.inventory.get(slot))
                }
            }
            this.gui.execute { it.isCancelled = true }
            this.gui.openInventory(player)
        }
    }
}