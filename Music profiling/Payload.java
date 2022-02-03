import java.util.ArrayList;
import java.io.Serializable;

public class Payload implements Serializable {
    public int timesPlayed;             // This field contains the answer for both int methods
    public ArrayList<String> topThree;  // This field contains the answer for both ArrayList<String> methods
    public long executionTime;
    public long waitingTime;

    Payload(int timesPlayed, ArrayList<String> topThree, long executionTime,
            long waitingTime) {
        this.timesPlayed = timesPlayed;
        this.topThree = topThree;
        this.executionTime = executionTime;
        this.waitingTime = waitingTime;
    }

}
