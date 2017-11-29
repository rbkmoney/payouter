package com.rbkmoney.payouter.meta;

public class UserIdentityRealmExtensionKit extends AbstractUserIdentityExtensionKit {

    public static final String KEY = "user-identity.realm";

    public static final UserIdentityRealmExtensionKit INSTANCE = new UserIdentityRealmExtensionKit();

    public UserIdentityRealmExtensionKit() {
        super(KEY);
    }
}
