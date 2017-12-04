package com.rbkmoney.payouter.meta;

public class UserIdentityIdExtensionKit extends AbstractUserIdentityExtensionKit {

    public static final String KEY = "user-identity.id";

    public static final UserIdentityIdExtensionKit INSTANCE = new UserIdentityIdExtensionKit();

    public UserIdentityIdExtensionKit() {
        super(KEY);
    }
}
