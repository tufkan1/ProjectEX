package io.github.tufkan1.projectex.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class KnowledgeSharingConfigTest {
    @AfterEach void clear() {
        System.clearProperty(KnowledgeSharingConfig.POLICY);
        System.clearProperty(KnowledgeSharingConfig.LIFETIME_HOURS);
        KnowledgeSharingConfig.reload();
    }

    @Test void acceptsCreativeOnlyAndBoundedLifetime() {
        System.setProperty(KnowledgeSharingConfig.POLICY, "creative_only");
        System.setProperty(KnowledgeSharingConfig.LIFETIME_HOURS, "168");
        var snapshot = KnowledgeSharingConfig.load();
        assertEquals(KnowledgeShareWorkflow.SharingPolicy.CREATIVE_ONLY, snapshot.policy());
        assertEquals(Duration.ofDays(7), snapshot.lifetime());
    }

    @Test void rejectsUnknownPolicyAndUnsafeLifetime() {
        System.setProperty(KnowledgeSharingConfig.POLICY, "everyone");
        assertThrows(IllegalArgumentException.class, KnowledgeSharingConfig::load);
        System.setProperty(KnowledgeSharingConfig.POLICY, "enabled");
        System.setProperty(KnowledgeSharingConfig.LIFETIME_HOURS, "169");
        assertThrows(IllegalArgumentException.class, KnowledgeSharingConfig::load);
    }
}
