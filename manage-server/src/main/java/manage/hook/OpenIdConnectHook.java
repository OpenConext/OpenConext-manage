package manage.hook;

import manage.oidc.OpenIdConnect;

public class OpenIdConnectHook extends MetaDataHookAdapter {

    private OpenIdConnect openIdConnect;

    public OpenIdConnectHook(OpenIdConnect openIdConnect) {
        this.openIdConnect = openIdConnect;
    }


}
