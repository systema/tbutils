package com.systema.eia.iot.tb.persistence.load;

import com.systema.eia.iot.tb.TbTest;
import com.systema.eia.iot.tb.clients.ExtRestClient;
import com.systema.eia.iot.tb.clients.TbDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.fail;

public class UserConfigTest extends TbTest {

    @Test
    public void testFullUserConfig() {
        var appUserEmail = "user@mail.com";
        var appUserPw = "myUserPw";
        var sysadminPw = "myAdminPw";
        var defaultTenantUserPw = "myTenantPw";

        var userConfigurator = new UserConfigurator(tbUrl);

        userConfigurator.updateDefaultUserPasswords(sysadminPw, defaultTenantUserPw);
        userConfigurator.createNewApplicationUser(appUserEmail, appUserPw, "myTenant");

        new ExtRestClient(tbUrl, TbDefaults.TB_SYSADMIN_USER, sysadminPw);
        new ExtRestClient(tbUrl, appUserEmail, appUserPw);

        try {
            new ExtRestClient(tbUrl, TbDefaults.TB_SYSADMIN_USER, TbDefaults.TB_SYSADMIN_PW);
            fail("Shouldn't be able to login with default sysadmin credentials any longer");
        } catch (HttpClientErrorException e) {
            // expected, since default password shouldnt work any longer
        }
        try {
            new ExtRestClient(tbUrl, TbDefaults.TB_TENANT_USER, TbDefaults.TB_TENANT_PW);
            fail("Shouldn't be able to login with default tenant credentials any longer");
        } catch (HttpClientErrorException e) {
            // expected, since default password shouldnt work any longer
        }

        //change passwords back to defaults so that other tests wont fail
        userConfigurator.updatePassword(TbDefaults.TB_SYSADMIN_USER, sysadminPw, TbDefaults.TB_SYSADMIN_PW);
        userConfigurator.updatePassword(TbDefaults.TB_TENANT_USER, defaultTenantUserPw, TbDefaults.TB_TENANT_PW);
    }
}
