package edu.gmu.cs675.master.model;


public class Replicas {
    private Integer seq;
    private String IP;

    public Replicas(String ip) {
        this.IP = ip;
    }

    public Replicas() {

    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }
}
