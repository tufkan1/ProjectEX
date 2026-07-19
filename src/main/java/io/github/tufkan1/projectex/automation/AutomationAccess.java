package io.github.tufkan1.projectex.automation;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/** Immutable owner/member policy; public access is deliberately insert-only. */
public record AutomationAccess(UUID owner, SortedSet<UUID> members, boolean publicInsert) {
    public static final int MAX_MEMBERS = 64;

    public AutomationAccess {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(members, "members");
        if (members.size() > MAX_MEMBERS || members.contains(owner)) {
            throw new IllegalArgumentException("Invalid automation access list");
        }
        members = Collections.unmodifiableSortedSet(new TreeSet<>(members));
    }

    public static AutomationAccess ownedBy(UUID owner) {
        return new AutomationAccess(owner, new TreeSet<>(), false);
    }

    public boolean permits(AutomationAuthority authority, AutomationOperation operation) {
        Objects.requireNonNull(authority, "authority");
        Objects.requireNonNull(operation, "operation");
        if (authority.machineBinding() || authority.operator()) {
            return true;
        }
        if (authority.actor().filter(actor -> actor.equals(owner) || members.contains(actor)).isPresent()) {
            return true;
        }
        return publicInsert && operation == AutomationOperation.INSERT_EMC;
    }

    public AutomationAccess withMember(UUID member, boolean enabled, AutomationAuthority authority) {
        requireOwner(authority);
        Objects.requireNonNull(member, "member");
        if (member.equals(owner)) {
            return this;
        }
        SortedSet<UUID> changed = new TreeSet<>(members);
        if (enabled) {
            changed.add(member);
        } else {
            changed.remove(member);
        }
        return new AutomationAccess(owner, changed, publicInsert);
    }

    public AutomationAccess withPublicInsert(boolean enabled, AutomationAuthority authority) {
        requireOwner(authority);
        return new AutomationAccess(owner, members, enabled);
    }

    private void requireOwner(AutomationAuthority authority) {
        if (!authority.operator() && authority.actor().filter(owner::equals).isEmpty()) {
            throw new SecurityException("Only the online owner or an operator may change automation access");
        }
    }
}
