import java.io.*;
import java.util.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Client {
    static Map<String, int[]> sumOfAllTimes;
    static LinkedHashMap<String, MusicProfile> musicCache;
    static LinkedHashMap<String, UserProfile> userCache;
    private static String FILENAME;

    public Client() {}

    public static void main(String[] args) {
        sumOfAllTimes = new HashMap<>();

        if (args.length > 0) {
            FILENAME = "naive_server_cached_client_" + args[0] + ".txt";
        }
        if (args.length > 1) {
            FILENAME = "server_client_cache_" + args[0] + ".txt";
        }

        musicCache = new LinkedHashMap<String, MusicProfile>(500, 0.7f, true) {
          private static final int MAX_ENTRIES = 250;

          protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
          }
        };

        userCache = new LinkedHashMap<String, UserProfile>(200, 0.7f, true) {
          private static final int MAX_ENTRIES = 100;

          protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
          }
        };

        try {

            Registry proxyRegistry = LocateRegistry.getRegistry();
            GetConnection proxyStub = (GetConnection) proxyRegistry.lookup("Proxy");
            ArrayList<String> queries = readQueries();

            for (String query : queries) {
                boolean foundInCache = checkCache(query);
                if (!foundInCache) {
                    int zone = Integer.parseInt(query.substring(query.length() -1));
                    ConnectInfo hostServer = proxyStub.getConnection(zone);
                    Registry serverRegistry = LocateRegistry.getRegistry();
                    MusicStats serverStub = (MusicStats) serverRegistry.lookup(hostServer.address);
                    passQuery(query, serverStub, hostServer.zone);
                }
            }
            writeAvgTimes();

        } catch (Exception e) {
            System.err.println("Client exception: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Read queries from file and returns them as ArrayList<String>.
     * No argument is required, but assumes you have a txt file with queries saved
     * as "queries.txt" in the same directory.
     * @return ArrayList<String> of lines contained in file
     */
    private static ArrayList<String> readQueries() {
        System.out.println("Reading queries...");
        String line;
        ArrayList<String> list = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader("cachedQueries.txt"));
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Checks if the query is cached. Writes to file if it exists in cache.
     * @param query: a query line from the query file
     * @return boolean: true if ID is found in cache, false if not
     */
    private static boolean checkCache(String query) {
        String method = getMethod(query);
        String[] arguments = getArguments(query);
        MusicProfile mProfile;
        UserProfile uProfile;
        double[] processTime = new double[1];

        long start = System.nanoTime();
        switch (method) {
            case "getTimesPlayed":
                mProfile = musicCache.get(arguments[0]);
                if (mProfile == null) {
                    return false;
                } else {
                    int timesPlayed = mProfile.getTotalTimesPlayed();
                    processTime[0] = (System.nanoTime()-start) / 1000000.0;

                    writeTimesPlayed(arguments[0], timesPlayed, processTime, 0);
                    return true;
                }

            case "getTimesPlayedByUser":
                uProfile = userCache.get(arguments[1]);
                if (uProfile == null) {
                    return false;
                } else {
                    mProfile = uProfile.getMusicProfile(arguments[0]);
                    if (mProfile == null) {
                        return false;
                    } else {
                        int timesPlayed = mProfile.getTotalTimesPlayed();
                        processTime[0] = (System.nanoTime()-start) / 1000000.0;

                        writeTimesPlayedByUser(arguments[0], timesPlayed, arguments[1], processTime, 0);
                        return true;
                    }
                }

            case "getTopThreeMusicByUser":
                uProfile = userCache.get(arguments[0]);
                if (uProfile == null) {
                    return false;
                } else {
                    List<String> topMusic = uProfile.getTopThreeMusicProfiles();
                    processTime[0] = (System.nanoTime()-start) / 1000000.0;
                    if (topMusic == null) {
                      return false;
                    }

                    writeTopThreeMusicByUser(arguments[0], topMusic, processTime, 0);
                    return true;
                }

            case "getTopArtistsByUserGenre":
                String genre = arguments[1];
                uProfile = userCache.get(arguments[0]);
                if (uProfile == null) {
                    return false;
                } else {
                    List<String> topArtists = uProfile.getTopArtistsByGenre(genre);
                    processTime[0] = (System.nanoTime()-start) / 1000000.0;
                    if (topArtists == null) {
                        return false;
                    }

                    writeTopArtistsByUserGenre(genre, arguments[0], topArtists, processTime, 0);
                    return true;
                }
        }
        return false;
    }

    /**
     * Passes a query to a provided stub and sends results to output writing.
     * @param query: a line in a query file
     * @param stub: a stub of an interface from a server
     */
    private static void passQuery(String query, MusicStats stub, int zone) throws RemoteException, InterruptedException {
        String method = getMethod(query);
        String[] arguments = getArguments(query);
        String musicID, userID;
        Payload response;
        double[] runtimes = new double[3];
        int[] sumTimes;

        long start = System.nanoTime();
        switch (method) {
            case "getTimesPlayed":
                musicID = arguments[0];
                response = stub.getTimesPlayed(musicID);
                runtimes[0] = (System.nanoTime()-start) / 1000000.0;
                runtimes[1] = response.executionTime / 1000000.0;
                runtimes[2] = response.waitingTime / 1000000.0;

                //sumTimes = [numCalls, sumTurnaround, sumExecution, sumWaiting]
                sumTimes = sumOfAllTimes.getOrDefault(method, new int[4]);
                sumTimes[0] ++;
                sumTimes[1] += runtimes[0];
                sumTimes[2] += runtimes[1];
                sumTimes[3] += runtimes[2];
                sumOfAllTimes.put(method, sumTimes);

                musicCache.put(musicID, new MusicProfile(musicID, null, 0, response.timesPlayed, null));
                writeTimesPlayed(musicID, response.timesPlayed, runtimes, zone);
                break;

            case "getTimesPlayedByUser":
                musicID = arguments[0];
                userID = arguments[1];
                response = stub.getTimesPlayedByUser(musicID, userID);
                runtimes[0] = (System.nanoTime()-start) / 1000000.0;
                runtimes[1] = response.executionTime / 1000000.0;
                runtimes[2] = response.waitingTime / 1000000.0;

                sumTimes = sumOfAllTimes.getOrDefault(method, new int[4]);
                sumTimes[0] ++;
                sumTimes[1] += runtimes[0];
                sumTimes[2] += runtimes[1];
                sumTimes[3] += runtimes[2];
                sumOfAllTimes.put(method, sumTimes);

                MusicProfile mProfile = new MusicProfile(musicID, null, 0, response.timesPlayed, null);
                musicCache.put(musicID, mProfile);
                UserProfile uProfile = new UserProfile(userID, null);
                uProfile.addMusicProfile(mProfile);
                userCache.put(userID, uProfile);

                writeTimesPlayedByUser(musicID, response.timesPlayed, userID, runtimes, zone);
                break;

            case "getTopThreeMusicByUser":
                userID = arguments[0];
                response = stub.getTopThreeMusicByUser(userID);
                runtimes[0] = (System.nanoTime()-start) / 1000000.0;
                runtimes[1] = response.executionTime / 1000000.0;
                runtimes[2] = response.waitingTime / 1000000.0;

                sumTimes = sumOfAllTimes.getOrDefault(method, new int[4]);
                sumTimes[0] ++;
                sumTimes[1] += runtimes[0];
                sumTimes[2] += runtimes[1];
                sumTimes[3] += runtimes[2];
                sumOfAllTimes.put(method, sumTimes);

                userCache.put(userID, new UserProfile(userID, response.topThree));
                writeTopThreeMusicByUser(userID, response.topThree, runtimes, zone);
                break;

            case "getTopArtistsByUserGenre":
                userID = arguments[0];
                String genre = arguments[1];
                response = stub.getTopArtistsByUserGenre(userID, genre);
                runtimes[0] = (System.nanoTime()-start) / 1000000.0;
                runtimes[1] = response.executionTime / 1000000.0;
                runtimes[2] = response.waitingTime / 1000000.0;

                sumTimes = sumOfAllTimes.getOrDefault(method, new int[4]);
                sumTimes[0] ++;
                sumTimes[1] += runtimes[0];
                sumTimes[2] += runtimes[1];
                sumTimes[3] += runtimes[2];
                sumOfAllTimes.put(method, sumTimes);

                uProfile = new UserProfile(userID, null);
                uProfile.setTopArtistByGenre(genre, response.topThree);
                userCache.put(userID, uProfile);
                writeTopArtistsByUserGenre(genre, userID, response.topThree, runtimes, zone);
                break;
        }
    }

    /**
     * Retrieves a string with the arguments from the input query
     * @param query: a line in a query file
     * @return a string array with the arguments for the method in the query
     */
    public static String[] getArguments(String query) {
        Pattern pattern = Pattern.compile("(?<=\\()(.*?)(?=\\))");
        Matcher matcher = pattern.matcher(query);
        String arguments = "";
        if (matcher.find()) {
            arguments = matcher.group(1);
        }
        return arguments.split(",");
    }

    /**
     * Retrieves a string with the method from the input query
     * @param query: a line in a query file
     * @return a string with the name of the method which the query specifies
     */
    public static String getMethod(String query){
        Pattern pattern = Pattern.compile("^(.*?)(?=\\()");
        Matcher matcher = pattern.matcher(query);
        String method = "";
        if (matcher.find()) {
            method = matcher.group(1);
        }
        return method;
    }

    /**
     * Writes the result of invocations to output file
     * @param musicID: represents a song
     * @param timesPlayed: number of times a song has been played
     * @param runtimes: list of turnaround, execution and waiting time for invocation
     * @param zone: the zone of a server
     */
    public static void writeTimesPlayed(String musicID, int timesPlayed, double[] runtimes, int zone) {
        File file = new File(FILENAME);
        try {
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bf = new BufferedWriter(fw);
            bf.write("Music " + musicID + " was played " + timesPlayed + " times.");
            if (runtimes.length == 3) {
                bf.write("\n\t(turnaround time: " + runtimes[0] + " ms, execution time: " +
                  runtimes[1] + " ms, waiting time: " + runtimes[2] + " ms, processed by Server "
                  + zone + ")\n");
            } else {
                bf.write("\n\tProcessed by cache in " + runtimes[0] + " ms\n");
            }
            bf.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the result of invocations to output file
     * @param musicID: represents a song
     * @param timesPlayed: number of times a song has been played
     * @param userID: represents a user
     * @param runtimes: list of turnaround, execution and waiting time for invocation
     * @param zone: the zone of a server
     */
    public static void writeTimesPlayedByUser(String musicID, int timesPlayed, String userID, double[] runtimes, int zone) {
        File file = new File(FILENAME);
        try {
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bf = new BufferedWriter(fw);
            bf.write("Music " + musicID + " was played " + timesPlayed + " times by user "
                + userID + ".");
            if (runtimes.length == 3) {
                bf.write("\n\t(turnaround time: " + runtimes[0] + " ms, execution time: " +
                  runtimes[1] + " ms, waiting time: " + runtimes[2] + " ms, processed by Server "
                  + zone + ")\n");
            } else {
                bf.write("\n\tProcessed by cache in " + runtimes[0] + " ms\n");
            }
            bf.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the result of invocations to output file
     * @param userID: represents a user
     * @param topMusic: list of three most played songs for a user
     * @param runtimes: list of turnaround, execution and waiting time for invocation
     * @param zone: the zone of a server
     */
    public static void writeTopThreeMusicByUser(String userID, List<String> topMusic, double[] runtimes, int zone) {
        File file = new File(FILENAME);
        try {
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bf = new BufferedWriter(fw);
            bf.write("Top three musics for user " + userID + " were " + topMusic.get(0)
                + ", " + topMusic.get(1) + ", " + topMusic.get(2) + ".");
            if (runtimes.length == 3) {
                bf.write("\n\t(turnaround time: " + runtimes[0] + " ms, execution time: " +
                  runtimes[1] + " ms, waiting time: " + runtimes[2] + " ms, processed by Server "
                  + zone + ")\n");
            } else {
                bf.write("\n\tProcessed by cache in " + runtimes[0] + " ms\n");
            }
            bf.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the result of invocations to output file
     * @param genre: represents a music genre
     * @param userID: represents a user
     * @param topArtists: list of three most played artists within a genre for a user
     * @param runtimes: list of turnaround, execution and waiting time for invocation
     * @param zone: the zone of a server
     */
    public static void writeTopArtistsByUserGenre(String genre, String userID, List<String> topArtists, double[] runtimes, int zone) {
        File file = new File(FILENAME);
        try {
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bf = new BufferedWriter(fw);
            bf.write("Top three artists for genre " + genre + " and user " + userID + " were ");
            StringBuilder arts = new StringBuilder();
            for (String artist : topArtists) {
                arts.append(artist).append(", ");
            }
            bf.write(arts.substring(0, arts.length() - 2) + ".");
            if (runtimes.length == 3) {
                bf.write("\n\t(turnaround time: " + runtimes[0] + " ms, execution time: " +
                  runtimes[1] + " ms, waiting time: " + runtimes[2] + " ms, processed by Server "
                  + zone + ")\n");
            } else {
                bf.write("\n\tProcessed by cache in " + runtimes[0] + " ms\n");
            }
            bf.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates and writes the average time measurements of every method call
     * to output file
     */
    public static void writeAvgTimes() {
        File file = new File(FILENAME);
        try {
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bf = new BufferedWriter(fw);
            bf.write("\n");
            for (String method : sumOfAllTimes.keySet()) {
                int[] times = sumOfAllTimes.get(method);
                bf.write("\nAverage time used for method '" + method + "()' is:");
                bf.write("\n\t\tTurnaround: " + times[1]/times[0] + " ms");
                bf.write("\n\t\tExecution: " + times[2]/times[0] + " ms");
                bf.write("\n\t\tWaiting: " + times[3]/times[0] + " ms\n");
            }
            bf.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
