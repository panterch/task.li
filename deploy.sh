#!/bin/sh
mvn package && scp target/task.li-srv-webapp.war jetty@www.task.li:/usr/share/jetty/webapps/root.war && ssh jetty@www.task.li "sudo /etc/init.d/jetty restart"

