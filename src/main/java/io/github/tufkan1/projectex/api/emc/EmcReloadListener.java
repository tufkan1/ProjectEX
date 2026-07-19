package io.github.tufkan1.projectex.api.emc;

/** Receives an immutable snapshot after a successful atomic publication. */
@FunctionalInterface
public interface EmcReloadListener {
    void onReload(EmcSnapshot snapshot);
}
