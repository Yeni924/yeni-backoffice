package com.yeni.backoffice.core.payment.dto;

public final class WorkerDtos {

    private WorkerDtos() {
    }

    public record WorkerRunRequest(Integer limit) {
        public int normalizedLimit() {
            return limit == null ? 20 : Math.min(Math.max(limit, 1), 100);
        }
    }

    public record WorkerRunResult(
            int targetCount,
            int claimedCount,
            int successCount,
            int failureCount,
            int skippedCount
    ) {
    }

    public record WorkerItemResult(boolean success) {
    }
}
