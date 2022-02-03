import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.concurrent.*;

class CachedServerJob<T> implements Callable<T> {
    int type;
    String arg1, arg2;
    long executionTimeStart, waitingTimeStart; //TODO: set start time for waitingTimeStart where appropriate
    Server server;

    /**
     * Constructor
     */
    CachedServerJob(int t, String s1, String s2, Server serv, long ws) {
        this.type = t;
        this.arg1 = s1;
        this.arg2 = s2;
        this.server = serv;
        this.waitingTimeStart = ws;
    }

    /**
     * Factory for ServerJobs
     *
     * This is used for getTimesPlayed (type = 1) and getTopThreeMusicByUser (type = 3)
     * @param s1 is the String argument for these jobs
     * T should be declared to be Integer or List<String> corresponding to type
     */
    static <T> CachedServerJob<T> createServerJob(int type, String s1, Server serv, long ws) {
        if (type == 1 || type == 3) {
            return new CachedServerJob<>(type, s1, null, serv, ws);
        } else {
            System.err.println("Error: invalid type for server job!");
            return null;
        }
    }

    /**
     * Overload for factory
     * This one is used for getTimesPlayedByUser and
     * getTopArtistsByUserGenre
     */
    static <T> CachedServerJob<T> createServerJob(int type, String s1, String s2, Server serv, long ws) {
        if (type == 2 || type == 4) {
            return new CachedServerJob<>(type, s1, s2, serv, ws);
        } else {
            System.err.println("Error: invalid type for server job!");
            return null;
        }
    }

    @Override
    public T call() throws InterruptedException {
        Payload result = null;
        /* select the correct job, and dispatch */
        switch (type) {
            case 1 :
                result = getTimesPlayed(arg1);
                break;
            case 2 :
                result = getTimesPlayedByUser(arg1, arg2);
                break;
            case 3 :
                result = getTopThreeMusicByUser(arg1);
                break;
            case 4 :
                result = getTopArtistsByUserGenre(arg1, arg2);
                break;
            default :
                System.err.println("What?");
        }
        return (T) result;
    }

     /**
     * @param musicID representing a song
     * @return the number of times a given musicId has been played
     * @throws InterruptedException in case of connectivity issues
     */
    private Payload getTimesPlayed(String musicID) throws InterruptedException {
        executionTimeStart = System.nanoTime();
        TimeUnit.MILLISECONDS.sleep(80);
        MusicProfile profile = server.musicCache.get(musicID);
        if(profile != null) {
          System.out.println("  [ Retrieving times played from cache ]");
            Payload payload = new Payload(profile.getTotalTimesPlayed(),
                    null,
                    System.nanoTime() - executionTimeStart,
                    executionTimeStart - waitingTimeStart);
            resetTimers();
            return payload;
        } else {
            ArrayList<String> list = readDataset();
            int totalTimesPlayed = 0;
            for (String s : list) {
                if (s.contains(musicID)) {
                    String[] line = s.split(",");
                    totalTimesPlayed += Integer.parseInt(line[line.length - 1].trim());
                }
            }
            server.musicCache.put(musicID ,new MusicProfile(musicID, null, 0, totalTimesPlayed, null));
            Payload payload = new Payload(totalTimesPlayed,
                    null,
                    System.nanoTime() - executionTimeStart,
                    executionTimeStart - waitingTimeStart);
            resetTimers();
            return payload;
        }
    }

    /**
     *Returns an integer representing the number of times a user played the given song represented by a musicId
     * @param musicID id representing a song
     * @param userID id representing a user
     * @return an integer representing the number of times a user played the given song represented by a musicId
     * @throws InterruptedException in case of connectivity issues
     */
    private Payload getTimesPlayedByUser(String musicID, String userID) throws InterruptedException {
        executionTimeStart = System.nanoTime();
        TimeUnit.MILLISECONDS.sleep(80);
        UserProfile userProfile = server.userCache.get(userID);
        if(userProfile != null) {
            MusicProfile musicProfile = userProfile.getMusicProfile(musicID);
            if (musicProfile != null) {
                Payload payload = new Payload(musicProfile.getTimesPlayed(),
                        null,
                        System.nanoTime() - executionTimeStart,
                        executionTimeStart - waitingTimeStart);
                resetTimers();
                return payload;
            }
        }
        ArrayList<String> list = readDataset();
        for (String s : list) {
            if (s.contains(musicID) && s.contains(userID)) {
                String[] line = s.split(",");
                MusicProfile newMusicProfile = new MusicProfile(musicID, null, Integer.parseInt(line[line.length - 1].trim()), 0, null);
                if(userProfile != null) {
                    userProfile.addMusicProfile(newMusicProfile);
                    server.userCache.put(userID, userProfile);
                } else {
                    UserProfile newUserProfile = new UserProfile(userID, null);
                    newUserProfile.addMusicProfile(newMusicProfile);
                    server.userCache.put(userID, newUserProfile);
                }
                Payload payload = new Payload(newMusicProfile.getTimesPlayed(),
                        null,
                        System.nanoTime() - executionTimeStart,
                        executionTimeStart - waitingTimeStart);
                resetTimers();
                return payload;
            }
        }
        System.out.println("  [ WARNING: Returning empty payload ]");
        return new Payload(0,
                null,
                System.nanoTime() - executionTimeStart,
                executionTimeStart - waitingTimeStart);
    }


    /**
     * @param userID id used to identify a user
     * @return a list of the three most played musicId's for a given userId
     * @throws InterruptedException in case of connectivity issues
     */
    private Payload getTopThreeMusicByUser(String userID) throws InterruptedException {
        executionTimeStart = System.nanoTime();
        TimeUnit.MILLISECONDS.sleep(80);
        UserProfile userProfile = server.userCache.get(userID);
        if(userProfile != null) {
            if(userProfile.getTopThreeMusicProfiles() != null) {
                Payload payload = new Payload(0,
                        userProfile.getTopThreeMusicProfiles(),
                        System.nanoTime() - executionTimeStart,
                        executionTimeStart - waitingTimeStart);
                resetTimers();
                return payload;
            }
        }
        ArrayList<String> list = readDataset();
        HashMap<String, Integer> map = new HashMap<>();
        ArrayList<String> topThreeList = new ArrayList<>();
        for (String s : list) {
            if (s.contains(userID)) {
                String[] line = s.split(",");
                String musicId = line[0];
                Integer numberOfTimesPlayed = Integer.parseInt(line[line.length - 1].trim());
                map.put(musicId, numberOfTimesPlayed);
            }
        }
        Stream<Map.Entry<String, Integer>> topEntries = map.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3);
        topEntries.forEach(e -> topThreeList.add(e.getKey()));
        if(userProfile != null) {
            userProfile.setTopThreeMusicProfiles(topThreeList);
            server.userCache.put(userID, userProfile);
        } else {
            UserProfile newUserProfile = new UserProfile(userID, topThreeList);
            server.userCache.put(userID, newUserProfile);
        }
        Payload payload = new Payload(0,
                topThreeList,
                System.nanoTime() - executionTimeStart,
                executionTimeStart - waitingTimeStart);
        resetTimers();
        return payload;
    }

    /**
     * @param userID id used to identify a user
     * @param genre music genre e.g. Rock
     * @return a list of the three most played artistId's within a specific genre
     * @throws InterruptedException in case of connectivity issues
     */
    private Payload getTopArtistsByUserGenre(String userID, String genre) throws InterruptedException {
        executionTimeStart = System.nanoTime();
        TimeUnit.MILLISECONDS.sleep(80);
        UserProfile userProfile = server.userCache.get(userID);
        if(userProfile != null) {
            if(userProfile.getTopArtistsByGenre(genre) != null) {
                Payload payload = new Payload(0,
                        userProfile.getTopArtistsByGenre(genre),
                        System.nanoTime() - executionTimeStart,
                        executionTimeStart - waitingTimeStart);
                resetTimers();
                return payload;
            }
        }
        ArrayList<String> list = readDataset();
        HashMap<String, Integer> map = new HashMap<>();
        ArrayList<String> topThreeList = new ArrayList<>();
        for (String s : list) {
            if (s.contains(userID) && s.contains(genre)) {
                String[] line = s.split(",");
                int numberOfTimesPlayed = Integer.parseInt(line[line.length - 1].trim());
                for (String a : line) {
                    if(a.startsWith("A")) {
                        map.merge(a, numberOfTimesPlayed, Integer::sum);
                    }
                }
            }
        }
        Stream<Map.Entry<String, Integer>> topEntries = map.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3);
        topEntries.forEach(e -> topThreeList.add(e.getKey()));
        if(userProfile != null) {
            userProfile.setTopArtistByGenre(genre, topThreeList);
            server.userCache.put(userID, userProfile);
        } else {
            UserProfile newUserProfile = new UserProfile(userID, topThreeList);
            server.userCache.put(userID, newUserProfile);
        }
        Payload payload = new Payload(0,
                topThreeList,
                System.nanoTime() - executionTimeStart,
                executionTimeStart - waitingTimeStart);
        resetTimers();
        return payload;
    }

    /**
     * @return the dataset from file and return it as a list of strings
     */
    private ArrayList<String> readDataset() {
        System.out.println("Reading data...");
        String line;
        ArrayList<String> list = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("dataset.csv"));
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void resetTimers() {
        executionTimeStart = 0L;
        waitingTimeStart = 0L;
    }
}
