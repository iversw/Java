import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GetConnection extends Remote {

    ConnectInfo getConnection(int zoneNumber) throws RemoteException;
}