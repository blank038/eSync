package com.aiyostudio.esync.internal.view

import com.aiyostudio.esync.internal.module.entity.EnderChestEntity
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.util.TextUtil.colorify
import com.aiyostudio.esync.internal.util.TextUtil.getColorifyStringList
import com.aystudio.core.bukkit.util.common.CommonUtil
import com.aystudio.core.bukkit.util.inventory.GuiModel
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class EnderChestView(
    val player: Player,
    val entity: EnderChestEntity
) {
    private lateinit var gui: GuiModel

    init {
        EfficientSyncBukkit.instance.saveResource("view/enderChest.yml", "view/enderChest.yml", false) {
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
            contentSlots.forEach { slot -> slot.takeIf { it < 27 }?.let { this.gui.setItem(it, entity.items.get(it)) } }
            this.gui.execute { it.isCancelled = true }
            this.gui.openInventory(player)
        }
    }
}