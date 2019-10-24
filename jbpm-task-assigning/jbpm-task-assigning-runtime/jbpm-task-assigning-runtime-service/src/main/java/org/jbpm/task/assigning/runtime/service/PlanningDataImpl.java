package org.jbpm.task.assigning.runtime.service;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.jbpm.task.assigning.process.runtime.integration.client.PlanningData;

@Entity
@Table(name = "PlanningData")
public class PlanningDataImpl implements PlanningData {

    @Id
    @Column(name = "taskId")
    private long taskId;
    private String assignedUser;
    private int index;
    private boolean pinned;
    private boolean published;

    public PlanningDataImpl() {
    }

    public PlanningDataImpl(long taskId) {
        this.taskId = taskId;
    }

    public PlanningDataImpl(long taskId, String assignedUser, int index, boolean pinned, boolean published) {
        this.taskId = taskId;
        this.assignedUser = assignedUser;
        this.index = index;
        this.pinned = pinned;
        this.published = published;
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
    public boolean isPinned() {
        return pinned;
    }

    @Override
    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    @Override
    public boolean isPublished() {
        return published;
    }

    @Override
    public void setPublished(boolean published) {
        this.published = published;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlanningDataImpl)) {
            return false;
        }
        PlanningDataImpl that = (PlanningDataImpl) o;
        return taskId == that.taskId &&
                index == that.index &&
                pinned == that.pinned &&
                published == that.published &&
                Objects.equals(assignedUser, that.assignedUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, assignedUser, index, pinned, published);
    }
}
