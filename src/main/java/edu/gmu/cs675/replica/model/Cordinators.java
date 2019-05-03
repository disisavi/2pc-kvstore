package edu.gmu.cs675.replica.model;

public class Cordinators {
    private Integer seq;
    private String IP;

    public Cordinators(String Ip) {
        this.IP = Ip;
    }

    public Cordinators() {
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
