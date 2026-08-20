package com.example.darks.repair_auto.telegram.conversation;

import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSessionState;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSessionState;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TelegramConversationPolicy {

    private final Map<TelegramCustomerSessionState, Set<TelegramInputType>> customer = new EnumMap<>(
            TelegramCustomerSessionState.class);
    private final Map<TelegramTechnicianSessionState, Set<TelegramInputType>> technician = new EnumMap<>(
            TelegramTechnicianSessionState.class);

    public TelegramConversationPolicy() {
        customer.put(TelegramCustomerSessionState.LANGUAGE_SELECTION, set(TelegramInputType.CALLBACK));
        customer.put(TelegramCustomerSessionState.AWAITING_NAME, set(TelegramInputType.TEXT));
        customer.put(TelegramCustomerSessionState.AWAITING_CONTACT, set(TelegramInputType.CONTACT));
        customer.put(TelegramCustomerSessionState.MAIN_MENU, set(TelegramInputType.TEXT, TelegramInputType.CALLBACK));
        customer.put(TelegramCustomerSessionState.SELECTING_CATEGORY, set(TelegramInputType.CALLBACK));
        customer.put(TelegramCustomerSessionState.AWAITING_DESCRIPTION, set(TelegramInputType.TEXT));
        customer.put(TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP,
                set(TelegramInputType.PHOTO, TelegramInputType.CALLBACK));
        customer.put(TelegramCustomerSessionState.AWAITING_LOCATION,
                set(TelegramInputType.LOCATION, TelegramInputType.TEXT));
        customer.put(TelegramCustomerSessionState.AWAITING_LOCATION_ADDRESS,
                set(TelegramInputType.TEXT, TelegramInputType.LOCATION));
        customer.put(TelegramCustomerSessionState.CONFIRMING_REQUEST, set(TelegramInputType.CALLBACK));
        customer.put(TelegramCustomerSessionState.UPDATING_PROFILE_NAME, set(TelegramInputType.TEXT));
        customer.put(TelegramCustomerSessionState.UPDATING_PROFILE_PHONE, set(TelegramInputType.CONTACT));
        customer.put(TelegramCustomerSessionState.SELECTING_REVIEW_REQUEST, set(TelegramInputType.CALLBACK));
        customer.put(TelegramCustomerSessionState.SELECTING_REVIEW_RATING, set(TelegramInputType.CALLBACK));
        customer.put(TelegramCustomerSessionState.AWAITING_REVIEW_COMMENT,
                set(TelegramInputType.TEXT, TelegramInputType.CALLBACK));
        customer.put(TelegramCustomerSessionState.CONFIRMING_REVIEW, set(TelegramInputType.CALLBACK));

        technician.put(TelegramTechnicianSessionState.LANGUAGE_SELECTION, set(TelegramInputType.CALLBACK));
        technician.put(TelegramTechnicianSessionState.MAIN_MENU, set(TelegramInputType.TEXT, TelegramInputType.CALLBACK));
        technician.put(TelegramTechnicianSessionState.AWAITING_REJECTION_REASON, set(TelegramInputType.TEXT));
        technician.put(TelegramTechnicianSessionState.AWAITING_DIAGNOSIS, set(TelegramInputType.TEXT));
        technician.put(TelegramTechnicianSessionState.AWAITING_WAIT_REASON, set(TelegramInputType.TEXT));
        technician.put(TelegramTechnicianSessionState.AWAITING_RESUME_NOTE, set(TelegramInputType.TEXT));
        technician.put(TelegramTechnicianSessionState.AWAITING_WORK_PERFORMED, set(TelegramInputType.TEXT));
        technician.put(TelegramTechnicianSessionState.AWAITING_DIAGNOSIS_PHOTO, set(TelegramInputType.PHOTO));
        technician.put(TelegramTechnicianSessionState.AWAITING_COMPLETION_PHOTO, set(TelegramInputType.PHOTO));
    }

    public boolean isAllowed(TelegramCustomerSessionState state, TelegramInputType inputType) {
        return customer.getOrDefault(state, Set.of()).contains(inputType);
    }

    public boolean isAllowed(TelegramTechnicianSessionState state, TelegramInputType inputType) {
        return technician.getOrDefault(state, Set.of()).contains(inputType);
    }

    private Set<TelegramInputType> set(TelegramInputType first, TelegramInputType... rest) {
        EnumSet<TelegramInputType> set = EnumSet.of(first, rest);
        return Set.copyOf(set);
    }
}
