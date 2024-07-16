package nl.theepicblock.polymc.testmod.automated;

import io.github.theepicblock.polymc.impl.NOPPolyMap;
import io.github.theepicblock.polymc.impl.PolyMapImpl;
import io.github.theepicblock.polymc.mixins.wizards.ItemEntityAccessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.test.CustomTestProvider;
import net.minecraft.test.TestFunction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import nl.theepicblock.polymc.testmod.Testmod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class ItemEncodeTests implements FabricGameTest {
    @CustomTestProvider
    public Collection<TestFunction> testItem() {
        var list = new ArrayList<TestFunction>();
        // Different ways in which we can test itemstacks being transformed by PolyMc
        var reserializationMethods = new HashMap<String, ReserializationMethod>();
        reserializationMethods.put("reencode", this::reencodeMethod);
        reserializationMethods.put("item entity", this::itemEntityMethod);

        var i = 0;
        for (var isItemVanilla : new boolean[]{true, false}) {
            for (var useNopMap : new Boolean[]{false, true}) {
                for (var method : reserializationMethods.entrySet()) {
                    var item = isItemVanilla ? Items.STICK : Testmod.TEST_ITEM;
                    list.add(new TestFunction(
                            "itembatch_"+i++,
                            String.format("itemtests (%s, %s, %s)", item.getTranslationKey(), useNopMap, method.getKey()),
                            EMPTY_STRUCTURE,
                            100,
                            0,
                            true,
                            (ctx) -> {
                                // The actual test function
                                var packetCtx = new PacketTester(ctx);
                                packetCtx.setGameMode(GameMode.CREATIVE);
                                if (useNopMap) {
                                    packetCtx.setMap(new NOPPolyMap());
                                }

                                var originalStack = new ItemStack(item);
                                originalStack.setCount(5);
                                var copyOfOriginal = originalStack.copy();

                                var newStack = method.getValue().reserialize(originalStack, packetCtx);

                                if (isItemVanilla || useNopMap) {
                                    ctx.assertTrue(newStack.getItem() == originalStack.getItem(), "Item shouldn't have been transformed by PolyMc. Result: "+newStack);
                                    ctx.assertFalse(newStack.contains(DataComponentTypes.CUSTOM_DATA), "PolyMc shouldn't add data when it's not transforming");
                                    ctx.assertTrue(ItemStack.areItemsAndComponentsEqual(newStack, originalStack), "PolyMc should not influence stacks in any way if it's not transforming");
                                } else {
                                    ctx.assertTrue(newStack.getItem() != originalStack.getItem(), "Item should've been transformed by PolyMc. Result: "+newStack);
                                }
                                ctx.assertTrue(newStack.getCount() == 5, "PolyMc shouldn't affect itemcount");
                                ctx.assertTrue(ItemStack.areItemsAndComponentsEqual(originalStack, copyOfOriginal), "PolyMc shouldn't affect the original item");

                                ctx.assertTrue(ItemStack.areItemsAndComponentsEqual(originalStack, PolyMapImpl.recoverOriginalItem(newStack)),
                                        "Item should survive round-trip (when player is in creative mode)");

                                // Create a new polyd stack with a different count
                                if (method.getKey().equals("reencode")) {
                                    originalStack.setCount(7);
                                    var secondStack = method.getValue().reserialize(originalStack, packetCtx);
                                    ctx.assertTrue(ItemStack.areItemsAndComponentsEqual(newStack, secondStack), "The same item with different counts should be stackable");
                                }

                                packetCtx.close();
                                ctx.complete();
                            }
                    ));
                }
            }
        }

        return list;
    }

    public ItemStack reencodeMethod(ItemStack stack, PacketTester ctx) {
        return ctx.reencode(new ScreenHandlerSlotUpdateS2CPacket(0,0,0, stack)).getStack();
    }

    public ItemStack itemEntityMethod(ItemStack stack, PacketTester ctx) {
        var coords = ctx.getTestContext().getAbsolute(new Vec3d(0,0,0));
        var entity = new ItemEntity(ctx.getTestContext().getWorld(), coords.x, coords.y, coords.z, stack);

        var trackerPackets = ctx.captureAll(() -> {
            ctx.getTestContext().getWorld().spawnEntity(entity);
            ctx.getTestContext().getWorld().tick(() -> false); // Tick the world so packets are sent
        });

        var entry = trackerPackets.stream()
                .filter(p -> p instanceof EntityTrackerUpdateS2CPacket)
                .map(p -> (EntityTrackerUpdateS2CPacket)p)
                .filter(p -> p.id() == entity.getId())
                .flatMap(p -> p.trackedValues().stream())
                .filter(p -> p.id() == ItemEntityAccessor.getStackTracker().id())
                .findAny();

        return (ItemStack)(entry.orElseThrow().value());
    }

    public interface ReserializationMethod {
        ItemStack reserialize(ItemStack stack, PacketTester ctx);
    }
}
