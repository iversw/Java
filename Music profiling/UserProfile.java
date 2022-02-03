import java.util.ArrayList;
import java.util.HashMap;

public class UserProfile {

    private String userId;
    private ArrayList<String> topThreeMusicProfiles;
    private HashMap<String, ArrayList<String>> topArtistByGenre;
    private HashMap<String, MusicProfile> musicProfiles;


    public UserProfile(String userId, ArrayList<String> topThreeMusicProfiles) {
        this.userId = userId;
        this.topThreeMusicProfiles = topThreeMusicProfiles;
        this.topArtistByGenre = new HashMap<>();
        this.musicProfiles = new HashMap<>();
    }

    public String getUserId() {
        return userId;
    }

    public ArrayList<String> getTopThreeMusicProfiles() {
        return topThreeMusicProfiles;
    }

    public void setTopThreeMusicProfiles(ArrayList<String> topThreeMusicProfiles) {
        this.topThreeMusicProfiles = topThreeMusicProfiles;
    }

    public ArrayList<String> getTopArtistsByGenre(String genre) {
        return topArtistByGenre.getOrDefault(genre, null);
    }

    public void setTopArtistByGenre(String genre, ArrayList<String> topArtistByGenre) {
        this.topArtistByGenre.put(genre, topArtistByGenre);
    }

    public void addMusicProfile(MusicProfile profile) {
        musicProfiles.put(profile.getMusicId(), profile);
    }

    public MusicProfile getMusicProfile(String musicId) {
        return musicProfiles.get(musicId);
    }

}
