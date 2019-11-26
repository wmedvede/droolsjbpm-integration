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

package org.kie.server.services.taskassigning.runtime;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.jbpm.task.assigning.process.runtime.integration.client.PlanningTask;

@Entity
@Table(name = "PlanningTask")
public class PlanningTaskImpl implements PlanningTask {

    @Id
    @Column(name = "taskId")
    private long taskId;
    private String assignedUser;
    private int index;
    private short published = 0;

    public PlanningTaskImpl() {
    }

    public PlanningTaskImpl(long taskId) {
        this.taskId = taskId;
    }

    public PlanningTaskImpl(long taskId, String assignedUser, int index, boolean published) {
        this.taskId = taskId;
        this.assignedUser = assignedUser;
        this.index = index;
        setPublished(published);
    }

    @Override
    public long getTaskId() {
        return taskId;
    }

    @Override
    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getAssignedUser() {
        return assignedUser;
    }

    @Override
    public void setAssignedUser(String assignedUser) {
        this.assignedUser = assignedUser;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean isPublished() {
        return published == 1;
    }

    @Override
    public void setPublished(boolean published) {
        this.published = (short) (published ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlanningTaskImpl)) {
            return false;
        }
        PlanningTaskImpl that = (PlanningTaskImpl) o;
        return taskId == that.taskId &&
                Objects.equals(assignedUser, that.assignedUser) &&
                index == that.index &&
                published == that.published;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, assignedUser, index, published);
    }
}
