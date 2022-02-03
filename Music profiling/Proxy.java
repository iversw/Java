import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.util.concurrent.TimeUnit;
import java.lang.Math;

/**
 * Proxy server
 *
 * Performs load balancing as
 * well as forwarding connections
 * from clients to the relevant
 * servers
 *
 * Its remote method gives the client
 * an address and a port number
 */
class Proxy implements GetConnection {
    private final int NUM_SERVERS = 5;
    private final int PORT_NUMBER_OFFSET = 2000;
    ConnectInfo[] serverAddrs;
    int[] connectionCount;

    static final int PROXY_PORT_NUMBER = 2005;

    /* remote objects */
    BalancerIf[] servers;

    Proxy() throws RemoteException {
        this.serverAddrs = new ConnectInfo[NUM_SERVERS];
        this.connectionCount = new int[NUM_SERVERS];
        servers = new BalancerIf[NUM_SERVERS];

        for (int i = 0; i < 5; i++){
          int j = i + 2000;
          serverAddrs[i] = new ConnectInfo("Server" + j, j, i+1);
        }
    }

    /**
     * TODO:
     * Should all the different hosts be started
     * through a main method, or some other way?
     *
     * TODO:
     * create a more sophisticated solution
     * so that the proxy server still stays
     * up if one or more servers goes down, as well
     * as being able to start the proxy without all
     * servers already being up.
     */
    public static void main(String[] args) throws RemoteException{
        // first initialize remote objects
        Proxy lb = new Proxy();
        lb.init();

        // then host proxys remote interface
        String name = "Proxy";
        try {
            GetConnection stub =
                (GetConnection)UnicastRemoteObject.exportObject(lb, PROXY_PORT_NUMBER);
            Registry reg = LocateRegistry.getRegistry();
            reg.rebind(name, stub);
            System.out.println("Proxy servers GetConnection interface bound in registry");

            String[] regNames = reg.list();
            if (regNames.length == 0) {
                System.out.println("\nREGISTRY EMPTY\n");
            }
            System.out.println("-----------");
            for (String s : regNames) {
                System.out.println(s);
            }

        } catch (Exception e) {
            System.err.println("Exception in proxy");
            e.printStackTrace();
        }
    }

    /**
     * Initializes the remote objects
     */
    private void init() {
        String name = "b" + (PORT_NUMBER_OFFSET + 6);
        for (int i = 0; i < NUM_SERVERS; i++) {
            try {
                Registry reg = LocateRegistry.getRegistry();
                servers[i] = (BalancerIf) reg.lookup(name);
            } catch (RemoteException | NotBoundException e) {
                System.err.println("Failure");
                e.printStackTrace();
            }
        }
    }

    /**
     * @param zoneNumber a number between 1-5 inclusive
     * @return a ConnectInfo object, which is the address of
     * the allocated server
     */
    @Override
    public ConnectInfo getConnection(int zoneNumber) throws RemoteException {
        int index, leftIndex, rightIndex;
        // the modulos here is wrong
        index = Math.floorMod(zoneNumber-1, NUM_SERVERS);
        leftIndex = Math.floorMod(index-1, NUM_SERVERS);
        rightIndex = Math.floorMod(index+1, NUM_SERVERS);

        int serverIndex = checkFreeServer(zoneNumber-1, leftIndex, rightIndex);
        System.out.printf("[ PROXY ]: zoneNumber = %d, ServerIndex = %d\n", zoneNumber, serverIndex);
        if (serverIndex != -1) {
            if (serverIndex != (zoneNumber-1)) {
                System.out.println("[ PROXY ]: Assigning a server in different time-zone!");
                try {
                    TimeUnit.MILLISECONDS.sleep(90);
                } catch (Exception e) {
                    System.err.println("sleeping thread interrupted");
                }
            }
            connectionCount[serverIndex]++;
            return serverAddrs[serverIndex];
        }

        try {
            updateServerInfo(leftIndex, index, rightIndex);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        /* check again for free servers after updating */
        int r = checkFreeServer(zoneNumber-1, leftIndex, rightIndex);
        if (r != -1 && r != (zoneNumber-1)) {
            System.out.println("[ PROXY ]: Assigning a server in different time-zone!");
            try {
                TimeUnit.MILLISECONDS.sleep(90);
            } catch (Exception e) {
                System.err.println("sleeping thread interrupted");
            }
        }
        /* if all are still overloaded then just add to the original target */
        if (r == -1) {
            connectionCount[zoneNumber-1]++;
            return serverAddrs[zoneNumber-1];
        } else {
            connectionCount[r]++;
            return serverAddrs[r];
        }
    }

    /**
     * Get updated info from 3 servers about their
     * workload
     * @param a, b, c : these are the index values of the servers
     * into serverAddrs[], as well as connectionCount[]
     * b should be the target server, a is left neighbor,
     * c is right neighbor
     *
     * TODO: a more sophisticated scheme can be used here:
     * to immediately select one of the servers if they have
     * room, rather than getting updated information from
     * all 3 servers as there is network latency.
     */
    private void updateServerInfo(int a, int b, int c) throws Exception {

        // get updated info from server's remote BalancerIf
        // then update connectionCount[]
        System.err.println("\n===========================");
        System.err.printf("Getting load from servers in zones: %d, %d and %d\n", a+1, b+1, c+1);
        System.err.println("\n===========================");
        connectionCount[a] = servers[a].getLoad();
        connectionCount[b] = servers[b].getLoad();
        connectionCount[c] = servers[c].getLoad();

    }

    /**
     * zoneNumber is the target zone
     * leftIndex is zoneNumber-1, rightIndex is zoneNumber+1
     *
     * returns server index value, or -1 if all are overloaded
     */
    private int checkFreeServer(int zoneNumber, int leftIndex, int rightIndex) {
        // first try server in same zone as client
        if (connectionCount[zoneNumber] < 10) {
            return zoneNumber;
        }

        if (connectionCount[leftIndex] < 10 && connectionCount[rightIndex] < 10) {
            return (connectionCount[leftIndex] < connectionCount[rightIndex] ? leftIndex : rightIndex);
        } else {
            if (connectionCount[leftIndex] < 10) return leftIndex;
            if (connectionCount[rightIndex] < 10) return rightIndex;
        }
        return -1;
    }

}
