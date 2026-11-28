/**
 * CONTROLLER layer (MVC).
 *
 * <p>JavaFX controllers wired to FXML views via {@code fx:controller}.
 * Controllers translate user actions into calls on services
 * ({@code model.service}) and never touch DAOs or sockets directly.
 * One controller per screen: LoginController (SC_001),
 * DashboardController (post-login shell, SC_003 for teachers and SC_004 for
 * admins), AccountController (SC_005), LockUnlockController (SC_006),
 * ProjectionController (SC_007), LiveViewController (SC_008),
 * RecordingController (SC_009), FileDistributionController (SC_010),
 * SubmissionController (SC_011), TeacherRecordingsController (SC_012),
 * AdminRecordingsController (SC_013).</p>
 */
package com.dau.labmanagement.server.controller;
