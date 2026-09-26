package dev.skylasliver.expbook.page;

import com.mojang.serialization.Codec;
import dev.skylasliver.expbook.registry.ModItems;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * The five page tiers. A page contributes capacity only and never stores points,
 * which is exactly what keeps page items safely stackable.
 */
public enum PageTier implements StringRepresentable {
    LEATHER("leather", () -> ModItems.LEATHER_PAGE.get()),
    GOLD("gold", () -> ModItems.GOLD_PAGE.get()),
    DIAMOND("diamond", () -> ModItems.DIAMOND_PAGE.get()),
    NETHERITE("netherite", () -> ModItems.NETHERITE_PAGE.get()),
    NETHER_STAR("nether_star", () -> ModItems.NETHER_STAR_PAGE.get());

    public static final Codec<PageTier> CODEC = StringRepresentable.fromEnum(PageTier::values);

    /** Ordinal-based wire form; the enum order is part of the save format. */
    public static final StreamCodec<ByteBuf, PageTier> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(PageTier::fromOrdinal, PageTier::ordinal);

    private static PageTier fromOrdinal(int ordinal) {
        PageTier[] tiers = values();
        if (ordinal < 0 || ordinal >= tiers.length) {
            throw new IllegalArgumentException("Invalid page tier ordinal: " + ordinal);
        }
        return tiers[ordinal];
    }

    private final String id;
    private final Supplier<Item> item;

    PageTier(String id, Supplier<Item> item) {
        this.id = id;
        this.item = item;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    /** Capacity this page adds to a book. */
    public int points() {
        return switch (this) {
            case LEATHER -> dev.skylasliver.expbook.BookConfig.LEATHER.get();
            case GOLD -> dev.skylasliver.expbook.BookConfig.GOLD.get();
            case DIAMOND -> dev.skylasliver.expbook.BookConfig.DIAMOND.get();
            case NETHERITE -> dev.skylasliver.expbook.BookConfig.NETHERITE.get();
            case NETHER_STAR -> dev.skylasliver.expbook.BookConfig.NETHER_STAR.get();
        };
    }

    public Item item() {
        return this.item.get();
    }

    public Component displayName() {
        return Component.translatable("item.skylasliver_expbook." + this.id + "_page");
    }

}
