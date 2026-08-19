package com.example.darks.repair_auto.repair.action.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Action currently executable by the authenticated actor on the repair request or job")
public enum RepairAvailableAction {
    @Schema(description = "Accept a pending job assignment")
    ACCEPT_ASSIGNMENT,

    @Schema(description = "Reject a pending job assignment")
    REJECT_ASSIGNMENT,

    @Schema(description = "Start repair execution on an assigned or scheduled job")
    START_REPAIR,

    @Schema(description = "Update diagnostic findings for an in-progress or waiting-for-parts repair")
    UPDATE_DIAGNOSIS,

    @Schema(description = "Transition an in-progress repair to waiting for parts")
    WAIT_FOR_PARTS,

    @Schema(description = "Resume an in-progress repair from waiting for parts")
    RESUME_REPAIR,

    @Schema(description = "Upload a diagnostic photo for an active repair")
    UPLOAD_DIAGNOSIS_PHOTO,

    @Schema(description = "Upload a completion photo for an in-progress repair")
    UPLOAD_COMPLETION_PHOTO,

    @Schema(description = "Complete an in-progress repair after diagnosis and completion photo are recorded")
    COMPLETE_REPAIR,

    @Schema(description = "Upload a customer problem photo for an active repair request")
    UPLOAD_PROBLEM_PHOTO,

    @Schema(description = "Submit a customer rating and review for a completed repair")
    SUBMIT_REVIEW
}
