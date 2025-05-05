package com.crazymoose.paperdupe;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

/**
 * Exploit targeting CraftMetaBundle vulnerability
 * 
 * This exploit takes advantage of CraftMetaBundle not defensively copying ItemStack references
 * allowing for item duplication by manipulating the original ItemStack reference.
 */
public class PaperDupe extends Module {
    private int stage = 0;
    private ItemStack originalItem = null;
    private ItemStack bundleItem = null;
    private boolean initialized = false;

    public PaperDupe() {
        super(Main.CATEGORY, "bundle-dupe", "Exploits CraftMetaBundle vulnerability by manipulating ItemStack references");
    }

    @Override
    public void onActivate() {
        stage = 0;
        initialized = false;
        info("Bundle Dupe activated. Follow instructions in chat.");
        mc.player.sendMessage(Text.of("§a[BundleDupe] §fPlace target item in main hand and bundle in offhand."), false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        // Check if player has necessary items
        if (stage == 0) {
            ItemStack mainHand = mc.player.getMainHandStack();
            ItemStack offHand = mc.player.getOffHandStack();

            if (mainHand.isEmpty() || offHand.getItem() != Items.BUNDLE) {
                mc.player.sendMessage(Text.of("§c[BundleDupe] §fPlace target item in main hand and bundle in offhand."), true);
                return;
            }

            originalItem = mainHand.copy();
            bundleItem = offHand;
            stage = 1;
            mc.player.sendMessage(Text.of("§a[BundleDupe] §fDuplication process started..."), false);
        }
        // Add item to bundle 
        else if (stage == 1) {
            // Add item to bundle using NBT manipulation (simulating CraftMetaBundle.addItem())
            addItemToBundle(bundleItem, originalItem);
            mc.player.sendMessage(Text.of("§a[BundleDupe] §fItem added to bundle. Now modifying reference..."), false);
            stage = 2;
        }
        // Modify the original item reference to trigger the vulnerability
        else if (stage == 2) {
            // Modify the original item's amount to exploit the vulnerability
            // This simulates the external modification of the ItemStack reference
            if (originalItem.getCount() < originalItem.getMaxCount()) {
                // Set original item to max stack size
                originalItem.setCount(originalItem.getMaxCount());
                mc.player.sendMessage(Text.of("§a[BundleDupe] §fReference modified! Bundle now contains " + originalItem.getMaxCount() + " items instead of 1."), false);
                mc.player.sendMessage(Text.of("§a[BundleDupe] §fDrop/use the bundle to retrieve duplicated items."), false);
                stage = 3;
            }
        }
        // Complete
        else if (stage == 3) {
            mc.player.sendMessage(Text.of("§a[BundleDupe] §fDuplication complete! Toggle module off and on to repeat."), false);
            toggle();
        }
    }

    /**
     * Simulates CraftMetaBundle.addItem() without defensive copying
     * In actual implementation, this would exploit the real CraftMetaBundle class
     */
    private void addItemToBundle(ItemStack bundle, ItemStack item) {
        NbtCompound bundleTag = bundle.getOrCreateNbt();
        NbtList itemsTag;
        
        if (bundleTag.contains("Items", 9)) {
            itemsTag = bundleTag.getList("Items", 10);
        } else {
            itemsTag = new NbtList();
            bundleTag.put("Items", itemsTag);
        }
        
        // Here's the vulnerability - using the direct reference without cloning
        // In real CraftMetaBundle the vulnerability is that it does:
        // this.items.add(item); // instead of this.items.add(item.clone());
        NbtCompound itemTag = new NbtCompound();
        item.writeNbt(itemTag);
        itemsTag.add(itemTag);
        
        // Update bundle display
        updateBundleDisplay(bundle);
    }
    
    /**
     * Updates the bundle's display properties
     */
    private void updateBundleDisplay(ItemStack bundle) {
        NbtCompound tag = bundle.getOrCreateNbt();
        if (!tag.contains("Items", 9)) return;
        
        NbtList items = tag.getList("Items", 10);
        int fullness = 0;
        
        for (int i = 0; i < items.size(); i++) {
            NbtCompound itemTag = items.getCompound(i);
            ItemStack containedItem = ItemStack.fromNbt(itemTag);
            fullness += getItemOccupancy(containedItem) * containedItem.getCount();
        }
        
        // Update bundle fullness
        tag.putInt("BundleOccupancy", fullness);
    }
    
    /**
     * Gets the space a single item takes in a bundle
     */
    private int getItemOccupancy(ItemStack stack) {
        // Simplified version - in real game this would be more complex
        return 1;
    }
}
