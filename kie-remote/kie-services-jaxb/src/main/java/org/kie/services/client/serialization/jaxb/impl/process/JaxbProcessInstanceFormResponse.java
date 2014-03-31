package org.kie.services.client.serialization.jaxb.impl.process;

import org.kie.services.client.serialization.jaxb.rest.AbstractJaxbResponse;
import org.kie.services.client.serialization.jaxb.rest.AdapterCDATA;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name="process-instance-form")
@XmlAccessorType(XmlAccessType.FIELD)
public class JaxbProcessInstanceFormResponse extends AbstractJaxbResponse {

    @XmlJavaTypeAdapter(AdapterCDATA.class)
    private String formUrl;

    @XmlElement
    private String ctxUID;

    public JaxbProcessInstanceFormResponse() {
        // Default Constructor
    }

    public JaxbProcessInstanceFormResponse(String formUrl, String ctxUID) {
        this.formUrl = formUrl;
        this.ctxUID = ctxUID;
    }

    public JaxbProcessInstanceFormResponse(String formUrl, String ctxUID, String requestUrl) {
        super(requestUrl);
        this.formUrl = formUrl;
        this.ctxUID = ctxUID;
    }

    public String getFormUrl() {
        return formUrl;
    }

    public String getCtxUID() {
        return ctxUID;
    }
}
