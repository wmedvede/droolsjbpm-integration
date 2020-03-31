/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.services.taskassigning.core.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("TaUser")
public class User extends TaskOrUser implements OrganizationalEntity {

    private String entityId;
    private boolean enabled;
    private Set<Group> groups = new HashSet<>();
    private Map<String, Set<Object>> labelValues = new HashMap<>();
    private Map<String, Object> attributes;

    public User() {
    }

    public User(long id, String entityId) {
        super(id);
        this.entityId = entityId;
    }

    public User(long id, String entityId, boolean enabled) {
        super(id);
        this.entityId = entityId;
        this.enabled = enabled;
    }

    @Override
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public Set<Object> getLabelValues(String labelName) {
        return labelValues.get(labelName);
    }

    public void setLabelValues(String labelName, Set<Object> values) {
        labelValues.put(labelName, values);
    }

    public Map<String, Set<Object>> getAllLabelValues() {
        return labelValues;
    }

    public void setAllLabelValues(Map<String, Set<Object>> labelValues) {
        this.labelValues = labelValues;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public User getUser() {
        return this;
    }

    @Override
    public boolean isUser() {
        return true;
    }

    @Override
    public Integer getEndTimeInMinutes() {
        return 0;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", enabled='" + enabled + '\'' +
                ", entityId='" + entityId + '\'' +
                ", groups=" + groups +
                ", labelValues=" + labelValues +
                ", attributes=" + attributes +
                '}';
    }
}
