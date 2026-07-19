package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.player.PlayerAlchemyState;

/** CAS boundary over one explicitly bound UUID account; no player object is synthesized. */
public interface AutomationAccount {
    PlayerAlchemyState snapshot();

    boolean compareAndSet(PlayerAlchemyState expected, PlayerAlchemyState replacement);
}
