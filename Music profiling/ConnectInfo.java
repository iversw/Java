import java.io.Serializable;
/**
 * This class is just a wrapper around
 * the return value from Proxys remote
 * method, which is a String address
 * and an int port number
 */
public class ConnectInfo implements Serializable {
    public String address;
    public int portNumber;
    public int zone;

    ConnectInfo(String address, int portNumber, int zone) {
        this.address = address;
        this.portNumber = portNumber;
        this.zone = zone;
    }
}
