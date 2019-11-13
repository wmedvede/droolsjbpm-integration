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

package org.jbpm.task.assigning.user.system.integration.impl;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.task.assigning.user.system.integration.User;
import org.jbpm.task.assigning.user.system.integration.UserSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO move/refactor this class to the proper module/place, etc.
 */
public class WildflyUserSystemService implements UserSystemService {

    private static final String WF_ROLES_FILE = "/roles.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(WildflyUserSystemService.class);

    WildflyUtil.UserGroupInfo userGroupInfo;

    public WildflyUserSystemService() {
        try {
            userGroupInfo = WildflyUtil.buildWildflyUsers(getClass(), WF_ROLES_FILE);
        } catch (Exception e) {
            LOGGER.error("An error was produced during users file loading from resource: " + WF_ROLES_FILE, e);
            userGroupInfo = new WildflyUtil.UserGroupInfo(new ArrayList<>(), new ArrayList<>());
        }
    }

    @Override
    public List<User> findAllUsers() {
        return userGroupInfo.getUsers();
    }

    @Override
    public User findUser(String id) {
        return userGroupInfo.getUsers().stream()
                .filter(user -> user.getId().equals(id))
                .findFirst().orElse(null);
    }
}
