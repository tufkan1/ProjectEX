package example.projectexapi;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.alchemy.WorldTransmutationProtection;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcSubscription;

/** Compile-checked example using only ProjectEX's supported entry point and API packages. */
public final class ProjectEXApiExample {
    private EmcSubscription subscription;

    public void initialize() {
        ProjectEX.emc().find(EmcKey.parse("minecraft:diamond"));
        subscription = ProjectEX.emc().subscribe(snapshot -> rebuildCache(snapshot.revision()));
        WorldTransmutationProtection.EVENT.register(context -> mayBuild(context.player()));
    }

    public void close() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    private static void rebuildCache(long revision) {
        // Consumer cache rebuild belongs here; never mutate the ProjectEX snapshot.
    }

    private static boolean mayBuild(net.minecraft.server.level.ServerPlayer player) {
        // Delegate to the consumer's server-side claim system.
        return player != null;
    }
}
