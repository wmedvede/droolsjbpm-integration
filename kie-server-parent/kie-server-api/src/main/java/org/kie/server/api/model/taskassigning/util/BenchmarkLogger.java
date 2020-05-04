/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.api.model.taskassigning.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkLogger.class);

    public static final String SIMPLE_TIME_FORMAT = "{},{}";

    private BenchmarkLogger() {
    }

    public static void debug(String format, Object... arguments) {
        LOGGER.debug(format, arguments);
    }

    public static void debug(String msg) {
        LOGGER.debug(msg);
    }
}
