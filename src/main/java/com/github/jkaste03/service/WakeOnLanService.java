package com.github.jkaste03.service;

import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WakeOnLanService {

    private static final Logger log = LoggerFactory.getLogger(WakeOnLanService.class);

    public void sendMagicPacket(String macAddress, String ipStr, int port) {
        try {
            byte[] macBytes = getMacBytes(macAddress);
            byte[] bytes = new byte[6 + 16 * macBytes.length];

            // Magic packet header: 6x 0xFF
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) 0xff;
            }
            // Magic packet body: 16x MAC address
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }

            InetAddress address = InetAddress.getByName(ipStr);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.send(packet);
            }

            log.info("Magic packet sent to {}, {}:{}", macAddress, ipStr, port);
        } catch (Exception e) {
            log.error("Failed to send WoL packet to IP: {}, MAC: {}. Error: {}", ipStr, macAddress, e.getMessage());
            throw new RuntimeException("Internal WoL error", e);
        }
    }

    private byte[] getMacBytes(String macStr) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }

}
