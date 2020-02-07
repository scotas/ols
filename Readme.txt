Compiling from Source.
- Checkout a definition for a build environment using Docker
$ git clone https://github.com/scotas/docker-images.git
$ cd docker-images/ols-dev-env
$ ./buildDockerImageR5.sh
$ cd ../..

- Build from sources
$ docker run -ti --rm ols-dev:2.0.5 bash
[oracle@3023358b5b05 home]$ cd solr
[oracle@3023358b5b05 solr]$ ant dist-core dist-solrj
.....
[oracle@3023358b5b05 solr]$ cd ../ols
[oracle@3023358b5b05 ols]$ ant package-zip
...
package-zip:
      [zip] Building zip: /home/lucene/build/lucene-ols-bin-2.0.5.zip

BUILD SUCCESSFUL
Total time: 2 minutes 14 seconds
[oracle@3023358b5b05 ols]$ 

- Compiling from live sources, Checkout Scotas OLS Source
$ git clone https://github.com/scotas/ols.git
$ docker run -ti -v "$(pwd)/ols:/home/ols" --rm ols-dev:2.0.5 bash
[oracle@3023358b5b05 home]$ cd solr
[oracle@3023358b5b05 solr]$ ant dist-core dist-solrj
.....
[oracle@3023358b5b05 solr]$ cd ../ols
[oracle@3023358b5b05 ols]$ ant package-zip
...
package-zip:
      [zip] Building zip: /home/lucene/build/lucene-ols-bin-2.0.5.zip

BUILD SUCCESSFUL
Total time: 2 minutes 14 seconds
[oracle@3023358b5b05 ols]$ 

Installing binary distribution using Docker
$ cd ../docker-images/sample-stacks
$ sudo mkdir -p /home/data/db/19c-ols
$ sudo chown 54321:54321 /home/data/db/19c-ols
$ curl -s https://raw.githubusercontent.com/scotas/docker-images/master/ols-scripts-r5/00-unzip-ols.sh | docker config create 00-unzip-ols.sh -
5eujc2f3aj0dbup3v77hm8043
$ curl -s https://raw.githubusercontent.com/scotas/docker-images/master/ols-scripts-r5/01-ols-ins.sh | docker config create 01-ols-ins.sh -
p07owsoxnd0m0z8xkkkjkip0i
$ curl -s https://raw.githubusercontent.com/scotas/docker-images/master/ols-scripts-r5/02-clean-up-ols-files.sh | docker config create 02-clean-up-ols-files.sh -
d6x4mo4o9szpoxn01ckwby8wc
$ docker stack deploy -c docker-compose-ols.yml ols
$ docker service logs -f ols_db
....

