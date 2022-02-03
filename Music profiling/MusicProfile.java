import java.util.ArrayList;

public class MusicProfile {

    private final String musicId;
    private final String genre;
    private final int timesPlayed;
    private final int totalTimesPlayed;
    private final ArrayList<String> artists;

    public MusicProfile(String musicId, String genre, int timesPlayed, int totalTimesPlayed, ArrayList<String> artists) {
        this.musicId = musicId;
        this.genre = genre;
        this.timesPlayed = timesPlayed;
        this.artists = artists;
        this.totalTimesPlayed = totalTimesPlayed;
    }

    public String getMusicId() {
        return musicId;
    }

    public ArrayList<String> getArtists() {
        return artists;
    }

    public int getTimesPlayed() {
        return timesPlayed;
    }

    public int getTotalTimesPlayed() {
        return totalTimesPlayed;
    }

    public String getGenre() {
        return genre;
    }

}