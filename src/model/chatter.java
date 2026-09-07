package model;

import java.io.Serializable;
import java.util.Objects;

public class chatter implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nickname;
    private String ip;
    private int port;

    public chatter() {}

    public chatter(String nickname, String ip, int port) {
        this.nickname = nickname;
        this.ip = ip;
        this.port = port;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String toProtocolString() {
        return nickname + "|" + ip + "|" + port;
    }

    public static chatter fromProtocolString(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        String[] parts = str.split("\\|", 3);
        if (parts.length == 3) {
            try {
                return new chatter(parts[0].trim(), parts[1].trim(), Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        chatter c = (chatter) o;
        return port == c.port &&
                Objects.equals(nickname, c.nickname) &&
                Objects.equals(ip, c.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname, ip, port);
    }

    @Override
    public String toString() {
        return nickname + " (" + ip + ":" + port + ")";
    }
}
