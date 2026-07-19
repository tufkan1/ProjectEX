package io.github.tufkan1.projectex.alchemy;

import io.github.tufkan1.projectex.api.emc.EmcMatch;
import java.util.Objects;

/** Client intent only: no request type carries a client-supplied EMC amount. */
public sealed interface AlchemyTransaction permits
    AlchemyTransaction.Learn, AlchemyTransaction.Burn, AlchemyTransaction.Create {

    EmcMatch item();

    long emcRevision();

    record Learn(EmcMatch item, long emcRevision) implements AlchemyTransaction {
        public Learn {
            Objects.requireNonNull(item, "item");
        }
    }

    record Burn(EmcMatch item, int count, long emcRevision) implements AlchemyTransaction {
        public Burn {
            Objects.requireNonNull(item, "item");
        }
    }

    record Create(EmcMatch item, int count, long emcRevision) implements AlchemyTransaction {
        public Create {
            Objects.requireNonNull(item, "item");
        }
    }
}
