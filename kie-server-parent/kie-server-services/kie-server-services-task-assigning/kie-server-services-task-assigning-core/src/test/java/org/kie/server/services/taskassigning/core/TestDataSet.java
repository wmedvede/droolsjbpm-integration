package org.kie.server.services.taskassigning.core;

public enum TestDataSet {

    SET_OF_24TASKS_8USERS_SOLUTION("/data/unsolved/24tasks-8users.xml"),
    SET_OF_50TASKS_5USERS_SOLUTION("/data/unsolved/50tasks-5users.xml"),
    SET_OF_100TASKS_5USERS_SOLUTION("/data/unsolved/100tasks-5users.xml"),
    SET_OF_500TASKS_20USERS_SOLUTION("/data/unsolved/500tasks-20users.xml"),

    SET_OF_1500TASKS_300USERS_SOLUTION("/data/unsolved/1500tasks-300users.xml"),
    SET_OF_3000ASKS_300USERS_SOLUTION("/data/unsolved/3000tasks-300users.xml"),
    SET_OF_6000ASKS_300USERS_SOLUTION("/data/unsolved/9000tasks-900users.xml"),


    NEW_SET_OF_300TASKS_300USERS_SOLUTION("/data/unsolved/task-assigning-batch-300-tasks-300users.xml"),
    NEW_SET_OF_15TASKS_3USERS_SOLUTION("/data/unsolved/task-assigning-batch-15-tasks-3users.xml"),
    NEW_SET_OF_15TASKS_6USERS_SOLUTION("/data/unsolved/task-assigning-batch-15-tasks-6users.xml");



    private String resource;

    TestDataSet(String resource) {
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}
