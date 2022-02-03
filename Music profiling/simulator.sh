# usage: ./clientSimulator x
# where 'x' is an int which
# represents the number of
# clients to start

if [ $# -ne 3 ]
then
    echo "Usage: ./simulator x smode cmode"
    echo "(where x is the number of clients to start,
smode and cmode are booleans, 1 means cached mode)"
    exit 0
fi

n=$1
smode=$2
cmode=$3

if [ $smode -eq 1 ]
then
    if [ $cmode -eq 1 ]
    then
        for ((i=1; i<=n; i++))
        do
            java Client $i $2 &
        done
    else
        for ((i=1;i<=n; i++))
        do
            java NaiveClient $i &
        done
    fi
else
    if [ $cmode -eq 1 ]
    then
        for ((i=1; i<=n; i++))
        do
            java Client $i &
        done
    else
        for ((i=1; i<=n; i++))
        do
            java NaiveClient $i -ns &
        done
    fi
fi
