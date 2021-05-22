package com.sadakatsu.kgsrip.indexer.ingestion.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WorkerStatus {
    public static WorkerStatus getStopped() {
        return WorkerStatus.builder()
            .state(ThreadState.STOPPED)
            .build();
    }

    public static WorkerStatus getPaused() {
        return WorkerStatus.builder()
            .state(ThreadState.PAUSED)
            .build();
    }

    public static WorkerStatus getSleepingNoUser() {
        return WorkerStatus.builder()
            .state(ThreadState.SLEEPING_NO_USER)
            .build();
    }

    public static WorkerStatus getSleeping503(String username, int year, int month) {
        return WorkerStatus.builder()
            .state(ThreadState.SLEEPING_503)
            .username(username)
            .year(year)
            .month(month)
            .build();
    }

    public static WorkerStatus getRunning(String username, int year, int month) {
        return WorkerStatus.builder()
            .state(ThreadState.RUNNING)
            .username(username)
            .year(year)
            .month(month)
            .build();
    }

    private final Integer month;
    private final Integer year;
    private final String username;
    private final ThreadState state;
}
