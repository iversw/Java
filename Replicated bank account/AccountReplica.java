public class AccountReplica {

    //args[0] = serverAddress
    //args[1] = accountName
    //args[2] = numClients
    //args[3] = (optional) fileName
    public static void main(String[] args) {
        
        if(args.length == 3) {
            setUp(args[0], args[1], args[2], null);
        } else if (args.length == 4) {
            setUp(args[0], args[1], args[2], args[3]);
        } else {
            System.out.println("Invalid number arguments. Only " + args.length + " arguments supplied:");
            for (String arg : args) {
                System.out.println(arg);
            }
        }
    }

    /**
     * Creates the wanted number of clients 
     * @param serverAddress address of the server
     * @param accountName   server name
     * @param num   number of clients
     * @param fileName  optional input filename
     */
    private static void setUp(String serverAddress, String accountName, String num, String fileName) {
        if (serverAddress != null && accountName != null && num != null) {
            int numClients = Integer.parseInt(num);
            System.out.println("Setting up " + numClients + " client(s)");
            if (fileName != null) {
                new Client(serverAddress, accountName, num,  fileName);
            } else {
                new Client(serverAddress, accountName, num,  null);
            }
        } else {
            System.out.println("Usage: Java AccountReplica <server address> <account name> <number of replicas> [file name]");
        }
    }

}
