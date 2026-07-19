package io.github.tufkan1.projectex.knowledge;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Integration boundary for team/claim mods; the default permits cross-player sharing. */
@FunctionalInterface
public interface KnowledgeSharingBoundary {
    boolean permits(ServerPlayer recipient, UUID snapshotOwner);
}
