public class ConnectionInetPortList {
    private int partnerPort;
    private String inetAddress;
    //Verbindung mit einer IP-Adresse und einem Partnerport
    public ConnectionInetPortList(String inetAddress, int partnerPort) {
        this.partnerPort = partnerPort;
        this.inetAddress = inetAddress;
    }

    public int getPartnerPort() {
        return partnerPort;
    }

    public void setPartnerPort(int partnerPort) {
        this.partnerPort = partnerPort;
    }

    public String getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(String inetAddress) {
        this.inetAddress = inetAddress;
    }
}

