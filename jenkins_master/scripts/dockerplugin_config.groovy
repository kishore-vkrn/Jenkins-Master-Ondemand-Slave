import jenkins.model.*
import hudson.model.*

import com.cloudbees.plugins.credentials.Credentials
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.cloudbees.plugins.credentials.SystemCredentialsProvider

import com.nirima.jenkins.plugins.docker.*
import com.nirima.jenkins.plugins.docker.launcher.*
import com.nirima.jenkins.plugins.docker.strategy.*

//def dockerCertificatesDirectory = System.getenv('DOCKER_CERTIFICATES_DIRECTORY')

def dockerCertificatesDirectoryCredentialsId = 'docker-certificates-credentials'
def jenkinsSlaveCredentialsId = '164395'
def Scope
def image
def labelString
def remoteFs
def credentialsId
def idleTerminationMinutes
def sshLaunchTimeoutMinutes
def jvmOptions
def javaPath
def memoryLimit
def memorySwap
def cpuShares
def prefixStartSlaveCmd
def suffixStartSlaveCmd
def instanceCapStr
def dnsString
def dockerCommand
def volumesString
def volumesFromString
def hostname
def bindPorts
def bindAllPorts
def privileged
def tty
def macAddress

///////////////////////////////////////////////////:
// Configure credz
///////////////////////////////////////////////////:

SystemCredentialsProvider system_creds = SystemCredentialsProvider.getInstance()
Boolean foundId=false
system_creds.getCredentials().each{
    if(jenkinsSlaveCredentialsId.equals(it.getId())) {
        foundId=true
    }
}

if(!foundId) {
    Map<Domain, List<Credentials>> domainCredentialsMap = system_creds.getDomainCredentialsMap()
    UsernamePasswordCredentialsImpl creds =
            new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM,
                    jenkinsSlaveCredentialsId,
                    'Slave credentials for SSH',
                    'jenkins',
                    'jenkins')
    domainCredentialsMap[Domain.global()].add(creds)
    system_creds.save()
    println 'Added docker cloud credentials.'
}


system_creds.save()
println 'Added docker cloud credentials.'

/////////////////////////////////////////////////////:
// Docker Cloud config per-se
/////////////////////////////////////////////////////:
//def swarmMasterUrl = System.getenv("SWARM_MASTER_URL")
//assert swarmMasterUrl != null : "SWARM_MASTER_URL env var not set!"

def docker_settings = [:]
docker_settings =
        [
                [
                        name: 'AWS Swarm',
                        serverUrl: 'http://52.202.83.90:2375',
                        containerCapStr: '300',
                        connectionTimeout: 5,
                        readTimeout: 15,
                        credentialsId: dockerCertificatesDirectoryCredentialsId,
                        version: '',
                        templates: [
                                [
                                        image: 'jenkins_slave',
                                        labelString: 'On-demand-Slaves',
                                        remoteFs: '',
                                        credentialsId: jenkinsSlaveCredentialsId,
                                        idleTerminationMinutes: '2',
                                        sshLaunchTimeoutMinutes: '1',
                                        jvmOptions: '',
                                        javaPath: '',
                                        memoryLimit: 2500,
                                        memorySwap: 0,
                                        cpuShares: 0,
                                        prefixStartSlaveCmd: '',
                                        suffixStartSlaveCmd: '',
                                        instanceCapStr: '200',
                                        dnsString: '',
                                        dockerCommand: '',
                                        volumesString: '',
                                        volumesFromString: '',
                                        hostname: '',
                                        bindPorts: '',
                                        bindAllPorts: false,
                                        privileged: false,
                                        tty: false,
                                        macAddress: ''
                                ]
                        ]
                ]
        ]

def dockerClouds = []
docker_settings.each { cloud ->

    def templates = []
    cloud.templates.each { template ->
        def dockerTemplateBase =
                new DockerTemplateBase(
                        template.image,
                        template.dnsString,
                        template.dockerCommand,
                        template.volumesString,
                        template.volumesFromString,
                        template.environmentsString,
                        template.lxcConfString,
                        template.hostname,
                        template.memoryLimit,
                        template.memorySwap,
                        template.cpuShares,
                        template.bindPorts,
                        template.bindAllPorts,
                        template.privileged,
                        template.tty,
                        template.macAddress
                )

        def dockerTemplate =
                new DockerTemplate(
                        dockerTemplateBase,
                        template.labelString,
                        template.remoteFs,
                        template.remoteFsMapping,
                        template.instanceCapStr
                )

        def dockerComputerSSHLauncher = new DockerComputerSSHLauncher(

                new hudson.plugins.sshslaves.SSHConnector(22, template.credentialsId, null, null, null, null, null )
        )

        dockerTemplate.setLauncher(dockerComputerSSHLauncher)

        dockerTemplate.setMode(Node.Mode.NORMAL)
        dockerTemplate.setNumExecutors(20)
        dockerTemplate.setRemoveVolumes(false)
        dockerTemplate.setRetentionStrategy(new DockerOnceRetentionStrategy(15))
        dockerTemplate.setPullStrategy(DockerImagePullStrategy.PULL_LATEST)

        templates.add(dockerTemplate)
    }

    dockerClouds.add(
            new DockerCloud(cloud.name,
                    templates,
                    cloud.serverUrl,
                    cloud.containerCapStr,
                    cloud.connectTimeout ?: 15,
                    cloud.readTimeout ?: 15,
                    cloud.credentialsId,
                    cloud.version
            )
    )
}

Jenkins.instance.clouds.addAll(dockerClouds)
println '         --> Configured docker cloud.'
