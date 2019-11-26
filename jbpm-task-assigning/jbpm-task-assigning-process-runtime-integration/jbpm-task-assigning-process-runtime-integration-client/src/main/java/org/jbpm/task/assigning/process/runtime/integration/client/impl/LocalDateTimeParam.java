package org.jbpm.task.assigning.process.runtime.integration.client.impl;

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
public class LocalDateTimeParam implements Comparable<LocalDateTimeParam> {

    public LocalDateTimeParam() {
    }

    public LocalDateTimeParam(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
    @XStreamAlias("local-date-time")
    @XStreamConverter(LocalDateTimeXStreamConverter.class)
    @XmlElement(name = "task-created-on")
    private LocalDateTime localDateTime;

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    @Override
    public int compareTo(LocalDateTimeParam other) {
        return localDateTime.compareTo(other.getLocalDateTime());
    }
}
