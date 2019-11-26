package org.kie.server.api.model.taskAssigning;

import java.time.LocalDateTime;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import org.kie.internal.jaxb.LocalDateTimeXmlAdapter;
import org.kie.soup.commons.xstream.LocalDateTimeXStreamConverter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "task-summary")
public class TaskAssigningSummary {

    @XmlElement(name = "task-id")
    private Long id;

    @XmlElement(name = "task-name")
    private String name;

    @XmlElement(name = "task-subject")
    private String subject;

    @XmlElement(name = "task-description")
    private String description;

    @XmlElement(name = "task-status")
    private String status;

    @XmlElement(name = "task-priority")
    private Integer priority;

    @XmlElement(name = "task-actual-owner")
    private String actualOwner;

    @XmlElement(name = "task-created-by")
    private String createdBy;

    @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
    @XStreamAlias("local-date-time")
    @XStreamConverter(LocalDateTimeXStreamConverter.class)
    @XmlElement(name = "task-created-on")
    private LocalDateTime createdOn;

    @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
    @XStreamAlias("local-date-time")
    @XStreamConverter(LocalDateTimeXStreamConverter.class)
    @XmlElement(name = "task-last-modification-date")
    private LocalDateTime lastModificationDate;

    @XmlElement(name = "task-proc-inst-id")
    private Long processInstanceId;

    @XmlElement(name = "task-proc-def-id")
    private String processId;

    @XmlElement(name = "task-container-id")
    private String containerId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getActualOwner() {
        return actualOwner;
    }

    public void setActualOwner(String actualOwner) {
        this.actualOwner = actualOwner;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public LocalDateTime getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(LocalDateTime lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    @Override
    public String toString() {
        return "TaskSummary{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", actualOwner='" + actualOwner + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", createdOn=" + createdOn +
                ", processInstanceId=" + processInstanceId +
                ", processId='" + processId + '\'' +
                ", containerId='" + containerId + '\'' +
                '}';
    }
}
