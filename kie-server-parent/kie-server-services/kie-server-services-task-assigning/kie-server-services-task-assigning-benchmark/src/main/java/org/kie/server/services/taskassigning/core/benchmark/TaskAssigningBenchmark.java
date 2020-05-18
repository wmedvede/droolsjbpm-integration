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

package org.kie.server.services.taskassigning.core.benchmark;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.optaplanner.benchmark.api.PlannerBenchmark;
import org.optaplanner.benchmark.api.PlannerBenchmarkFactory;
import org.optaplanner.benchmark.impl.aggregator.swingui.BenchmarkAggregatorFrame;

import static org.kie.server.services.taskassigning.core.benchmark.TaskAssigningBatchGenerator.buildBatchSolution;

public class TaskAssigningBenchmark {

    /**
     * --advanced and --aggregator are optional parameters.
     * Third/or second parameter is the benchmark configuration file
     *
     * e.g.
     *
     * mvn exec:java -Dexec.mainClass="org.kie.server.services.taskassigning.core.benchmark.TaskAssigningBenchmark"
     * -Dexec.args="--advanced org/kie/server/services/taskassigning/solver/taskAssigningBenchmarkConfig-First-Fit-Variants.xml"
     *
     * mvn exec:java -Dexec.mainClass="org.kie.server.services.taskassigning.core.benchmark.TaskAssigningBenchmark"
     * -Dexec.args="--advanced org/kie/server/services/taskassigning/solver/taskAssigningBenchmarkConfig-CH-quick-LS-with-time.xml"
     *
     */
    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);
        String configFile = "org/kie/server/services/taskassigning/solver/taskAssigningBenchmarkConfig.xml";

        boolean advanced = argList.contains("--advanced");
        if (!advanced) {
            runBasicBenchmark();
        } else {
            boolean aggregator = argList.contains("--aggregator");
            if (aggregator && argList.size() == 3) {
                configFile = argList.get(2);
            } else if (!aggregator && argList.size() == 2) {
                configFile = argList.get(1);
            }
            runAdvancedBenchmark(aggregator, configFile);
        }
    }

    public static void runBasicBenchmark() {
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.createFromSolverConfigXmlResource(
                "org/kie/server/services/taskassigning/solver/taskAssigningBasicBenchmarkSolverConfig.xml", new File("local/benchmarkReport"));

        PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark(
                //300 tasks 300 users, 30s looks ok to find an initialized solution with original configuration.
                buildBatchSolution(100, 100, 0)

                //1500 tasks 300 users, 1 min (idem)
                //buildBatchSolution(500, 100)

                //3000 tasks 300 users, 4 min (idem)
                //buildBatchSolution(1000, 100, 0)

                //6000 tasks 900 users, 15 min (idem)
                //buildBatchSolution(2000, 300, 0)
        );

        benchmark.benchmarkAndShowReportInBrowser();
    }

    /**
     * Advanced (benchmark XML): benchmark multiple solver configurations
     */
    public static void runAdvancedBenchmark(boolean aggregator, String configFile) {
        // Build the PlannerBenchmark
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.createFromXmlResource(configFile);

        PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark();
        // Benchmark the problem and show it
        benchmark.benchmarkAndShowReportInBrowser();

        // Show aggregator to aggregate multiple reports
        if (aggregator) {
            BenchmarkAggregatorFrame.createAndDisplayFromXmlResource(configFile);
        }
    }
}
