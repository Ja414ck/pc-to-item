package com.cobblemon.pctoitem

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.registry.Registry
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

class PCToItem : ModInitializer {

    companion object {
        const val MOD_ID = "pc-to-item"
        val LOGGER = LoggerFactory.getLogger(MOD_ID)
        
        lateinit var PC_SCREEN_HANDLER: ScreenHandlerType<PCScreenHandler>
    }

    override fun onInitialize() {
        LOGGER.info("PC To Item initialized!")
        
        PC_SCREEN_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MOD_ID, "pc_browser"),
            ExtendedScreenHandlerType(::PCScreenHandler, PCScreenData.CODEC)
        )
        
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("pcbrowser")
                    .executes { context ->
                        val player = context.source.player
                        if (player != null) {
                            openPCBrowser(player, 0)
                        }
                        1
                    }
            )
        }
    }
    
    fun openPCBrowser(player: ServerPlayerEntity, boxIndex: Int) {
        val pc = Cobblemon.storage.getPC(player)
        val clampedBox = boxIndex.coerceIn(0, pc.boxes.size - 1)
        
        player.openHandledScreen(PCScreenHandlerFactory(player, clampedBox))
    }
}

data class PCScreenData(val boxIndex: Int) {
    companion object {
        val CODEC: PacketCodec<RegistryByteBuf, PCScreenData> = PacketCodec.tuple(
            PacketCodecs.INTEGER, PCScreenData::boxIndex,
            ::PCScreenData
        )
    }
}

class PCScreenHandlerFactory(
    private val player: ServerPlayerEntity,
    private val boxIndex: Int
) : net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory<PCScreenData> {
    
    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
        return PCScreenHandler(syncId, playerInventory, boxIndex, player as? ServerPlayerEntity)
    }
    
    override fun getDisplayName(): Text {
        return Text.literal("PC Browser")
    }
    
    override fun getScreenOpeningData(player: ServerPlayerEntity): PCScreenData {
        return PCScreenData(boxIndex)
    }
}

class PCScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    initialBoxIndex: Int,
    private val serverPlayer: ServerPlayerEntity?
) : ScreenHandler(PCToItem.PC_SCREEN_HANDLER, syncId) {
    
    private val inventory: SimpleInventory = SimpleInventory(54)
    private var currentBoxIndex: Int = initialBoxIndex
    private var selectedSlot: Int = -1
    private var confirmMode: Boolean = false
    
    constructor(syncId: Int, playerInventory: PlayerInventory, data: PCScreenData) : 
        this(syncId, playerInventory, data.boxIndex, null)
    
    init {
        for (row in 0 until 6) {
            for (col in 0 until 9) {
                val slotIndex = row * 9 + col
                addSlot(PCSlot(inventory, slotIndex, 8 + col * 18, 18 + row * 18))
            }
        }
        
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18))
            }
        }
        
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 198))
        }
        
        if (serverPlayer != null) {
            refreshPCDisplay()
        }
    }
    
    private fun playSound(player: ServerPlayerEntity, sound: net.minecraft.sound.SoundEvent, pitch: Float = 1.0f) {
        player.world.playSound(
            null,
            player.x,
            player.y,
            player.z,
            sound,
            SoundCategory.PLAYERS,
            0.5f,
            pitch
        )
    }
    
    private fun refreshPCDisplay() {
        val player = serverPlayer ?: return
        
        val pc = Cobblemon.storage.getPC(player)
        if (currentBoxIndex >= pc.boxes.size) return
        
        val box = pc.boxes[currentBoxIndex]
        
        for (i in 0 until 54) {
            inventory.setStack(i, ItemStack.EMPTY)
        }
        
        for (pcSlot in 0 until 30) {
            val pokemon = box.get(pcSlot)
            val guiRow = pcSlot / 6
            val guiCol = pcSlot % 6
            val guiSlot = guiRow * 9 + guiCol
            
            if (pokemon != null) {
                val displayStack = createPokemonDisplayItem(pokemon, pcSlot, player)
                inventory.setStack(guiSlot, displayStack)
            }
        }
        
        if (currentBoxIndex > 0) {
            val prevBox = ItemStack(Items.ARROW)
            prevBox.set(DataComponentTypes.CUSTOM_NAME, Text.literal("<< Prev Box").formatted(Formatting.YELLOW))
            inventory.setStack(45, prevBox)
        }
        
        if (currentBoxIndex < pc.boxes.size - 1) {
            val nextBox = ItemStack(Items.ARROW)
            nextBox.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Next Box >>").formatted(Formatting.YELLOW))
            inventory.setStack(53, nextBox)
        }
        
        val boxInfo = ItemStack(Items.BOOK)
        boxInfo.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Box ${currentBoxIndex + 1}").formatted(Formatting.GOLD, Formatting.BOLD))
        inventory.setStack(8, boxInfo)
        
        val closeBtn = ItemStack(Items.BARRIER)
        closeBtn.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Close").formatted(Formatting.RED))
        inventory.setStack(49, closeBtn)
        
        if (selectedSlot >= 0 && confirmMode) {
            val cancelBtn = ItemStack(Items.RED_STAINED_GLASS_PANE)
            cancelBtn.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Cancel").formatted(Formatting.RED))
            inventory.setStack(46, cancelBtn)
            
            val confirmBtn = ItemStack(Items.LIME_STAINED_GLASS_PANE)
            confirmBtn.set(DataComponentTypes.CUSTOM_NAME, Text.literal("CONFIRM EXTRACT").formatted(Formatting.GREEN, Formatting.BOLD))
            inventory.setStack(52, confirmBtn)
        }
        
        sendContentUpdates()
    }
    
    private fun createPokemonDisplayItem(pokemon: Pokemon, pcSlot: Int, player: ServerPlayerEntity): ItemStack {
        val stack = PokemonItem.from(pokemon)
        
        val shinyPrefix = if (pokemon.shiny) "§e★ " else ""
        val name = Text.literal("${shinyPrefix}${pokemon.species.name}")
            .append(Text.literal(" Lv.${pokemon.level}").formatted(Formatting.GRAY))
        stack.set(DataComponentTypes.CUSTOM_NAME, name)
        
        return stack
    }
    
    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (player !is ServerPlayerEntity) return
        if (slotIndex < 0 || slotIndex >= 54) return
        
        val pc = Cobblemon.storage.getPC(player)
        
        // Prev box (slot 45)
        if (slotIndex == 45 && currentBoxIndex > 0 && !confirmMode) {
            currentBoxIndex--
            playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 1.0f)
            refreshPCDisplay()
            return
        }
        
        // Next box (slot 53)
        if (slotIndex == 53 && currentBoxIndex < pc.boxes.size - 1 && !confirmMode) {
            currentBoxIndex++
            playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 1.0f)
            refreshPCDisplay()
            return
        }
        
        // Close (slot 49)
        if (slotIndex == 49 && !confirmMode) {
            playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.8f)
            player.closeHandledScreen()
            return
        }
        
        // Cancel (slot 46)
        if (slotIndex == 46 && confirmMode) {
            selectedSlot = -1
            confirmMode = false
            playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.8f)
            refreshPCDisplay()
            return
        }
        
        // Confirm extract (slot 52)
        if (slotIndex == 52 && confirmMode && selectedSlot >= 0) {
            extractPokemon(player, selectedSlot)
            selectedSlot = -1
            confirmMode = false
            refreshPCDisplay()
            return
        }
        
        // Pokemon slot click
        val row = slotIndex / 9
        val col = slotIndex % 9
        if (row < 5 && col < 6) {
            val pcSlot = row * 6 + col
            val box = pc.boxes[currentBoxIndex]
            val pokemon = box.get(pcSlot)
            
            if (pokemon != null) {
                selectedSlot = pcSlot
                confirmMode = true
                
                // Play select sound
                playSound(player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f)
                
                player.sendMessage(
                    Text.literal("Selected ${pokemon.species.name}! Click ")
                        .formatted(Formatting.YELLOW)
                        .append(Text.literal("CONFIRM").formatted(Formatting.GREEN, Formatting.BOLD))
                        .append(Text.literal(" to extract.").formatted(Formatting.YELLOW)),
                    true
                )
                
                refreshPCDisplay()
            }
        }
    }
    
    private fun extractPokemon(player: ServerPlayerEntity, pcSlot: Int) {
        val pc = Cobblemon.storage.getPC(player)
        val box = pc.boxes[currentBoxIndex]
        val pokemon = box.get(pcSlot) ?: return
        
        if (player.inventory.emptySlot == -1) {
            // Error sound - inventory full
            playSound(player, SoundEvents.ENTITY_VILLAGER_NO, 1.0f)
            player.sendMessage(Text.literal("Your inventory is full!").formatted(Formatting.RED), false)
            return
        }
        
        val itemStack = PokemonItem.from(pokemon)
        
        val pokemonNbt = NbtCompound()
        pokemon.saveToNBT(player.registryManager, pokemonNbt)
        
        val existingData = itemStack.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt() ?: NbtCompound()
        existingData.put("PTI_NBT", pokemonNbt)
        itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(existingData))
        
        val shinyPrefix = if (pokemon.shiny) "§e★ " else ""
        itemStack.set(DataComponentTypes.CUSTOM_NAME, 
            Text.literal("${shinyPrefix}${pokemon.species.name} (Lv.${pokemon.level})")
        )
        
        box.set(pcSlot, null)
        player.inventory.insertStack(itemStack)
        
        // Success sound
        playSound(player, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f)
        
        player.sendMessage(
            Text.literal("Extracted ${pokemon.species.name} to item!").formatted(Formatting.GREEN),
            false
        )
        
        PCToItem.LOGGER.info("Player ${player.name.string} extracted ${pokemon.species.name} from PC Box ${currentBoxIndex + 1}, Slot ${pcSlot + 1}")
    }
    
    override fun quickMove(player: PlayerEntity, slot: Int): ItemStack {
        return ItemStack.EMPTY
    }
    
    override fun canUse(player: PlayerEntity): Boolean {
        return true
    }
}

class PCSlot(inventory: Inventory, index: Int, x: Int, y: Int) : Slot(inventory, index, x, y) {
    override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
    override fun canInsert(stack: ItemStack): Boolean = false
}
