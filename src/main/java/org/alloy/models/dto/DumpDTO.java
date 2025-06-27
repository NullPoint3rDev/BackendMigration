package org.alloy.models.dto;

import java.time.LocalDateTime;

public class DumpDTO {
    private Integer id;
    private LocalDateTime dateCreated;
    private String mac;
    private String ip;
    private String data;
    // ... другие нужные поля

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
} 