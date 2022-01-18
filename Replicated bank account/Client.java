import spread.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.lang.InterruptedException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Client implements AdvancedMessageListener {

    private SpreadConnection connection;
    private final String serverAddress;
    private final String accountName;
    private List<Transaction> executedList;
    private List<Transaction> outstandingList;
    private int outstandingCounter;
    private final int port = 4803; //Change to appropriate port
    private double balance;
    private static SpreadGroup spreadGroup;
    private String clientName;
    private String fileName;
    private Set<String> memberSet;
    private final int numClients;
    private Lock reentrantLock = new ReentrantLock();

    public Client(String serverAddress, String accountName, String numClients, String fileName) {
        this.serverAddress = serverAddress;
        this.accountName = accountName;
        executedList = new ArrayList<>();
        outstandingList = new ArrayList<>();
        outstandingCounter = 0;
        this.balance = 0.0;
        memberSet = new HashSet<>();
        this.numClients = Integer.parseInt(numClients);

        System.out.println("Client starting!");
        if (fileName != null) {
            this.fileName = fileName;
        }
        init();
    }

    /**
     * Connects client to server and waits for all clients
     */
    private void init() {
        try {
            establishConnection(serverAddress, accountName);
            waitForClients();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    /**
     * Connecting a client to the spread daemon
     * @param serverAddress string of the address to the server
     * @param accountName   string of the name of the server
     */
    private void establishConnection(String serverAddress, String accountName) {
        System.out.println("Attempting to connect to: " + serverAddress + " with account: " + accountName);
        try {
            connection = new SpreadConnection();
            connection.connect(InetAddress.getByName(serverAddress), port, UUID.randomUUID().toString(), false, true);
            connection.add(this);
            joinSpreadGroup(connection, accountName);
            clientName = connection.getPrivateGroup().toString();
            System.out.println("\tClient " + clientName + " joined successfully");
        } catch (UnknownHostException | SpreadException e) {
            System.out.println("\tFailed to establish connection: " + e + ": " + e.getMessage());
        }
    }

    /**
     * Joins a group of clients in the connection
     * @param connection    connection to the daemon
     * @param accountName   string of the name of the server
     */
    private static void joinSpreadGroup(SpreadConnection connection, String accountName) {
        try {
            spreadGroup = new SpreadGroup();
            spreadGroup.join(connection, accountName);
        } catch (SpreadException e) {
            System.out.println("Failed to join spread group: " + e);
        }
    }

    /**
     * Causes the client to sleep until all clients has joined the group and starts
     * execution of commands
     * @throws InterruptedException
     */
    public void waitForClients() throws InterruptedException {
        System.out.println("Waiting for clients " + memberSet.size() + "/" + numClients);
        while (memberSet.size() < numClients) {
            Thread.sleep(500);
        }
        startBroadcastingUpdates();
        if (fileName != null) {
            executeCommandsFromFile(fileName);
        } else {
            executeCommands();
        }
    }

    /**
     * Adds or removes clients to the group
     */
    @Override
    public void membershipMessageReceived(SpreadMessage message) {
        MembershipInfo membershipInfo = message.getMembershipInfo();
        if (membershipInfo.isCausedByJoin()) {
            Arrays.stream(message.getMembershipInfo().getMembers()).forEach(member -> memberSet.add(member.toString()));
            System.out.println("Waiting for clients " + memberSet.size() + "/" + numClients);
        }

        if (membershipInfo.isCausedByLeave() || membershipInfo.isCausedByDisconnect()) {
            this.memberSet.remove(membershipInfo.getLeft().toString());
        }
    }

    /**
     * Creates a threadpool which only job is to update the replicas every 10 seconds
     */
    private void startBroadcastingUpdates() {
        Runnable broadcastRunnable = this::updateReplicas;
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(0);
        executor.scheduleAtFixedRate(broadcastRunnable, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * Multicasts a message to the entire group of clients
     */
    private void updateReplicas() {
        reentrantLock.lock();
        try {
            SpreadMessage message = new SpreadMessage();
            message.setReliable();
            message.setSafe();
            message.addGroup(spreadGroup);

            Update update = new Update(outstandingList, memberSet);
            message.digest(update);
            connection.multicast(message);
        } catch (SpreadException e) {
            e.printStackTrace();
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * Removes a processed transaction from outstandingList and adds it to executedList
     * @param transaction   transaction that has been processed
     */
    private void updateLists(Transaction transaction) {
        List<Transaction> copy = new ArrayList<Transaction>(outstandingList);
        for (int i = 0; i < copy.size(); i++) {
          if (copy.get(i).uniqueId.equals(transaction.uniqueId)) {
            outstandingList.remove(i);
          }
        }
        executedList.add(transaction);
    }

    /**
     * Immediately prints the current balance
     */
    public void getQuickBalance() {
        System.out.println("QuickBalance: " + balance);
    }

    /*
     * Sleeps until outstandingList is empty and prints the current balance
     */
    public void getSyncedBalanceNaive() {
        try {
            while (outstandingList.size() != 0) {
                Thread.sleep(500);
            }
            System.out.println("Naive balance: " + balance);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the balance after syncronization and removes the transaction from the list
     * @param transaction   transaction of the command
     */
    public void getSyncedBalance(Transaction transaction) {
        System.out.println("Synced balance: " + balance);
    }

    /**
     * Adds a input amount to the balance
     * @param amount    int to add to balance
     * @param transaction   transaction of the command
     */
    public void deposit(double amount, Transaction transaction) {
        System.out.println("Adding " + amount + " to balance: " + balance);
        this.balance += amount;
        updateLists(transaction);
    }

    /**
     * Adds a input percentage to the balance
     * @param percent   int to add the percentage of
     * @param transaction   transaction of the command
     */
    public void addInterest(double percent, Transaction transaction) {
        System.out.println("Adding " + percent + "% to balance: " + balance);
        this.balance = this.balance + ((this.balance / 100) * percent);
        updateLists(transaction);
    }

    /**
     * Prints all the previously processed transactions
     */
    public void getHistory() {
        System.out.println("Recent transactions:");
        for (Transaction t : executedList) {
            System.out.println("\tID: " + t.uniqueId + " command: " + t.command);
        }
    }

    /**
     * Prints whether a transaction has been executed or not
     * @param uniqueId  the ID of a transaction
     */
    public void checkTxStatus(String uniqueId) {
        System.out.println("Checking ID: " + uniqueId);
        if (executedList.stream().anyMatch(transaction -> transaction.uniqueId.equals(uniqueId))) {
            System.out.println(" Transaction ID: " + uniqueId + " has been executed.");
        } else if (executedList.stream().anyMatch(transaction -> transaction.uniqueId.equals(uniqueId))) {
            System.out.println(" Transaction ID: " + uniqueId + " has not been executed.");
        } else {
            System.out.println(" Cannot find transaction ID");
        }
    }

    /**
     * Empties the list of previous transactions
     */
    public void cleanHistory() {
        System.out.println("Cleaning history...");
        executedList.clear();
    }

    /**
     * Prints the name of all current members in the group
     */
    public void memberInfo() {
        System.out.println("Printing member info...");
        for (String member : memberSet) {
            System.out.println("\tMember name: " + member);
        }
    }

    /**
     * Causes the client to sleep for a given duration
     * @param duration  int for duration of sleep
     */
    public void sleep(int duration) {
        System.out.println("Sleeping for " + duration + " seconds.");
        try {
            Thread.sleep((duration * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Kills the client
     */
    public void exit() {
        System.out.println("Shutting down...");
        try {
            if (connection != null) {
                System.out.println("Disconnecting client.");
                connection.disconnect();
                connection.remove(this);
            } else {
                System.out.println("Clients already disconnected.");
            }
        } catch (SpreadException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * Updates clients when a message is received and sends tasks to execution
     */
    @Override
    public void regularMessageReceived(SpreadMessage message) {
        reentrantLock.lock();
        try {
            if (message.getDigest().get(0) instanceof Update) {
                Update update = (Update) message.getDigest().get(0);
                this.memberSet = update.getMemberSet();
                for (Transaction transaction : update.getOutstandingList()) {
                    if (transaction.command.equals("getSyncedBalance") && !(update.getOutstandingList().get(0) == transaction)){
                        return;
                    } else {
                        String[] command = transaction.command.split(" ");
                        if (command.length == 2) {
                            executeTasks(command[0], command[1], transaction);
                        }

                        if (command.length == 1) {
                            executeTasks(command[0], null, transaction);
                        }
                    }
                }
            }
        } catch (SpreadException e) {
            e.printStackTrace();
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * Switch cases of the different commands
     * @param command   input command
     * @param arg   value of the command if needed, e.g. for deposits
     * @param transaction   transaction to be processed
     */
    private void executeTasks(String command, String arg, Transaction transaction) {
        switch (command) {
            case "getQuickBalance": {
                getQuickBalance();
                break;
            }
            case "getSyncedBalanceNaive": {
                getSyncedBalanceNaive();
                break;
            }
            case "getSyncedBalance": {
              List<Transaction> copy = new ArrayList<Transaction>(outstandingList);
              for (int i = 0; i < copy.size(); i++) {
                if (copy.get(i).uniqueId.equals(transaction.uniqueId)) {
                  outstandingList.remove(i);
                }
              }
                if (transaction.clientName.equals(clientName)) {
                    getSyncedBalance(transaction);
                    //getSyncedBalanceNaive();
                }
                break;
            }
            case "deposit": {
                deposit(Double.parseDouble(arg), transaction);
                break;
            }
            case "addInterest": {
                addInterest(Double.parseDouble(arg), transaction);
                break;
            }
            case "getHistory": {
                getHistory();
                break;
            }
            case "checkTxStatus": {
                checkTxStatus(arg);
                break;
            }
            case "cleanHistory": {
                cleanHistory();
                break;
            }
            case "memberInfo": {
                memberInfo();
                break;
            }
            case "sleep": {
                sleep(Integer.parseInt(arg));
                break;
            }
            case "exit": {
                exit();
                break;
            }
        }
    }

    /**
     * Method for adding transactions of either deposit/addInterest or getSyncedBalance
     * @param command   command of the transaction
     * @param arg   value of the command or "" for synced balance
     */
    private void addTransaction(String command, String arg) {
        Transaction transaction;
        if (!arg.equals("")) {
            transaction = new Transaction(command + " " + arg, clientName, outstandingCounter);
        } else {
            transaction = new Transaction(command, clientName, outstandingCounter);
        }

        outstandingList.add(transaction);
        outstandingCounter++;
    }

    /**
     * Process a command and gives sends it to the appropriate method
     * @param command   array of commands
     * @param isFile    whether we are processing an input file or not
     */
    private void handleCommand(String[] command, boolean isFile) {
        if (enumContains(command[0])) {
            if (command.length == 1) {
                if (isTransaction(command[0])) {
                    addTransaction(command[0], "");
                } else {
                    executeTasks(command[0], null, null);
                }

            }
            if (command.length == 2) {
                if (isTransaction(command[0])) {
                    addTransaction(command[0], command[1]);
                } else {
                    executeTasks(command[0], command[1], null);
                }
            }
            if (command.length > 2) {
                System.out.println("Commands take only 0-1 arguments");
            }
        } else {
            System.out.println("Invalid command!");
        }
        if (!isFile) {
            executeCommands();
        }
    }

    /**
     * Handles commands from commandline
     */
    private void executeCommands() {
        System.out.println("Type command:");
        Scanner scanner = new Scanner(System.in);
        String[] command = scanner.nextLine().split(" ");
        handleCommand(command, false);
    }

    /**
     * Handles commands from input file
     * @param fileName  name of input file
     */
    private void executeCommandsFromFile(String fileName) {
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                String[] command = line.split(" ");
                handleCommand(command, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks whether a command should be a transaction obj
     * @param command   input command
     * @return  true if command should be a transaction
     */
    private boolean isTransaction(String command) {
        return command.equals("deposit") || command.equals("addInterest") || command.equals("getSyncedBalance");
    }

    /**
     * Ensures that the input is valid
     * @param command   input command
     * @return  true if command is valid
     */
    private boolean enumContains(String command) {
        String[] commandAndArg = command.split(" ");
        for (Commands c : Commands.values()) {
            if (c.name().contains(commandAndArg[0])) {
                return true;
            }
        }
        return false;
    }
}
