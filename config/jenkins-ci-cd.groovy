def utilRepo, commonUtility,commonShellCommands,jenkinsGroovy, gitProp,artifactoryProps,deployProps
def gitAccessKey = 'e67dc0aa-09f5-481f-8980-24c6d8ab3234'
def branchName = 'master'
def jenkinsRepo = 'https://github.com/minakshinayak2018/config-repo.git'

node {
	def gitCheckOutCalls 
		stage('LOAD PROPERTIES FILES') {
			git branch: branchName,
                        credentialsId: gitAccessKey, 
                        url: jenkinsRepo
			
		       	       gitProp = readProperties file:'./propertiesFiles/git.properties'
			       commonProps = readProperties file:'./propertiesFiles/common.properties'
			       artifactoryProps = readProperties file:'./propertiesFiles/artifactory.properties'
			       deployProps = readProperties file:'./propertiesFiles/deploy.properties'
			       jenkinsGroovy = load './propertiesFiles/gitCheckout.groovy'
			
			//Util repo
			       utilRepo = jenkinsGroovy.checkOutRepo(gitProp['gitUtilRepo'], branchName,gitAccessKey);
			       commonUtility = load "./util/commonUtility.groovy"
			       commonShellCommands = readProperties file:'./propertiesFiles/commonShellCommands.properties'
			       echo 'LOAD SUCCESS'		      
		}
			   
stage('READ GIT') {
	      jenkinsGroovy.checkOutRepo(gitProperties['gitAppRepo'], branchName,gitAccessKey);
              echo 'READ SUCCESS'
	    }
stage('SONAR SCAN') {
                sh commonShellCommands.buildSonarScan
                echo 'SONAR SCAN SUCCESS'
	   }
stage('BUILD') {
                    sh commonShellCommands.mavenClean
		    echo 'BUILD SUCCESS'
           }
 stage('UPLOAD ARTIFACT') {
		      server = Artifactory.server artifactoryProps.ARTIFACTORY_ID
		      uploadSpec = """{
                	"files": [
                    	        {	
 				"build": "${env.BUILD_NUMBER}",
                       		"pattern": "target/*.war",
                        	"target": "app-repo/target/${env.BUILD_NUMBER}/"
                    		}
                    	      ]
                	}"""
                     server.upload(uploadSpec)
		     echo ' UPLOAD ARTIFACT SUCCESS'
	       }
stage('DEPLOY') {
                 try   {
			sh deployProps.dockerContainerId
			output=readFile('result').trim()
			if(output!=null)
			{
			sh deployProps.dockerContainerRm
			}
			}catch (err)
			{
			 echo 'DELETE FAILED'
			}
			sh deployProps.dockerImageDeploy
			sh deployProps.dockerRestart
			echo 'DEPLOY SUCCESS'     
           } 
stage('EMAIL NOTIFICATION') {
			commonUtility.notifyBuild(env.BUILD_STATUS)
			}
           }
