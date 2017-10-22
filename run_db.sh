docker run --name drops-mongo --restart=unless-stopped -d mongo
docker run --name drops-mariadb \
		-e MYSQL_ROOT_PASSWORD=drops \
    -e MYSQL_DATABASE=drops \
    -e MYSQL_USER=drops \
    -e MYSQL_PASSWORD=drops \
		-e MYSQL_ROOT_PASSWORD=yes \
    -d mariadb:latest
