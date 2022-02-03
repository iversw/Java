import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a "smart" server with caching which implements the interface MusicStats. The interface is overridden and exposed to
 * clients through remote object invocation (RMI)
 */
public class Server implements MusicStats {

    private final int QUEUE_CAPACITY = 20;
    private LinkedBlockingDeque<Runnable> waitingList;
    private ThreadPoolExecutor pool;
    private boolean naiveMode;

    /* port and name */
    private final String name;
    private final int port;

    /* caches */
    // Cache 100 musicProfiles
    LinkedHashMap<String, MusicProfile> musicCache = new LinkedHashMap<String, MusicProfile>(100, 0.7f, true) {
        private static final int MAX_ENTRIES = 100;

        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    };
    // Cache 100 userProfiles
    LinkedHashMap<String, UserProfile> userCache = new LinkedHashMap<String, UserProfile>(100, 0.7f, true) {
        private static final int MAX_ENTRIES = 100;

        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    /**
     * Public constructor for Server
     * @param name unique server name
     * @param port unique port
     */
    public Server(String name, int port, boolean naive) {
        this.waitingList = new LinkedBlockingDeque<>(QUEUE_CAPACITY);
        this.pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, waitingList);
        this.naiveMode = naive;
        this.name = name;
        this.port = port;
        // naiveMode = false;
    }

    /**
     * Sets up the remote interfaces through Server.java and
     * Balance.java
     *
     * 2006 through 2010 are the port number for Balance objects
     */
    public void launch() {
        try {
            MusicStats stub = (MusicStats) UnicastRemoteObject.exportObject(this, port);

            Balance balance = new Balance(this, port + 6);
            balance.init();

            Registry registry = LocateRegistry.getRegistry();
            registry.bind(name, stub);
            System.err.println("Server: " + name + " ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Use
     * > java Server -n
     * to start the server in naive mode.
     * This uses NaiveServerJob instead of CachedServerJob
     */
    public static void main(String[] args) {
        System.getProperty("user.dir");
        int portNum = 50000;

        Server obj = new Server("Server"+portNum, portNum, true);
        obj.launch();

        /* parse cmd line */
        for (String s : args) {
            if (s.equals("-n")) {
                obj.naiveMode = true;
                break;
            }
        }
        if (obj.naiveMode) {
            System.out.println("Running in naive mode...");
        } else {
            System.out.println("Running in cached mode...");
        }
    }


    /**
     * @param musicID representing a song
     * @return the number of times a given musicId has been played
     * @throws RemoteException in case of connectivity issues
     */
    @Override
    public Payload getTimesPlayed(String musicID) throws RemoteException {
        // return FutureTask?
        // --> Futures cannot be returned from remote object, so we have to
        // wait for the Future inside the remote method
        long waitingTimeStart = System.nanoTime();

        // TODO:
        // Have the methods return
        // timing information to the client
        NaiveServerJob<Payload> naiveJob;
        CachedServerJob<Payload> cachedJob;
        Future<Payload> future;
        // create server job and submit to queue
        if (naiveMode) {
            naiveJob = NaiveServerJob.createServerJob(1, musicID, waitingTimeStart);
            assert naiveJob != null;
            future = pool.submit(naiveJob);
        } else {
            cachedJob = CachedServerJob.createServerJob(1, musicID, this, waitingTimeStart);
            assert cachedJob != null;
            future = pool.submit(cachedJob);
        }

        Payload res = null;
        try {
            res = future.get();
        } catch (InterruptedException e) {
            System.err.println("The remote method was interrupted.");
        } catch (ExecutionException e) {
            System.err.println("The exec thread aborted task.");
            System.err.println("Cause: " + e.getCause());
        }
        return res;
    }

    /**
     *Returns an integer representing the number of times a user played the given song represented by a musicId
     * @param musicID id representing a song
     * @param userID id representing a user
     * @return an integer representing the number of times a user played the given song represented by a musicId
     * @throws RemoteException in case of connectivity issues
     */
    @Override
    public Payload getTimesPlayedByUser(String musicID, String userID) throws RemoteException, InterruptedException {
        long waitingTimeStart = System.nanoTime();
        NaiveServerJob<Payload> naiveJob;
        CachedServerJob<Payload> cachedJob;
        Future<Payload> future;
        // create server job and submit to queue
        if (naiveMode) {
            naiveJob = NaiveServerJob.createServerJob(2, musicID, userID, waitingTimeStart);
            assert naiveJob != null;
            future = pool.submit(naiveJob);
        } else {
            cachedJob = CachedServerJob.createServerJob(2, musicID, userID, this, waitingTimeStart);
            assert cachedJob != null;
            future = pool.submit(cachedJob);
        }

        Payload res = null;
        try {
            res = future.get();
        } catch (ExecutionException e) {
            System.err.println("The exec thread aborted task.");
            System.err.println("Cause: " + e.getCause());
        }
        return res;
    }

    /**
     * @param userID id used to identify a user
     * @return a list of the three most played musicId's for a given userId
     * @throws RemoteException in case of connectivity issues
     */
    @Override
    public Payload getTopThreeMusicByUser(String userID) throws RemoteException, InterruptedException {
        long waitingTimeStart = System.nanoTime();
        NaiveServerJob<Payload> naiveJob;
        CachedServerJob<Payload> cachedJob;
        Future<Payload> future;
        // create server job and submit to queue
        if (naiveMode) {
            naiveJob = NaiveServerJob.createServerJob(3, userID, waitingTimeStart);
            assert naiveJob != null;
            future = pool.submit(naiveJob);
        } else {
            cachedJob = CachedServerJob.createServerJob(3, userID, this, waitingTimeStart);
            assert cachedJob != null;
            future = pool.submit(cachedJob);
        }

        Payload res = null;
        try {
            res = future.get();
        } catch (ExecutionException e) {
            System.err.println("The exec thread aborted task.");
            System.err.println("Cause: " + e.getCause());
        }
        return res;
    }

    /**
     * @param userID id used to identify a user
     * @param genre music genre e.g. Rock
     * @return a list of the three most played artistId's within a specific genre
     * @throws RemoteException in case of connectivity issues
     */
    @Override
    public Payload getTopArtistsByUserGenre(String userID, String genre) throws RemoteException, InterruptedException {
        long waitingTimeStart = System.nanoTime();
        NaiveServerJob<Payload> naiveJob;
        CachedServerJob<Payload> cachedJob;
        Future<Payload> future;
        // create server job and submit to queue
        if (naiveMode) {
            naiveJob = NaiveServerJob.createServerJob(4, userID, genre, waitingTimeStart);
            assert naiveJob != null;
            future = pool.submit(naiveJob);
        } else {
            cachedJob = CachedServerJob.createServerJob(4, userID, genre, this, waitingTimeStart);
            assert cachedJob != null;
            future = pool.submit(cachedJob);
        }

        Payload res = null;
        try {
            res = future.get();
        } catch (ExecutionException e) {
            System.err.println("The exec thread aborted task.");
            System.err.println("Cause: " + e.getCause());
        }
        return res;
    }

    /**
     * called by Balance
     * to return the list size
     */
    int getSize() {
        return waitingList.size();
    }

}
