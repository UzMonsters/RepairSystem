package com.example.darks.repair_auto.telegram.technician.domain;

public enum TelegramTechnicianSessionState {
    LANGUAGE_SELECTION,
    MAIN_MENU,
    AWAITING_REJECTION_REASON,
    AWAITING_DIAGNOSIS,
    AWAITING_WAIT_REASON,
    AWAITING_RESUME_NOTE,
    AWAITING_WORK_PERFORMED,
    AWAITING_DIAGNOSIS_PHOTO,
    AWAITING_COMPLETION_PHOTO;

    public boolean isAwaitingInput() {
        return this == AWAITING_REJECTION_REASON
                || this == AWAITING_DIAGNOSIS
                || this == AWAITING_WAIT_REASON
                || this == AWAITING_WORK_PERFORMED
                || this == AWAITING_DIAGNOSIS_PHOTO
                || this == AWAITING_COMPLETION_PHOTO;
    }
}
