package org.kie.services.client.api;

import org.kie.api.runtime.manager.RuntimeManager;

public interface RemoteSessionFactory {

    public RuntimeManager newRuntimeManager();
    
}