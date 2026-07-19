package io.github.tufkan1.projectex.api.emc;

/** Removable EMC lifecycle subscription. Closing more than once has no effect. */
@FunctionalInterface
public interface EmcSubscription extends AutoCloseable {
    @Override
    void close();
}
