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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;

public class BenchmarkRegistry {

    private static final Map<String, StopWatch> timeRegistry = new HashMap<>();

    private BenchmarkRegistry() {
    }

    public static void registerStartTime(String timeId) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        timeRegistry.put(timeId, stopWatch);
    }

    public static StopWatch registerEndTime(String timeId) {
        return registerEndTime(timeId, false, true);
    }

    public static StopWatch registerSafeEndTime(String timeId) {
        return registerEndTime(timeId, true, true);
    }

    private static StopWatch registerEndTime(String timeId, boolean safeMode, boolean printLog) {
        StopWatch stopWatch = timeRegistry.get(timeId);
        if (safeMode) {
            if (stopWatch != null && !stopWatch.isStopped()) {
                stopWatch.stop();
            }
        } else {
            stopWatch.stop();
        }
        if (printLog) {
            BenchmarkLogger.debug(BenchmarkLogger.SIMPLE_TIME_FORMAT, timeId, stopWatch != null ? stopWatch.getTime() : 0);
        }
        return stopWatch;
    }
}

