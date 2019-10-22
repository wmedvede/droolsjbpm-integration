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

package org.jbpm.task.assigning.runtime.service;

import java.util.HashSet;
import java.util.Set;

/**
 * Convenient repository for keeping track published tasks during runtime. Used for optimization purposes.
 */
public class PublishedTaskCache implements Cloneable {

    private Set<Long> publishedTasks = new HashSet<>();

    public void put(Long taskId) {
        publishedTasks.add(taskId);
    }

    public boolean isPublished(Long taskId) {
        return publishedTasks.contains(taskId);
    }

    public Object clone() {
        try {
            final PublishedTaskCache clone = (PublishedTaskCache) super.clone();
            clone.publishedTasks = new HashSet<>(this.publishedTasks);
            return clone;
        } catch (CloneNotSupportedException e) {
            //will never happen.
            throw new RuntimeException("Failed to clone PublishedTaskCache", e);
        }
    }
}
