/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.services.taskassigning.user.system.simple;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

import org.kie.server.services.taskassigning.user.system.api.Group;
import org.kie.server.services.taskassigning.user.system.api.User;

public class WildflyUtil {

    public static UserGroupInfo buildWildflyUsers(Class clazz, String resource) throws IOException {
        try (InputStream input = clazz.getResourceAsStream(resource)) {
            return buildWildflyUsers(input);
        }
    }

    /**
     * Reads a Wildfly roles.properties configuration file and extracts the user definitions and the corresponding
     * groups. Can be useful when we start processing jBPM runtime tasks and we want to build the users configuration.
     * @param input InputStream with the user + roles file in the WF format.
     * @return a UserGroupInfo instance with the Users and Groups loaded.
     * @throws IOException
     */
    public static UserGroupInfo buildWildflyUsers(InputStream input) throws IOException {
        final int[] userIds = {0};
        final int[] groupIds = {0};
        final List<User> users = new ArrayList<>();
        final List<Group> groups = new ArrayList<>();
        final Map<String, Group> groupMap = new HashMap<>();

        List<String> lines = IOUtils.readLines(input, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.startsWith("#"))
                .collect(Collectors.toList());
        lines.forEach(line -> {
            String[] lineSplit = line.split("=");
            String userLogin = lineSplit[0];
            Set<Group> userGroups = new HashSet<>();
            String encodedGroups = lineSplit[1];
            Stream.of(encodedGroups.split(",")).forEach(groupName -> {
                Group group = groupMap.get(groupName);
                if (group == null) {
                    group = new GroupImpl(groupName);
                    groupMap.put(groupName, group);
                    groups.add(group);
                }
                userGroups.add(group);
            });
            User user = new UserImpl(userLogin, userGroups);
            users.add(user);
        });
        return new UserGroupInfo(users, groups);
    }

    public static class UserGroupInfo {

        private List<User> users;
        private List<Group> groups;

        UserGroupInfo(List<User> users, List<Group> groups) {
            this.users = users;
            this.groups = groups;
        }

        public List<User> getUsers() {
            return users;
        }

        public List<Group> getGroups() {
            return groups;
        }
    }
}
