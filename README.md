# Puppet Container Registry
Puppet Container Registry (formerly known as Europa from Distelli) makes it easy for software teams to host Docker images within their infrastructure along with a unified view of all their images stored in local and remote repositories.

<br/>
https://puppet.com/products/puppet-container-registry

### Getting Started

To get started with Community Edition you can pull the latest image from Docker Hub:

`docker pull distelli/europa:latest`

```shell
docker run -d --rm -p 3306:3306 --name=mysql -e MYSQL_DATABASE=pcr -e MYSQL_USER=pcr -e MYSQL_PASSWORD=password -e MYSQL_ROOT_PASSWORD=password mysql/mysql-server:latest
docker run --rm --name europa -e EUROPA_DB_ENDPOINT=mysql://172.17.0.2:3306/pcr -e EUROPA_DB_USER=pcr -e EUROPA_DB_PASS=password --link mysql:mysql -p 8080:80 -p 8443:443 distelli/europa:latest
```

Full documentation and a getting started guide is available at https://puppet.com/docs/container-registry/team/index.html

### Contribute

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to contribute issues, fixes, and patches to this project.

### Support

If any issues are encountered while using PCR Community Edition, please file an issue on the [GitHub issue tracker](github.com/puppetlabs/europa/issues).

### License

This project is distributed under [Apache License, Version 2.0](LICENSE).
