import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BalancerIf extends Remote {

    int getLoad() throws RemoteException, InterruptedException;

}
