# Puppet Container Registry
Puppet Container Registry (formerly known as Europa from Distelli) makes it easy for software teams to host Docker images within their infrastructure along with a unified view of all their images stored in local and remote repositories.

<br/>
https://puppet.com/products/puppet-container-registry

### Editions

There are three editions of Puppet Container Registry (PCR):

- Community Edition (CE) is available freely under the Apache 2.0 license.
- Premium Edition (PE) includes extra features that are useful for small teams
- Enterprise Edition (EE) includes even more feautures (Teams, SAML, Service Accounts) useful for enterprises.

For pricing and support for PCR Premium & Enterprise Editions please visit https://puppet.com/products/puppet-container-registry.

### Features

<ul>
  <li>Push and pull images, securely, from the privacy of your own network.</li>
  <li>Choose where to store your images from a variety of options, including S3 and local disk.</li>
  <li>Support for Docker v2 API.</li>
  <li>Support for connecting and synchronizing to other external Docker repositories.</li>
  <li>Audit trails for repositories with a history of every action.</li>
  <li>Automated push pipelines allowing the redundant push of images to multiple downstream repositories.</li>
  <li>Single Sign-on via a SAML IDP</li>
  <li>Fine Grained Access Control</li>
  <li>Teams.</li>
</ul>

<table>
  <tr><th><br>Feature</th><th style="text-align:center">Community</th><th style="text-align:center">Premium</th><th style="text-align:center">Enterprise</th></tr>
  <tr><td>Open Source</td>             <td style="text-align:center">Yes</td><td style="text-align:center">.</td><td style="text-align:center">.</td></tr>
  <tr><td>Local Repositories</td>      <td style="text-align:center">Yes</td><td style="text-align:center">Yes</td><td style="text-align:center">Yes</td></tr>
  <tr><td>Remote Repositories</td>     <td style="text-align:center">Yes</td><td style="text-align:center">Yes</td><td style="text-align:center">Yes</td></tr>
  <tr><td>Automated Push Pipelines</td><td style="text-align:center">Yes</td><td style="text-align:center">Yes</td><td style="text-align:center">Yes</td></tr>
  <tr><td>Multi-user Support</td>      <td style="text-align:center">.</td><td style="text-align:center">Yes</td><td style="text-align:center">Yes</td></tr>
  <tr><td>Access Control</td>          <td style="text-align:center">.</td><td style="text-align:center">Yes</td><td style="text-align:center">Yes</td></tr>
  <tr><td>Single Sign-on</td>          <td style="text-align:center">.</td><td style="text-align:center">.</td><td style="text-align:center">Yes</td></tr>
  <tr><td>Teams</td>                   <td style="text-align:center">.</td><td style="text-align:center">.</td><td style="text-align:center">Yes</td></tr>
</table>


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
