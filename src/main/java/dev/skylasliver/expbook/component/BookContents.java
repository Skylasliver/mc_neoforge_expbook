package dev.skylasliver.expbook.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.skylasliver.expbook.page.PageTier;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Authoritative contents of an experience book. Points live in one shared pool;
 * pages only add capacity, so removing a page can strand points above the new
 * capacity and must be validated before it commits.
 */
public record BookContents(int storedPoints, List<PageTier> pages) {

    /** Maximum pages a book can hold: a 3x9 grid in the page screen. */
    public static final int MAX_PAGES = 27;

    public static final BookContents EMPTY = new BookContents(0, List.of());

    public static final Codec<BookContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("stored_points", 0).forGetter(BookContents::storedPoints),
            PageTier.CODEC.listOf().optionalFieldOf("pages", List.of()).forGetter(BookContents::pages)
    ).apply(instance, BookContents::new));

    /**
     * Declared over {@link ByteBuf} so it stays usable both for component sync and for
     * menu payloads, which hand in a {@code RegistryFriendlyByteBuf}.
     */
    public static final StreamCodec<ByteBuf, BookContents> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BookContents::storedPoints,
                    PageTier.STREAM_CODEC.apply(ByteBufCodecs.list()), BookContents::pages,
                    BookContents::new);

    public BookContents {
        storedPoints = Math.max(0, storedPoints);
        pages = List.copyOf(pages.size() > MAX_PAGES ? pages.subList(0, MAX_PAGES) : pages);
    }

    /** Total capacity contributed by the inserted pages. */
    public int capacity() {
        int total = 0;
        for (PageTier tier : this.pages) {
            total += tier.points();
        }
        return total;
    }

    public boolean isEmpty() {
        return this.storedPoints == 0 && this.pages.isEmpty();
    }

    public BookContents withStoredPoints(int points) {
        return new BookContents(points, this.pages);
    }

    public BookContents withPages(List<PageTier> newPages) {
        return new BookContents(this.storedPoints, newPages);
    }

    public BookContents withPageAdded(PageTier tier) {
        if (this.pages.size() >= MAX_PAGES) {
            return this;
        }
        List<PageTier> next = new ArrayList<>(this.pages);
        next.add(tier);
        return withPages(next);
    }

    /**
     * @return contents with the page at {@code index} removed, or {@code null} when the
     *         removal would leave stored points above the resulting capacity.
     */
    public BookContents withPageRemoved(int index) {
        if (index < 0 || index >= this.pages.size()) {
            return null;
        }
        List<PageTier> next = new ArrayList<>(this.pages);
        next.remove(index);
        BookContents candidate = withPages(next);
        if (candidate.capacity() < candidate.storedPoints()) {
            return null;
        }
        return candidate;
    }
}
