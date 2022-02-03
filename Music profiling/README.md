# Implementation notes
The distributed system is split between three main parts. Client, Proxy and Server. The client(s)
objective is to read queries from an input file, request a server from the proxy and write responses from
these queries to an output file. All clients have a reference to the same proxy. The proxy will perform
load balancing between the servers by checking whether the servers are free before sending a new job.
Every 10 requests to the same server, the Proxy will update its information about this Server’s current
jobs. Remote methods are handled by threads server side. When a remote method is called, a job object
is handed to a thread pool, consisting of a single execution thread and a waiting thread. When a job is
finished, the result is returned, and the oldest job from the waiting list will start being executed.
There are two different classes of client, one with a cache, and one without. When running a client with
a cache, the client will check its cache for requested information before asking the proxy for a server. If
the result is found in cache, none of the times specified in the assignment text will be available, and
thus not written to the output file. Instead, it will say that the request was processed by cache. The
client will send jobs sequentially, and wait for a response before sending the next. In order to stress test
the proxy / servers, it is necessary to run several clients simultaneously. After the queries are completed,
an output file will be written with information about which queries were retrieved from cache, as well as
the speed of the execution.

# Compiling / setup
No packages etc are used, so just do a simple javac *.java to compile. Running the system
requires a bit more setup. You will need 3 terminals open.
1. In one terminal, first start the registry, then run ServerSimulator. Running ServerSimulator with
a -n argument will create naive servers. Running without the flag creates servers with a cache.
2. Then, start the proxy in a different terminal.
3. Once the proxy is running, either run a NaiveClient, Client(cached) or the simulator.sh script.
The client programs will run a single client.

# Running the bash script
Running the script:
The script should be run with 3 arguments, all numbers. The first indicates how many clients you want
to start simultaneously, the second should be a 0 or 1, indicating whether you are running with cached
servers. The last number is the same, but for clients.
Example: “simulator.sh 2 1 0” will run 2 naive clients and assumes server-side caching.

Note: the arguments for server caching in the script is only to determine output file name, not what
mode the servers are in. Determining server mode has to be done when running ServerSimulator, as
described above.
