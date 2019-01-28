def utilRepo,commonUtility,
def credentialsId= 'e67dc0aa-09f5-481f-8980-24c6d8ab3234'
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
			       utilRepo =jenkinsGroovy.gitCheckout(gitProp['gitUtilRepo'], gitProp['branchName'],credentialsId);
			       commonUtility = load "util/commonUtility.groovy"
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
			                commonUtility.scan(commonProps)
				             }
				         }
			        }
			      stage('BUILD') {
                  steps {
		  	               script {
			                    commonUtility.build(commonProps)
			                      }
				               }
			            }
			      stage('UPLOAD ARTIFACT') {
                             steps {
		  	                         script {
			                       commonUtility.artifactory(artifactoryProps)
			                                }
				                           }
			              }
			     stage('DEPLOY') {
                         steps {
		  	                     script {
			                        commonUtility.deploy(deployProps)
			                       deleteDir()
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
