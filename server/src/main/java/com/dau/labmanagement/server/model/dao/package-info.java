/**
 * MODEL layer — DAOs over JDBI 3.
 *
 * <p>One DAO per table of the ERD (9 tables): {@code UserDao},
 * {@code MachineDao}, {@code SystemSettingDao}, {@code StudentSessionDao},
 * {@code SubmissionSessionDao}, {@code RecordingFileDao},
 * {@code RecordingLogDao}, {@code SubmissionDao}, {@code SubmissionFileDao}.
 * DAOs only translate rows to records; business rules belong to services.
 * Added at FND-02 (Sprint 0) together with db/migration.</p>
 */
package com.dau.labmanagement.server.model.dao;
