package com.cashi.osiptelvalidation.interfaces.rest.resources;

/**
 * Response: POST /api/v1/osiptel/batches
 */
public record EnqueueBatchResource(int enqueued, int skipped, String batchId) {}
