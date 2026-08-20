package com.example.darks.repair_auto.telegram.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSessionState;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSessionState;
import org.junit.jupiter.api.Test;

class TelegramConversationPolicyTest {

    private final TelegramConversationPolicy policy = new TelegramConversationPolicy();

    @Test
    void customerWizardInputsMatchExpectedStates() {
        assertThat(policy.isAllowed(
                TelegramCustomerSessionState.AWAITING_DESCRIPTION,
                TelegramInputType.TEXT)).isTrue();
        assertThat(policy.isAllowed(
                TelegramCustomerSessionState.AWAITING_DESCRIPTION,
                TelegramInputType.PHOTO)).isFalse();
        assertThat(policy.isAllowed(
                TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP,
                TelegramInputType.PHOTO)).isTrue();
        assertThat(policy.isAllowed(
                TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP,
                TelegramInputType.TEXT)).isFalse();
        assertThat(policy.isAllowed(
                TelegramCustomerSessionState.AWAITING_LOCATION,
                TelegramInputType.LOCATION)).isTrue();
        assertThat(policy.isAllowed(
                TelegramCustomerSessionState.CONFIRMING_REQUEST,
                TelegramInputType.CALLBACK)).isTrue();
    }

    @Test
    void technicianInteractiveInputsMatchExpectedStates() {
        assertThat(policy.isAllowed(
                TelegramTechnicianSessionState.AWAITING_REJECTION_REASON,
                TelegramInputType.TEXT)).isTrue();
        assertThat(policy.isAllowed(
                TelegramTechnicianSessionState.AWAITING_REJECTION_REASON,
                TelegramInputType.PHOTO)).isFalse();
        assertThat(policy.isAllowed(
                TelegramTechnicianSessionState.AWAITING_DIAGNOSIS_PHOTO,
                TelegramInputType.PHOTO)).isTrue();
        assertThat(policy.isAllowed(
                TelegramTechnicianSessionState.LANGUAGE_SELECTION,
                TelegramInputType.CALLBACK)).isTrue();
    }
}
