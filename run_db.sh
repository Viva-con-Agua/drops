docker run --name drops-mongo --restart=unless-stopped -p 27017:27017 -d mongo
docker run --name drops-mariadb -p 3306:3306 \
		-e MYSQL_ROOT_PASSWORD=drops \
    -e MYSQL_DATABASE=drops \
    -e MYSQL_USER=drops \
    -e MYSQL_PASSWORD=drops \
		-e MYSQL_ROOT_PASSWORD=yes \
    -d mariadb:latest
