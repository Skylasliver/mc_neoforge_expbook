package dev.skylasliver.expbook.registry;

import com.mojang.serialization.Codec;
import dev.skylasliver.expbook.ExperienceBookMod;
import dev.skylasliver.expbook.component.BookContents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ExperienceBookMod.MOD_ID);

    /** Points and pages of an experience book, as one atomic value. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BookContents>> BOOK_CONTENTS =
            COMPONENTS.register("book_contents", () -> DataComponentType.<BookContents>builder()
                    .persistent(BookContents.CODEC)
                    .networkSynchronized(BookContents.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    /** Fill-level index 0..3, only used to drive the item model override. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FILL_LEVEL =
            COMPONENTS.register("fill_level", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AUTOMATION =
            COMPONENTS.register("automation", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 7)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TARGET_LEVEL =
            COMPONENTS.register("target_level", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 1000)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> REPAIR_CREDIT =
            COMPONENTS.register("repair_credit", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private ModComponents() {
    }
}
