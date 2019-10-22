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

package org.jbpm.task.assigning.process.runtime.integration.client;

import java.util.Objects;

public class PotentialOwner {

    private String entityId;
    private boolean user;

    public PotentialOwner(boolean user, String entityId) {
        this.user = user;
        this.entityId = entityId;
    }

    public String getEntityId() {
        return entityId;
    }

    public boolean isUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PotentialOwner)) {
            return false;
        }
        PotentialOwner that = (PotentialOwner) o;
        return user == that.user &&
                Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, user);
    }
}
