import java.io.Serializable;

/**
 * Class representing a transaction
 */

public class Transaction implements Serializable {
    String command;
    String uniqueId;
    String clientName;
  
    Transaction(String command, String clientName, int outstandingCounter) {
        uniqueId = clientName + outstandingCounter;
        this.command = command;
        this.clientName = clientName;
    }

}
