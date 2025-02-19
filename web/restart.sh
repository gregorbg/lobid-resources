#!/bin/sh

USAGE="\nusage: <GIT REPO NAME>
This script will just echo -en \033[1mSTOP\033[0m the process!
First parameter is mandatory.
The process is normally be monitored by 'monit'.
Monit will automatically restart it when it's stopped.
Have a look at /etc/monit/conf.d/play-instances.rc to see
which paramters are used (port, java opts etc.)
To see if monit is really observing your instances, try:
$ lynx localhost:2812
The username is 'admin', the password 'monit'.
If you want to stop it permanently via cmd, do this first:
$ sudo /etc/ini.d/monit stop
"

if [ ! $# -eq 1 ]; then
	echo "$USAGE"
	exit 65
fi

REPO=$1
HOME="/home/sol"

cd $HOME/git/$REPO/
mvn clean install -DskipTests=true
cd $HOME/git/$REPO/web
kill $(cat target/universal/stage/RUNNING_PID)
sleep 14
kill -9 $(cat target/universal/stage/RUNNING_PID)
rm target/universal/stage/RUNNING_PID
echo "Going to sleep for 11 seconds. Then lookup the process list for the repo name.
If everything is fine, 'monit' is going to start the $REPO instance ..."
sleep 11

ps -ef | grep $REPO
exit 0
