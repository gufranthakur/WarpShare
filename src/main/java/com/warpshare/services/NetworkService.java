package com.warpshare.services;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NetworkService {
    public JmDNS jmdns;
    public ServiceInfo serviceInfo;
    public static final String SERVICE_TYPE = "_warpshare._tcp.local.";

    private ExecutorService executor = Executors.newCachedThreadPool();

    public CompletableFuture<Void> startAdvertisingAsync(int port, String deviceName) {
        return CompletableFuture.runAsync(() -> {
            try {
                startAdvertising(port, deviceName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public void startAdvertising(int port, String deviceName) throws IOException {
        // Use specific network interface for better reliability
        InetAddress address = getBestNetworkAddress();
        if (address == null) {
            address = InetAddress.getLocalHost();
        }

        jmdns = JmDNS.create(address);

        Map<String, String> props = new HashMap<>();
        props.put("device", deviceName);
        props.put("version", "1.0");
        props.put("protocol", "http");

        serviceInfo = ServiceInfo.create(SERVICE_TYPE, deviceName, port, 0, 0, props);
        jmdns.registerService(serviceInfo);

        System.out.println("mDNS service advertising started on: " + address.getHostAddress() + ":" + port);
    }

    public CompletableFuture<Void> startDiscoveryAsync(Consumer<ServiceInfo> onServiceFound) {
        return CompletableFuture.runAsync(() -> {
            try {
                startDiscovery(onServiceFound);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public void startDiscovery(Consumer<ServiceInfo> onServiceFound) throws IOException {
        InetAddress address = getBestNetworkAddress();
        if (address == null) {
            address = InetAddress.getLocalHost();
        }

        jmdns = JmDNS.create(address);

        jmdns.addServiceListener(SERVICE_TYPE, new ServiceListener() {
            public void serviceAdded(ServiceEvent event) {
                // Request service info with shorter timeout for faster discovery
                executor.submit(() -> {
                    jmdns.requestServiceInfo(event.getType(), event.getName(), 3000);
                });
            }

            public void serviceRemoved(ServiceEvent event) {
                System.out.println("Service removed: " + event.getName());
            }

            public void serviceResolved(ServiceEvent event) {
                System.out.println("Service resolved: " + event.getName());
                onServiceFound.accept(event.getInfo());
            }
        });

        System.out.println("mDNS discovery started on: " + address.getHostAddress());
    }

    private InetAddress getBestNetworkAddress() {
        try {
            // Find the best non-loopback IPv4 address
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isLoopback() && ni.isUp() && !ni.isVirtual()) {
                    for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            // Prefer addresses in common private ranges
                            String ip = addr.getHostAddress();
                            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                                return addr;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get best network address: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<Void> stopAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                stop();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public void stop() throws IOException {
        if (serviceInfo != null) {
            jmdns.unregisterService(serviceInfo);
            serviceInfo = null;
        }
        if (jmdns != null) {
            jmdns.close();
            jmdns = null;
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        System.out.println("NetworkService stopped");
    }
}