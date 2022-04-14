package com.systema.iot.examples.vibration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.systema.eia.iot.tb.clients.ExtRestClient;
import com.systema.eia.iot.tb.persistence.TbConfigPaths;
import com.systema.eia.iot.tb.persistence.backup.TbBackup;
import com.systema.eia.iot.tb.persistence.load.ConfigUploader;
import com.systema.eia.iot.tb.persistence.load.UserConfigurator;
import com.systema.eia.iot.tb.persistence.search.TbFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Save and load the thingsboard configuration required for running the tb-utils vibration demo solution.
 */
public class TbConfigurator {

    final static Logger logger = LoggerFactory.getLogger(TbConfigurator.class);

    // intentionally private because should be never needed elsewhere
    // todo this could also come from the docker environment
    public static final String TBUTILS_DEMO_SYSADMIN_PW = "tbutils";
    public static final String TBUTILS_DEMO_TENANT_PW = "tbutils";

    private final String TB_CONFIG__BUNDLE_NAME = "Systema Widgets";
    private final String TB_CONFIG__ROOT_RULE_CHAIN_NAME = "Vibration Root Rule Chain";

    ExtRestClient client;

    TbConfigPaths configPaths;

    TbFinder finder;

    public TbConfigurator(URL tbURL, String tbUser, String tbPassword, File configRootDir) {
        client = new ExtRestClient(tbURL, tbUser, tbPassword);

        configPaths = new TbConfigPaths(configRootDir);
        finder = new TbFinder(client);
    }

    public void updateConfiguration() {
        ConfigUploader rollback = new ConfigUploader(client);

        try {
            // can't be updated automatically (yet)
            if (finder.getRuleChain().getByName(TB_CONFIG__ROOT_RULE_CHAIN_NAME) == null) {
                rollback.getRuleChain().loadAll(configPaths.rulesPath);
            } else {
                System.err.println(
                        "Rule chain is already present - Skipping update of rule-chain, not supported yet by " +
                        "tbutils");
            }

            // update all other configs in place by adjusting extings config items
            rollback.getDeviceProfile().loadAll(configPaths.profilesPath);
            //            rollback.getDevice().loadAll(configPaths.devicesPath);
            rollback.getWidgetBundle().loadAll(configPaths.widgetsBundlePath);
            rollback.getDevice().loadAll(configPaths.devicesPath);

            WidgetsBundle widgetBundle = finder.getWidgetBundle().getByName(TB_CONFIG__BUNDLE_NAME);
            ConfigUploader.WidgetConfigurator widget = rollback.getWidget();
            widget.configureBundle(widgetBundle);
            //TODO: can this be
            // widget.loadAll(configPaths.widgetsPath);
            rollback.getWidget().loadAll(configPaths.widgetsPath);

            // fix entity alias first in dashboard
            loadAndPatchDashboardSelector(rollback, configPaths.dashboardsPath);
        } catch (Exception e) {
            logger.warn("Something went wrong while uploading config to ThingsBoard.");
            e.printStackTrace();
        }
    }

    private void loadAndPatchDashboardSelector(ConfigUploader rollback, File dashboardsPath) {
        ConfigUploader.DashboardConfigurator dashboard = rollback.getDashboard();
        Device device = new TbFinder(client).getDevice().getByName("Drill42");

        File jsonFile = new File(dashboardsPath, "Vibration_CM.json");
        logger.info("Load: " + jsonFile.getAbsolutePath());

        JsonNode dbJson = parseJson(jsonFile);

        // patch the id with the existing device
        ((ObjectNode) dbJson).remove("id");
        // /configuration/entityAliases/600f1e7c-73f5-1952-d253-e08af9fee0c3/filter/singleEntity/id
        JsonNode entityNode =
                dbJson.get("configuration").get("entityAliases").get("0ee6c8a4-cf38-7248-9719-e1d806ca0cf6")
                        .get("filter").get("singleEntity");

        // https://stackoverflow.com/questions/30997362/how-to-modify-jsonnode-in-java
        // https://stackoverflow.com/questions/26352325/why-doesnt-textnode-have-update-valueset-method
        String newId = new TextNode((device != null ? device.getId() : null) + "").textValue();
        ((ObjectNode) entityNode).replace("id", new TextNode(newId));

        // parse into pojo and save in TB
        //TODO can this be simplified to
        // dashboard.load(dbJson);
        Dashboard db = dashboard.read(dbJson);
        dashboard.load(db);
    }

    // just used to encapsulate the ugly try-catch
    private static JsonNode parseJson(File jsonFile) {
        JsonNode jsonNode = null;
        try {
            jsonNode = new ObjectMapper().readTree(jsonFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonNode;
    }

    public void saveConfig(File rootDir) {
        if (!rootDir.isDirectory()) {
            throw new IllegalArgumentException("tb-config directory missing or incorrect" + rootDir);
        }

        TbBackup backup = new TbBackup(client);

        // todo replace hard-coded strings with json point /title
        backup.getDeviceProfile().save("vibration-device-profile", configPaths.profilesPath);
        backup.getRuleChain().save("Vibration Root Rule Chain", configPaths.rulesPath);
        backup.getWidgetBundle().save("Systema Widgets", configPaths.widgetsBundlePath);
        backup.getWidget().save("Systema Widgets", "Change Device", configPaths.widgetsPath);

        // todo needed?
        //        backup.getDevice().save("Drill42", configPaths.devicesPath);

        backup.getDashboard().save("Vibration CM", configPaths.dashboardsPath);

        for (String deviceName : Arrays.asList("Default Vibration Device")) {
            backup.getDevice().save(deviceName, configPaths.devicesPath);
        }

    }

    // CLI utility to save configs
    public static void main(String[] args) throws MalformedURLException {
        String usage = "Usage: ConfigLoader <save/load> <configdir>";

        if (args.length != 2) {
            System.err.println(usage);
            System.exit(1);
        }

        String action = args[0];
        File configRootDirectory = new File(args[1]);

        final String SAVE_ACTION = "save";
        final String LOAD_ACTION = "load";

        if (!Arrays.asList(SAVE_ACTION, LOAD_ACTION).contains(action)) {
            throw new IllegalArgumentException(usage + "\nInvalid action");
        }

        String tbURL = System.getenv().get("TB_URL");
        String tbUser = System.getenv().get("TB_USER");
        String tbPassword = System.getenv().get("TB_PW");

        // if not yet done create application user
        new UserConfigurator(new URL(tbURL)).createNewApplicationUser(tbUser, tbPassword, "SYSTEMA");

        TbConfigurator configLoader = new TbConfigurator(new URL(tbURL), tbUser, tbPassword, configRootDirectory);

        if (action.equals(SAVE_ACTION)) {
            logger.info("Saving thingsboard configuration to " + configRootDirectory);

            configLoader.saveConfig(configRootDirectory);
        } else {
            logger.info("Updating thingsboard configuration to " + configRootDirectory);
            configLoader.updateConfiguration();
        }
    }
}
