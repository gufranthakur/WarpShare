package com.warpshare.services;


import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NetworkService {
    public JmDNS jmdns;
    public ServiceInfo serviceInfo;
    public static final String SERVICE_TYPE = "_warpshare._tcp.local.";

    public void startAdvertising(int port, String deviceName) throws IOException {
        jmdns = JmDNS.create(InetAddress.getLocalHost());
        Map<String, String> props = new HashMap<>();
        props.put("device", deviceName);

        serviceInfo = ServiceInfo.create(SERVICE_TYPE, deviceName, port, 0, 0, props);
        jmdns.registerService(serviceInfo);
    }

    public void startDiscovery(Consumer<ServiceInfo> onServiceFound) throws IOException {
        jmdns = JmDNS.create(InetAddress.getLocalHost());
        jmdns.addServiceListener(SERVICE_TYPE, new ServiceListener() {
            public void serviceAdded(ServiceEvent event) {
                jmdns.requestServiceInfo(event.getType(), event.getName(), 1000);
            }

            public void serviceRemoved(ServiceEvent event) {}

            public void serviceResolved(ServiceEvent event) {
                onServiceFound.accept(event.getInfo());
            }
        });
    }

    public void stop() throws IOException {
        if (serviceInfo != null) {
            jmdns.unregisterService(serviceInfo);
        }
        if (jmdns != null) {
            jmdns.close();
        }
    }
}