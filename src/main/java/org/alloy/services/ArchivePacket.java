package org.alloy.services;

import java.time.LocalDateTime;

/**
 * Модель пакета в стиле archive проекта
 */
public class ArchivePacket {
    private String ip;
    private String mac;
    private String data;
    private LocalDateTime serverDatetime;
    
    public ArchivePacket() {
        this.serverDatetime = LocalDateTime.now();
    }
    
    public ArchivePacket(String ip, String mac, String data) {
        this.ip = ip;
        this.mac = mac;
        this.data = data;
        this.serverDatetime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public String getMac() {
        return mac;
    }
    
    public void setMac(String mac) {
        this.mac = mac;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public LocalDateTime getServerDatetime() {
        return serverDatetime;
    }
    
    public void setServerDatetime(LocalDateTime serverDatetime) {
        this.serverDatetime = serverDatetime;
    }
    
    @Override
    public String toString() {
        return "ArchivePacket{" +
                "ip='" + ip + '\'' +
                ", mac='" + mac + '\'' +
                ", data='" + data + '\'' +
                ", serverDatetime=" + serverDatetime +
                '}';
    }
}
