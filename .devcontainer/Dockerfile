# Before building this image oracle/database:19.3.0-ee must be available at your private registry
# See https://github.com/marcelo-ochoa/docker-images/blob/master/OracleDatabase/README.md for more details
# Finally build using ./buildDockerImage.sh script
ARG ORACLE_VERSION
ARG ORACLE_RU
ARG ORACLE_IMAGE
FROM  "${ORACLE_IMAGE}:${ORACLE_VERSION}.${ORACLE_RU}" AS rdbms_release

FROM alpine:latest AS solr40source
LABEL maintainer="Marcelo Ochoa <mochoa@scotas.com>"

RUN apk add --update subversion curl && rm -rf /var/cache/apk/*

WORKDIR /src
RUN	svn co http://svn.apache.org/repos/asf/lucene/dev/branches/lucene_solr_4_0
ADD	ols-solr-lucene.patch /src/lucene_solr_4_0/
WORKDIR /src/lucene_solr_4_0
RUN	svn patch /src/lucene_solr_4_0/ols-solr-lucene.patch

FROM alpine/git:latest AS olssource
LABEL maintainer="Marcelo Ochoa <mochoa@scotas.com>"
ARG REPO_URL
ENV REPO_URL ${REPO_URL:-https://github.com/scotas/ols.git}
ARG BRANCH
ENV BRANCH ${BRANCH}

WORKDIR /src

# Clone GitHub Repo from an specific branch
RUN git clone -b $BRANCH $REPO_URL

FROM  oraclelinux:8-slim
LABEL maintainer="Marcelo Ochoa <mochoa@scotas.com>"

USER root
RUN microdnf install oracle-instantclient-release-23ai-el8 zip unzip libnsl libaio curl tar && \
    rpm -Uvh https://packages.microsoft.com/config/centos/8/packages-microsoft-prod.rpm && \
    dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo && \
    microdnf install --enablerepo ol8_appstream --enablerepo ol8_addons --enablerepo ol8_codeready_builder \
    	oracle-instantclient-basic oracle-instantclient-devel oracle-instantclient-sqlplus oracle-instantclient-jdbc \
        ant ant-contrib ant-junit apache-ivy git docker-ce java-21-openjdk-devel --nobest && \
    curl -sSL https://dot.net/v1/dotnet-install.sh | bash -s -- --runtime aspnetcore --version 3.1.32 --install-dir /usr/share/dotnet && \
    ln -s /usr/share/dotnet/dotnet /usr/bin/dotnet && \
    echo 2 | alternatives --config java && \
    groupadd -g 54321 oinstall && useradd -g oinstall -u 54321 -N -G docker -m oracle && \
    echo 'export ORACLE_HOME=$(dirname $(dirname `readlink -f /usr/bin/sqlplus`))' >> /home/oracle/.bashrc && \
    echo 'export LD_LIBRARY_PATH=$ORACLE_HOME/lib:$LD_LIBRARY_PATH' >> /home/oracle/.bashrc && \
    echo 'export PATH=$ORACLE_HOME/bin:$PATH' >> /home/oracle/.bashrc && \
    echo 'export JAVA_HOME=$(dirname $(dirname `readlink -f /etc/alternatives/java`))' >> /home/oracle/.bashrc && \
    rm -rf /var/tmp/* && \
    microdnf clean all

# Source of Apache Solr/Lucene
COPY --from=solr40source --chown=oracle:oinstall /src/lucene_solr_4_0/ /home/oracle/lucene_solr_4_0/

# Source of Scotas OLS, use Git Folder local from docker-compose.yml
# COPY --from=olssource --chown=oracle:oinstall /src/ols/ /home/ols/

ARG ORACLE_VERSION
ARG ORACLE_PDB
ENV ORACLE_PDB ${ORACLE_PDB}
ARG SRC_HOME
ENV SRC_HOME ${SRC_HOME}
ARG RELEASE
ENV RELEASE ${RELEASE}

# RDBMS Required Libs
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/rdbms/jlib/ODCI.jar" /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/sqlj/lib/runtime12.jar" /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/rdbms/jlib/CartridgeServices.jar" /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/rdbms/jlib/xdb.jar" /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/lib/xmlparserv2.jar" /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/javavm/lib/aurora.zip" /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/rdbms/jlib/servlet.jar" /home/oracle/
# Required bin
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/bin/wrap"  /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/lib/libclntsh.so.${ORACLE_VERSION}.1"  /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/lib/libclntshcore.so.${ORACLE_VERSION}.1"  /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/lib/libmql1.so"  /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/lib/libipc1.so"  /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/lib/libnnz.so"  /home/oracle/
COPY --from=rdbms_release --chown=oracle:oinstall "${SRC_HOME}/lib/libons.so"  /home/oracle/

ADD  https://github.com/scotas/ols/releases/download/$RELEASE/valid.ols /home/oracle/

RUN ln -s /home/oracle/lucene_solr_4_0/solr /home/solr && \
    ln -s /home/oracle/lucene_solr_4_0/lucene /home/lucene && \
    chown -R oracle:oinstall /home/oracle

# Required libs for compiling
RUN export ORACLE_HOME=$(dirname $(dirname `readlink -f /usr/bin/sqlplus`)) && \
    mkdir -p $ORACLE_HOME/rdbms/jlib/ && \
    mkdir -p $ORACLE_HOME/rdbms/admin/ && \
    mkdir -p $ORACLE_HOME/jdbc/lib/ && \
    mkdir -p $ORACLE_HOME/sqlj/lib/ && \
    mkdir -p $ORACLE_HOME/javavm/lib/ && \
    mkdir -p $ORACLE_HOME/network/admin/ && \
    mv /home/oracle/ODCI.jar $ORACLE_HOME/rdbms/jlib/ODCI.jar && \
    mv /home/oracle/servlet.jar $ORACLE_HOME/rdbms/jlib/servlet.jar && \
    mv /home/oracle/runtime12.jar $ORACLE_HOME/sqlj/lib/runtime12.jar && \
    mv /home/oracle/CartridgeServices.jar $ORACLE_HOME/rdbms/jlib/CartridgeServices.jar && \
    mv /home/oracle/xdb.jar $ORACLE_HOME/rdbms/jlib/xdb.jar && \
    mv /home/oracle/xmlparserv2.jar $ORACLE_HOME/lib/xmlparserv2.jar && \
    mv /home/oracle/aurora.zip $ORACLE_HOME/javavm/lib/aurora.zip && \
    mv /home/oracle/wrap $ORACLE_HOME/bin && \
    mv /home/oracle/libclntsh.so.${ORACLE_VERSION}.1 $ORACLE_HOME/lib && \
    mv /home/oracle/libclntshcore.so.${ORACLE_VERSION}.1 $ORACLE_HOME/lib && \
    mv /home/oracle/libmql1.so $ORACLE_HOME/lib && \
    mv /home/oracle/libipc1.so $ORACLE_HOME/lib && \
    mv /home/oracle/libnnz.so $ORACLE_HOME/lib && \
    mv /home/oracle/libons.so $ORACLE_HOME/lib && \
    mkdir -p /home/ols/bin/com/scotas/license && \
    mv /home/oracle/valid.ols /home/ols/bin/com/scotas/license && \
    ln -s $ORACLE_HOME/lib/ojdbc8.jar $ORACLE_HOME/jdbc/lib/ojdbc8.jar && \
    ln -s $ORACLE_HOME/lib/ojdbc8.jar $ORACLE_HOME/jdbc/lib/ojdbc6.jar && \
    echo "${ORACLE_PDB}= \
(DESCRIPTION = \
  (ADDRESS = (PROTOCOL = TCP)(HOST = db)(PORT = 1521)) \
  (CONNECT_DATA = \
    (SERVER = DEDICATED) \
    (SERVICE_NAME = ${ORACLE_PDB}) \
  ) \
)" >> $ORACLE_HOME/network/admin/tnsnames.ora

WORKDIR	/home/ols
USER	oracle

# Define default command to start Oracle Database. 
CMD	exec /bin/bash
