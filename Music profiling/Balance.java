import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Balance implements BalancerIf {
    Server s;
    int port;

    Balance(Server serverObj, int p) {
        this.s = serverObj;
        this.port = p;
    }

    void init() {
        try {
            BalancerIf stub = (BalancerIf) UnicastRemoteObject.exportObject(this, port);
            Registry registry = LocateRegistry.getRegistry();
            String name = "b" + port;
            registry.bind("b" + port, stub);
            System.err.println("Balance: " + name + " ready");
        } catch (Exception e) {
            System.err.println("Balance exception: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Send info to proxy server about the amount
     * of work in the queue.
     */
    @Override
    public int getLoad() {
        return s.getSize();
    }
}