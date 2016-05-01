FROM ubuntu:14.04
MAINTAINER Kishore

# Update & Install common packages
RUN apt-get update && apt-get install -y wget git curl zip && apt-get install -y software-properties-common

# Install a basic SSH server
RUN apt-get install -y openssh-server
RUN sed -i 's|session    required     pam_loginuid.so|session    optional     pam_loginuid.so|g' /etc/pam.d/sshd
RUN mkdir -p /var/run/sshd

######################################################## BUILD TOOLS #########################################################
# GIT
#####
RUN apt-get install -y git

# JAVA
############
RUN \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

# Define JAVA_HOME
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# MAVEN
#######
ENV MAVEN_VERSION 3.3.3
RUN mkdir -p /usr/share/maven \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    | tar -xzC /usr/share/maven --strip-components=1 \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

# Define MAVEN_HOME
ENV MAVEN_HOME /usr/share/maven

# Use tini as subreaper in Docker container to adopt zombie processes
COPY tini-static  /bin/tini
RUN  chmod +x /bin/tini  && echo "$TINI_SHA /bin/tini"

########################################################### JENKINS  ###########################################################
ENV JENKINS_VERSION 1.647
##########################
RUN curl -fsSL http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/${JENKINS_VERSION}/jenkins-war-${JENKINS_VERSION}.war -o /opt/jenkins.war
RUN chmod 644 /opt/jenkins.war

# Plugins
##########
ENV JENKINS_HOME /var/jenkins
ENV JENKINS_PLUGINS_LOCAL $JENKINS_HOME/plugins
#ENV JENKINS_UC https://updates.jenkins.io
ENV JENKINS_UC http://jenkins-updates.cloudbees.com
ENV COPY_REFERENCE_FILE_LOG $JENKINS_HOME/copy_reference_file.log
ENV JAVA_OPTS="-Xmx8192m"
ENV JENKINS_OPTS="--handlerCountStartup=100 --handlerCountMax=300  --webroot=/var/cache/jenkins/war"


# `/usr/share/jenkins/ref/` contains all reference configuration we want
# to set on a fresh new installation. Use it to bundle additional plugins
# or config file with your custom jenkins Docker image
RUN mkdir -p /usr/share/jenkins/ref/init.groovy.d/

########################################################### JENKINS  ###########################################################
ENV JENKINS_VERSION 1.647
##########################
RUN curl -fsSL http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/${JENKINS_VERSION}/jenkins-war-${JENKINS_VERSION}.war -o /opt/jenkins.war
RUN chmod 644 /opt/jenkins.war

# Jenkins Variables
ENV JENKINS_HOME /var/jenkins
ENV JENKINS_PLUGINS_LOCAL $JENKINS_HOME/plugins
ENV JENKINS_UC http://jenkins-updates.cloudbees.com
ENV JAVA_OPTS="-Xmx8192m"
ENV JENKINS_OPTS="--handlerCountStartup=100 --handlerCountMax=300  --webroot=/var/cache/jenkins/war"

# Create Directories
RUN mkdir -p /usr/share/jenkins/ref/
RUN mkdir -p $JENKINS_HOME
RUN mkdir -p $JENKINS_PLUGINS_LOCAL
RUN touch $JENKINS_HOME/copy_reference_file.log

# Plugins script add
COPY plugins.sh /usr/local/bin/plugins.sh
COPY plugins.txt /usr/share/jenkins/plugins.txt

# To install the plugins
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt

# Add user jenkins to the image
RUN adduser --quiet jenkins
# Set password for the jenkins user (you may want to alter this).
RUN echo "jenkins:jenkins" | chpasswd

# Standard SSH port
EXPOSE 22

# Connect to jenkins master
CMD ["/usr/sbin/sshd", "-D"]
