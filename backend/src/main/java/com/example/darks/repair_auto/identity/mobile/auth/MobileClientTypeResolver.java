package com.example.darks.repair_auto.identity.mobile.auth;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;

public final class MobileClientTypeResolver {

    private MobileClientTypeResolver() {
    }

    public static ActorType actorType(PushClientType clientType) {
        if (clientType == PushClientType.CUSTOMER_MOBILE) {
            return ActorType.CUSTOMER;
        }
        if (clientType == PushClientType.TECHNICIAN_MOBILE) {
            return ActorType.TECHNICIAN;
        }
        throw new BusinessException(ErrorCode.MOBILE_CLIENT_TYPE_INVALID);
    }

    public static PushClientType clientType(ActorType actorType) {
        if (actorType == ActorType.CUSTOMER) {
            return PushClientType.CUSTOMER_MOBILE;
        }
        if (actorType == ActorType.TECHNICIAN) {
            return PushClientType.TECHNICIAN_MOBILE;
        }
        throw new BusinessException(ErrorCode.MOBILE_CLIENT_TYPE_INVALID);
    }
}
