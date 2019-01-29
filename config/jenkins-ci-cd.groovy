def utilRepo, commonUtility,commonShellCommands,jenkinsGroovy, gitProp,artifactoryProps,deployProps
pipeline {
	agent any
        	stages {
		stage('LOAD PROPERTIES FILES') {
                  steps {
                       script {
		       	       gitProp = readProperties file:'./propertiesFiles/git.properties'
			       commonProps = readProperties file:'./propertiesFiles/common.properties'
			       artifactoryProps = readProperties file:'./propertiesFiles/artifactory.properties'
			       deployProps = readProperties file:'./propertiesFiles/deploy.properties'
			       jenkinsGroovy = load './propertiesFiles/gitCheckout.groovy'
			       utilRepo = jenkinsGroovy(gitProp.gitUtilRepo, gitProp.branchName,gitProp.utilRepoCredentialsId);
			       commonUtility = load "./util/commonUtility.groovy"
			       commonShellCommands = readProperties file:'./util/propertiesFiles/commonShellCommands.properties
			       echo utilRepo
			       echo 'LOAD SUCCESS'
				         }
			            }
			   }
stage('READ GIT') {
        steps {
 	   script {
                    git url: gitProp.gitAppRepo,
                    branch: gitProp.branchName
                    echo 'READ SUCCESS'
                   }
	        }
	    }
stage('SONAR SCAN') {
        steps {
            script {
                sh commonShellCommands.buildSonarScan
                echo 'SONAR SCAN SUCCESS'
	             }
                }
	   }
stage('BUILD') {
          steps {
              script {
                    sh commonShellCommands.mavenClean
		    echo 'BUILD SUCCESS'
                      }
               }
           }
 stage('UPLOAD ARTIFACT') {
            steps {
		 script {
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
	             }
	       }
stage('DEPLOY') {
         steps {
             script {
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
                  }         
           } 
stage('EMAIL NOTIFICATION') {
		steps{
		   script {
			commonUtility.notifyBuild(env.BUILD_STATUS)
				    }
			     }
			}
     	         }
    }
