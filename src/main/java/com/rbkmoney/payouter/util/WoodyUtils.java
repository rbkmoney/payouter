package com.rbkmoney.payouter.util;

import com.rbkmoney.damsel.payout_processing.ExternalUser;
import com.rbkmoney.damsel.payout_processing.InternalUser;
import com.rbkmoney.damsel.payout_processing.UserInfo;
import com.rbkmoney.damsel.payout_processing.UserType;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.meta.UserIdentityIdExtensionKit;
import com.rbkmoney.payouter.meta.UserIdentityRealmExtensionKit;
import com.rbkmoney.woody.api.trace.ContextUtils;
import com.rbkmoney.woody.api.trace.context.metadata.MetadataExtension;

import java.util.Objects;

/**
 * Created by jeckep on 21.08.17.
 */
public class WoodyUtils {
    public static UserInfo getUserInfo() {
        String userId = ContextUtils.getCustomMetadataValue(UserIdentityIdExtensionKit.INSTANCE.getExtension());
        String realmName = ContextUtils.getCustomMetadataValue(UserIdentityRealmExtensionKit.INSTANCE.getExtension());

        Objects.requireNonNull(userId, "user id not found");
        Objects.requireNonNull(realmName, "Realm not found");

        return new UserInfo(userId, getUserTypeByRealm(realmName));
    }

    public static void setUserInfo(String userId, UserType userType) {
        ContextUtils.setCustomMetadataValue(UserIdentityIdExtensionKit.INSTANCE.getKey(), userId);
        ContextUtils.setCustomMetadataValue(UserIdentityRealmExtensionKit.INSTANCE.getKey(), getRealmNameByUserType(userType));
    }

    public static String getRealmNameByUserType(UserType userType) {
        if (userType.isSetInternalUser()) {
            return "internal";
        }
        if (userType.isSetExternalUser()) {
            return "external";
        }
        throw new NotFoundException(String.format("Failed to get realm by user type, userType=%s", userType));
    }

    public static UserType getUserTypeByRealm(String realmName) {
        switch (realmName) {
            case "internal":
                return UserType.internal_user(new InternalUser());
            case "external":
                return UserType.external_user(new ExternalUser());
            default:
                throw new NotFoundException(String.format("Failed to get user type by realm, realmName=%s", realmName));
        }
    }
}
