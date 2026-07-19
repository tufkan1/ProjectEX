package io.github.tufkan1.projectex.knowledge;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC verifier that rejects stale, future, malformed, or tampered sharing payloads. */
public final class KnowledgeSnapshotSigner {
    public static final Duration MAX_LIFETIME = Duration.ofDays(7);
    private final byte[] secret;

    public KnowledgeSnapshotSigner(byte[] secret) {
        if (secret.length < 32) throw new IllegalArgumentException("Knowledge signing secret is too short");
        this.secret = secret.clone();
    }

    public KnowledgeSnapshot create(
        UUID owner, Collection<EmcKey> knowledge, Instant now, Duration lifetime, UUID snapshotId
    ) {
        if (lifetime.isNegative() || lifetime.isZero() || lifetime.compareTo(MAX_LIFETIME) > 0) {
            throw new IllegalArgumentException("Invalid knowledge snapshot lifetime");
        }
        KnowledgeSnapshot unsigned = new KnowledgeSnapshot(
            KnowledgeSnapshot.CURRENT_VERSION, snapshotId, owner, now.getEpochSecond(),
            now.plus(lifetime).getEpochSecond(), List.copyOf(knowledge), new byte[0]
        );
        return unsigned.withSignature(sign(unsigned.canonicalBytes()));
    }

    public Verification verify(KnowledgeSnapshot snapshot, Instant now) {
        if (snapshot.knowledge().size() > KnowledgeSnapshot.MAX_ENTRIES) return Verification.OVERSIZED;
        long epoch = now.getEpochSecond();
        if (snapshot.issuedAt() > epoch + 60) return Verification.FUTURE;
        if (snapshot.expiresAt() <= epoch) return Verification.EXPIRED;
        if (snapshot.expiresAt() - snapshot.issuedAt() > MAX_LIFETIME.toSeconds()) return Verification.EXPIRED;
        byte[] expected = sign(snapshot.withSignature(new byte[0]).canonicalBytes());
        return MessageDigest.isEqual(expected, snapshot.signature()) ? Verification.VALID : Verification.INVALID_SIGNATURE;
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    public enum Verification { VALID, INVALID_SIGNATURE, EXPIRED, FUTURE, OVERSIZED }
}
