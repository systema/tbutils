package com.systema.eia.iot.tb.examples;

import com.systema.eia.iot.tb.clients.ExtRestClient;
import com.systema.eia.iot.tb.utils.Scope;
import com.systema.eia.iot.tb.utils.SubscriptionType;
import com.systema.eia.iot.tb.ws.AttributeHistoryKt;
import com.systema.eia.iot.tb.ws.WsSubscribeKt;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.UUID;

public class JavaSupportLibUsage {

    public static void main(String[] args) {
        String tbURL = System.getenv().get("TB_URL");
        ExtRestClient erc = new ExtRestClient(tbURL);


        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        erc.saveAttribute(deviceId, Scope.CLIENT_SCOPE, "foo", "bar");

        WsSubscribeKt.subscribeToWSMsg(erc, DeviceId.fromString("foo"), SubscriptionType.LATEST_TELEMETRY, s -> {
            System.out.println("foo");

            // see https://discuss.kotlinlang.org/t/java-interop-unit-closures-required-to-return-kotlin-unit/1842/11
            return null;
        });

        // register device for automatic telemetry updates
        AttributeHistoryKt.saveAttributeChanges(erc, deviceId);
    }
}
