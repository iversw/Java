# Replicated bank account
A distributed system where multiple clients all share the same transactions via a daemon, ensuring they all are in the same state.

## Requirements:
Java spread toolkit is necessary for hosting the daemon which the account replicas will connect to.

http://www.spread.org/docs/javadocs/java.html

## Notes
This system will connect several clients(replicas) to the same simulated account, allowing for the transactions they perform to influence each others' balances, as they effectively are the same account represented on different clients.

The distributed system can be run by opening up a terminal for each client you wish to run. To start a client, 
simply run AccountReplica with the following arguments (assumes you have a folder named “spread” with 
Spread’s java files in the same directory):
1. Server address.
2. Account name
3. Number of replicas (keep this the same for all clients)
4. Filename of input file (optional)
Running without a filename argument will allow for inputs to be sent through the command line. The clients will
wait until the specified number of replicas has connected. All clients can receive inputs.
