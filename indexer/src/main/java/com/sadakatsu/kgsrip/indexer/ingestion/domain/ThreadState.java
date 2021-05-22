package com.sadakatsu.kgsrip.indexer.ingestion.domain;

public enum ThreadState {
    STOPPED,
    PAUSED,
    SLEEPING_NO_USER,
    SLEEPING_503,
    RUNNING
}
