import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MusicStats extends Remote {

    Payload getTimesPlayed(String musicID) throws RemoteException, InterruptedException;

    Payload getTimesPlayedByUser(String musicID, String userID) throws RemoteException, InterruptedException;

    Payload getTopThreeMusicByUser(String userID) throws RemoteException, InterruptedException;

    Payload getTopArtistsByUserGenre(String userID, String genre) throws RemoteException, InterruptedException;

}
