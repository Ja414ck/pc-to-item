package com.cobblemon.pctoitem

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class PCToItemClient : ClientModInitializer {
    
    override fun onInitializeClient() {
        HandledScreens.register(PCToItem.PC_SCREEN_HANDLER, ::PCBrowserScreen)
    }
}

class PCBrowserScreen(
    handler: PCScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : HandledScreen<PCScreenHandler>(handler, inventory, title) {
    
    companion object {
        val TEXTURE: Identifier = Identifier.of("minecraft", "textures/gui/container/generic_54.png")
    }
    
    init {
        backgroundHeight = 222
        playerInventoryTitleY = backgroundHeight - 94
    }
    
    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        val x = (width - backgroundWidth) / 2
        val y = (height - backgroundHeight) / 2
        
        // Draw chest-like background
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, 6 * 18 + 17)
        context.drawTexture(TEXTURE, x, y + 6 * 18 + 17, 0, 126, backgroundWidth, 96)
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }
}
