package io.github.tufkan1.projectex.content.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.automation.AutomationAccess;
import io.github.tufkan1.projectex.automation.EmcLinkFilter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned ownership and filter state persisted by EMC automation blocks and their drops. */
public record AutomationBlockState(
    int version,
    Optional<UUID> owner,
    SortedSet<UUID> members,
    boolean publicInsert,
    EmcLinkFilter insertFilter,
    EmcLinkFilter extractFilter
) {
    public static final int CURRENT_VERSION = 1;
    private static final MapCodec<Optional<UUID>> OWNER_CODEC = Codec.STRING.optionalFieldOf("owner")
        .xmap(value -> value.map(UUID::fromString), value -> value.map(UUID::toString));
    private static final Codec<List<UUID>> MEMBERS_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString)
        .listOf(0, AutomationAccess.MAX_MEMBERS);
    private static final Codec<EmcLinkFilter.Mode> MODE_CODEC = Codec.STRING.xmap(
        EmcLinkFilter.Mode::valueOf, EmcLinkFilter.Mode::name
    );
    private static final Codec<List<EmcKey>> ITEMS_CODEC = Codec.STRING.xmap(EmcKey::parse, EmcKey::toString)
        .listOf(0, 64);
    public static final Codec<AutomationBlockState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.intRange(1, CURRENT_VERSION).fieldOf("version").forGetter(AutomationBlockState::version),
            OWNER_CODEC.forGetter(AutomationBlockState::owner),
            MEMBERS_CODEC.optionalFieldOf("members", List.of())
                .forGetter(state -> List.copyOf(state.members)),
            Codec.BOOL.optionalFieldOf("public_insert", false).forGetter(AutomationBlockState::publicInsert),
            MODE_CODEC.optionalFieldOf("insert_mode", EmcLinkFilter.Mode.DENY_LIST)
                .forGetter(state -> state.insertFilter.mode()),
            ITEMS_CODEC.optionalFieldOf("insert_items", List.of())
                .forGetter(state -> List.copyOf(state.insertFilter.items())),
            MODE_CODEC.optionalFieldOf("extract_mode", EmcLinkFilter.Mode.DENY_LIST)
                .forGetter(state -> state.extractFilter.mode()),
            ITEMS_CODEC.optionalFieldOf("extract_items", List.of())
                .forGetter(state -> List.copyOf(state.extractFilter.items()))
        ).apply(instance, (version, owner, members, publicInsert, insertMode, insertItems,
                           extractMode, extractItems) -> new AutomationBlockState(
            version, owner, uniqueMembers(members), publicInsert,
            new EmcLinkFilter(insertMode, new TreeSet<>(insertItems)),
            new EmcLinkFilter(extractMode, new TreeSet<>(extractItems))
        ))
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AutomationBlockState> STREAM_CODEC =
        StreamCodec.of(AutomationBlockState::write, AutomationBlockState::read);

    public AutomationBlockState {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported automation block state");
        }
        java.util.Objects.requireNonNull(owner, "owner");
        java.util.Objects.requireNonNull(members, "members");
        java.util.Objects.requireNonNull(insertFilter, "insertFilter");
        java.util.Objects.requireNonNull(extractFilter, "extractFilter");
        if (owner.isEmpty() && (!members.isEmpty() || publicInsert)) {
            throw new IllegalArgumentException("Unclaimed automation block cannot grant access");
        }
        if (members.size() > AutomationAccess.MAX_MEMBERS
            || owner.filter(members::contains).isPresent()) {
            throw new IllegalArgumentException("Invalid automation block access list");
        }
        members = Collections.unmodifiableSortedSet(new TreeSet<>(members));
    }

    public static AutomationBlockState empty() {
        return new AutomationBlockState(CURRENT_VERSION, Optional.empty(), new TreeSet<>(), false,
            EmcLinkFilter.allowAll(), EmcLinkFilter.allowAll());
    }

    public AutomationBlockState claim(UUID player) {
        return owner.isPresent() ? this : new AutomationBlockState(
            version, Optional.of(player), members, publicInsert, insertFilter, extractFilter
        );
    }

    public Optional<AutomationAccess> access() {
        return owner.map(value -> new AutomationAccess(value, members, publicInsert));
    }

    public AutomationBlockState withAccess(AutomationAccess access) {
        if (owner.isEmpty() || !owner.orElseThrow().equals(access.owner())) {
            throw new IllegalArgumentException("Automation owner cannot be replaced through access settings");
        }
        return new AutomationBlockState(version, owner, access.members(), access.publicInsert(),
            insertFilter, extractFilter);
    }

    public AutomationBlockState withFilters(EmcLinkFilter insert, EmcLinkFilter extract) {
        return new AutomationBlockState(version, owner, members, publicInsert, insert, extract);
    }

    private static void write(RegistryFriendlyByteBuf buffer, AutomationBlockState state) {
        buffer.writeVarInt(state.version);
        buffer.writeBoolean(state.owner.isPresent());
        state.owner.ifPresent(buffer::writeUUID);
        buffer.writeVarInt(state.members.size());
        state.members.forEach(buffer::writeUUID);
        buffer.writeBoolean(state.publicInsert);
        writeFilter(buffer, state.insertFilter);
        writeFilter(buffer, state.extractFilter);
    }

    private static AutomationBlockState read(RegistryFriendlyByteBuf buffer) {
        int version = buffer.readVarInt();
        Optional<UUID> owner = buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
        int count = buffer.readVarInt();
        if (count < 0 || count > AutomationAccess.MAX_MEMBERS) {
            throw new IllegalArgumentException("Invalid automation member count");
        }
        SortedSet<UUID> members = new TreeSet<>();
        for (int index = 0; index < count; index++) {
            if (!members.add(buffer.readUUID())) {
                throw new IllegalArgumentException("Duplicate automation member");
            }
        }
        boolean publicInsert = buffer.readBoolean();
        return new AutomationBlockState(version, owner, members, publicInsert,
            readFilter(buffer), readFilter(buffer));
    }

    private static void writeFilter(RegistryFriendlyByteBuf buffer, EmcLinkFilter filter) {
        buffer.writeEnum(filter.mode());
        buffer.writeVarInt(filter.items().size());
        filter.items().forEach(item -> buffer.writeUtf(item.toString(), 256));
    }

    private static EmcLinkFilter readFilter(RegistryFriendlyByteBuf buffer) {
        EmcLinkFilter.Mode mode = buffer.readEnum(EmcLinkFilter.Mode.class);
        int count = buffer.readVarInt();
        if (count < 0 || count > 64) throw new IllegalArgumentException("Invalid automation filter size");
        SortedSet<EmcKey> items = new TreeSet<>();
        for (int index = 0; index < count; index++) items.add(EmcKey.parse(buffer.readUtf(256)));
        return new EmcLinkFilter(mode, items);
    }

    private static SortedSet<UUID> uniqueMembers(List<UUID> values) {
        SortedSet<UUID> members = new TreeSet<>(values);
        if (members.size() != values.size()) {
            throw new IllegalArgumentException("Duplicate automation member");
        }
        return members;
    }
}
