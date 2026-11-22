/**
 * MODEL layer — business services.
 *
 * <p>Services own business rules and are the only layer controllers may call.
 * Planned services: {@code LoginService}, {@code SessionRegistry},
 * {@code LockService}, {@code ProjectionService}, {@code MonitoringService},
 * {@code RecordingService} (MP4 via JavaCV), {@code FileDistributionService},
 * {@code SubmissionService}, {@code AccountService}, {@code RetentionJob}.
 * Network work runs on virtual threads (Docs/ke_hoach_trien_khai.md, section 1).</p>
 */
package com.dau.labmanagement.server.model.service;
