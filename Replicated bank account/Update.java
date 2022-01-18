import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class Update implements Serializable {
    private List<Transaction> outstandingList;
    private Set<String> memberSet;

    public Update (List<Transaction> outstandingList, Set<String> memberSet) {
        this.outstandingList = outstandingList;
        this.memberSet = memberSet;
    }

    public List<Transaction> getOutstandingList() {
        return this.outstandingList;
    }

    public Set<String> getMemberSet() {
        return this.memberSet;
    }



}
