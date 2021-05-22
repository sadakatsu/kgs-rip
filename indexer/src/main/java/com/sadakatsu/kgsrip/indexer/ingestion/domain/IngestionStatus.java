package com.sadakatsu.kgsrip.indexer.ingestion.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Builder
@Getter
@Setter
public class IngestionStatus {
    private long gameCount;
    private long userCompletedCount;
    private long userInProgressCount;
    private long userNotStartedCount;
    private long userTotalCount;
    private Map<String, WorkerStatus> workerStatuses;
}
