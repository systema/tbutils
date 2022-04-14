package com.systema.eia.iot.tb.persistence.load;

import com.systema.eia.iot.tb.clients.ExtRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;

import java.net.URL;

import static com.systema.eia.iot.tb.clients.TbDefaults.*;

public class UserConfigurator {

    final static Logger logger = LoggerFactory.getLogger(UserConfigurator.class);
    private final URL tbUrl;
    private String defaultSysadminPw;

    public UserConfigurator(URL tbUrl) {
        this.tbUrl = tbUrl;
        defaultSysadminPw = TB_SYSADMIN_PW;
    }

    public void updateDefaultUserPasswords(String newSysadminPw, String newUserPw) {
        updateDefaultSysadminPassword(newSysadminPw);
        updatePassword(TB_TENANT_USER, TB_TENANT_PW, newUserPw);
    }

    /**
     * Create a new application user, if it's not already there.
     *
     * @param appUserEmail email / username for the new user; the 1st part of the address (before the "@"9 is saved as
     *                     first name, the last part as last name
     * @param appUserPw    password of the new user
     * @param tenantName   name of a new tenant that is being created along with the new user
     */
    public void createNewApplicationUser(String appUserEmail, String appUserPw, String tenantName) {
        try {
            new ExtRestClient(tbUrl, appUserEmail, appUserPw);
            // login worked -> app user is already present
            logger.info("Application user " + appUserEmail + " is already present - skipping configuration.");
        } catch (Throwable ignored) {
            // app user isn't already present
            ExtRestClient sysadminClient = new ExtRestClient(tbUrl, TB_SYSADMIN_USER, defaultSysadminPw);
            Tenant tenant = createTenant(sysadminClient, tenantName);

            createUser(sysadminClient, tenant, appUserEmail, appUserPw);
        }
    }

    /**
     * Change password of the ThingsBoard default system admin user. The new password will be stored in the
     * UserConfigurator object for future configuration activities.
     *
     * @param newPw new password for the sysadmin user
     * @see com.systema.eia.iot.tb.clients.TbDefaults
     */
    public void updateDefaultSysadminPassword(String newPw) {
        try {
            updatePassword(TB_SYSADMIN_USER, defaultSysadminPw, newPw);
            defaultSysadminPw = newPw;
        } catch (HttpClientErrorException e) {
            logger.info("Pw-change of sysadmin user failed: " + e.getMessage());
        }
    }

    /**
     * Change default password of the ThingsBoard default tenant user. If the current password is not the default one
     * anymore, then the change will be skipped.
     *
     * @param newPw new password for the user
     * @see com.systema.eia.iot.tb.clients.TbDefaults
     */
    public void updateDefaultTenantUserPassword(String newPw) {
        try {
            updatePassword(TB_TENANT_USER, TB_TENANT_PW, newPw);
            logger.info("Adjusted tenant user password");
        } catch (HttpClientErrorException e) {
            logger.info("Skipping pw-change of tenant user");
        }
    }

    /**
     * Change password of ThingsBoard user. If the password is already the new one, nothing happens.
     *
     * @param username
     * @param oldPassword
     * @param newPassword
     * @throws HttpClientErrorException if password change fails.
     */
    public void updatePassword(String username, String oldPassword, String newPassword) {
        try {
            new ExtRestClient(tbUrl, username, oldPassword).changePassword(oldPassword, newPassword);
            logger.info("Adjusted password of user " + username);
        } catch (HttpClientErrorException e) {
            new ExtRestClient(tbUrl, username, newPassword);
            logger.info("Password of user " + username + " was already set to the new value - nothing changed.");
        }
    }

    private Tenant createTenant(ExtRestClient client, String tenantName) {
        logger.info("Creating tenant \"" + tenantName + "\" ...");
        Tenant systemaTenant = new Tenant();
        systemaTenant.setTitle(tenantName);
        return client.saveTenant(systemaTenant);
    }

    private void createUser(ExtRestClient client, Tenant tenant, String userEmail, String userPw) {
        logger.info("Creating application user " + userEmail + " ...");
        User appUser = new User();

        appUser.setEmail(userEmail);
        appUser.setFirstName(userEmail.split("@")[0]);
        appUser.setLastName(userEmail.split("@")[1]);
        appUser.setAuthority(Authority.TENANT_ADMIN);
        appUser.setTenantId(tenant.getId());

        User saveUser = client.saveUser(appUser, false);
        client.activateUser(saveUser.getId(), userPw);
    }
}
