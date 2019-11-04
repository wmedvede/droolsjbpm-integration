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
    private short published = 0;
    private short detached = 0;

    public PlanningDataImpl() {
    }

    public PlanningDataImpl(long taskId) {
        this.taskId = taskId;
    }

    public PlanningDataImpl(long taskId, String assignedUser, int index, boolean published, boolean detached) {
        this.taskId = taskId;
        this.assignedUser = assignedUser;
        this.index = index;
        setPublished(published);
        setDetached(detached);
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
    public boolean isDetached() {
        return detached == 1;
    }

    @Override
    public void setDetached(boolean detached) {
        this.detached = (short) (detached ? 1 : 0);
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
                Objects.equals(assignedUser, that.assignedUser) &&
                index == that.index &&
                published == that.published &&
                detached == that.detached;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, assignedUser, index, published, detached);
    }
}
