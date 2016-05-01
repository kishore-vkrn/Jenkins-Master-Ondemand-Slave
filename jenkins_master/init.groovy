import hudson.model.*;
import jenkins.model.*;
import hudson.security.*
import jenkins.model.Jenkins
import hudson.model.User

import com.cloudbees.plugins.credentials.Credentials
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.cloudbees.plugins.credentials.SystemCredentialsProvider

import com.nirima.jenkins.plugins.docker.*
import com.nirima.jenkins.plugins.docker.launcher.*
import com.nirima.jenkins.plugins.docker.strategy.*

println "--> disabling master executors"
Jenkins.instance.setNumExecutors(1)

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
                    'Test Jenkins.',
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
                        name: 'swarm',
                        serverUrl: 'http://127.0.0.1:4243',
                        containerCapStr: '100',
                        connectionTimeout: 5,
                        readTimeout: 15,
                        credentialsId: dockerCertificatesDirectoryCredentialsId,
                        version: '',
                        templates: [
                                [
                                        image: 'batmat/jenkins-ssh-slave',
                                        labelString: 'some label for demo',
                                        remoteFs: '',
                                        credentialsId: jenkinsSlaveCredentialsId,
                                        idleTerminationMinutes: '5',
                                        sshLaunchTimeoutMinutes: '1',
                                        jvmOptions: '',
                                        javaPath: '',
                                        memoryLimit: 2500,
                                        memorySwap: 0,
                                        cpuShares: 0,
                                        prefixStartSlaveCmd: '',
                                        suffixStartSlaveCmd: '',
                                        instanceCapStr: '100',
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
        dockerTemplate.setNumExecutors(1)
        dockerTemplate.setRemoveVolumes(true)
        dockerTemplate.setRetentionStrategy(new DockerOnceRetentionStrategy(10))
        dockerTemplate.setPullStrategy(DockerImagePullStrategy.PULL_LATEST)

        templates.add(dockerTemplate)
    }

    dockerClouds.add(
            new DockerCloud(cloud.name,
                    templates,
                    cloud.serverUrl,
                    cloud.containerCapStr,
                    cloud.connectTimeout ?: 15, // Well, it's one for the money...
                    cloud.readTimeout ?: 15,    // Two for the show
                    cloud.credentialsId,
                    cloud.version
            )
    )
}

Jenkins.instance.clouds.addAll(dockerClouds)
println 'Configured docker cloud.'

String[] arg = ['1236', 'admin', 'N'] as String[]
def instance = Jenkins.getInstance()
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
def allUsers = User.getAll()
for (u in allUsers) {
 if (u.getId()== arg[0] ) {
	println "${arg[0]} all ready exists in Jenkins"
	}else {
	 hudsonRealm.createAccount(arg[0], arg[1])
	 if (arg[2] =="Y"){
		 def strategy = new GlobalMatrixAuthorizationStrategy()
		 strategy.add(Jenkins.ADMINISTER, arg[0])
		 instance.setAuthorizationStrategy(strategy)
	 }
	 instance.setSecurityRealm(hudsonRealm)
	 instance.save()
 }
}
