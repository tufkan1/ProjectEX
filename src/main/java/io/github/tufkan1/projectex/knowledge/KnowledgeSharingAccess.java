package io.github.tufkan1.projectex.knowledge;

/** Public registration point for an optional server team boundary. */
public final class KnowledgeSharingAccess {
    private static volatile KnowledgeSharingBoundary boundary = (recipient, owner) -> true;

    private KnowledgeSharingAccess() { }

    public static KnowledgeSharingBoundary boundary() { return boundary; }

    public static void registerBoundary(KnowledgeSharingBoundary replacement) {
        boundary = java.util.Objects.requireNonNull(replacement, "replacement");
    }
}
