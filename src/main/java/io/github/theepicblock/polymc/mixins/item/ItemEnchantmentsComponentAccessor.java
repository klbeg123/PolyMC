package io.github.theepicblock.polymc.mixins.item;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemEnchantmentsComponent.class)
public interface ItemEnchantmentsComponentAccessor {
    @Invoker("<init>")
    static ItemEnchantmentsComponent createItemEnchantmentsComponent(Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments, boolean showInTooltip) {
        throw new UnsupportedOperationException();
    }

    @Accessor
    Object2IntOpenHashMap<RegistryEntry<Enchantment>> getEnchantments();

    @Accessor
    boolean isShowInTooltip();
}