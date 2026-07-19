package io.github.tufkan1.projectex.api.endgame;

/** One server-bound Final Star lease with an atomic shared-cooldown activation. */
public interface FinalStarCapability {
    int VERSION = 1;
    FinalStarSlot slot();
    int cooldownTicks();
    boolean ready();
    boolean activate();
}
