import jenkins.model.Jenkins
import jenkins.model.*
import hudson.model.User
import hudson.security.*

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
   else {
     println "1236/admin created"
   }
	 instance.setSecurityRealm(hudsonRealm)
	 instance.save()
 }
}
