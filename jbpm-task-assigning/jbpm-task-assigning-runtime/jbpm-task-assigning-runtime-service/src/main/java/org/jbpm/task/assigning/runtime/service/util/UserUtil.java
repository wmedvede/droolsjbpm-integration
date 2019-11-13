package org.jbpm.task.assigning.runtime.service.util;

import java.util.HashSet;
import java.util.Set;

import org.jbpm.task.assigning.model.Group;
import org.jbpm.task.assigning.model.User;

public class UserUtil {

    private UserUtil() {
    }

    public static User fromExternalUser(org.jbpm.task.assigning.user.system.integration.User externalUser) {
        final User user = new User(externalUser.getId().hashCode(), externalUser.getId());
        final Set<Group> groups = new HashSet<>();
        user.setGroups(groups);
        if (externalUser.getGroups() != null) {
            externalUser.getGroups().forEach(externalGroup -> groups.add(new Group(externalGroup.getId().hashCode(), externalGroup.getId())));
        }
        return user;
    }
}
