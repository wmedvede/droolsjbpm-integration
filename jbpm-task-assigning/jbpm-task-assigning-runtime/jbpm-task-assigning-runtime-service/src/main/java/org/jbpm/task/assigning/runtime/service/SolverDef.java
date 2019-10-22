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

public class SolverDef {

    private String solverId;
    private String solverConfigFile;

    public SolverDef(String solverId, String solverConfigFile) {
        this.solverId = solverId;
        this.solverConfigFile = solverConfigFile;
    }

    public String getSolverId() {
        return solverId;
    }

    public String getSolverConfigFile() {
        return solverConfigFile;
    }
}
