package com.systema.eia.iot.tb.examples;

import com.systema.eia.iot.tb.clients.ExtRestClient;
import com.systema.eia.iot.tb.persistence.search.TbFinder;
import com.systema.eia.iot.tb.utils.SubscriptionType;
import kotlin.Unit;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.relation.EntityRelation;

import java.util.List;
import java.util.UUID;

import static com.systema.eia.iot.tb.ws.WsSubscribeKt.subscribeToWS;

public class RestClientExample {
    public static void main(String[] args) {

        String password = System.getenv().get("TB_PW");
        String username = System.getenv().get("TB_USER");
        String tbURL = System.getenv().get("TB_URL");

        if (username == null || password == null || tbURL == null) {
            throw new IllegalArgumentException("tb pw or url not set!");
        }

        // ThingsBoard REST API URL
        //        String url = "http://localhost:8080";
        String url = "http://" + tbURL;

        // Creating new rest client and auth with credentials
        ExtRestClient client = new ExtRestClient(url);
        client.login(username, password);

        // Find devices by profile name
        new TbFinder(client).getDevice().getAllByProfile("default");

        // Creating an Asset
        Asset asset = new Asset();
        asset.setName("Building " + UUID.randomUUID());
        asset.setType("building");
        asset = client.saveAsset(asset);

        // creating a Device
        Device device = new Device();
        device.setName("Thermometer 1 -" + UUID.randomUUID());
        device.setType("thermometer");
        device = client.saveDevice(device);

        // creating relations from device to asset
        EntityRelation relation = new EntityRelation();
        relation.setFrom(asset.getId());
        relation.setTo(device.getId());
        relation.setType("Contains");
        client.saveRelation(relation);

        subscribeToWS(client, device.getId(), SubscriptionType.SHARED_SCOPE, List.of("temperature", "humidity"),
                (changes) -> {
                    changes.forEach((change) -> System.out.println(change));
                    return Unit.INSTANCE; // ~void; required for Java-Kotlin compatibility
                });

        System.out.println("finished setting up smthg :-)");
    }
}