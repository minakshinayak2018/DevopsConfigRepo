pipeline {
	agent any
        	stages {
		stage('LOAD PROPERTIES FILES') {
                  steps {
                       script {
		       	 gitProp = readProperties file:'./propertiesFiles/git.properties'
			       commonProp = readProperties file:'./propertiesFiles/common.properties'
			       artifactoryProp = readProperties file:'./propertiesFiles/artifactory.properties'
			       deployProps = readProperties file:'./propertiesFiles/deploy.properties'
			       configProp = load commonProp.configFile
			       echo 'LOAD SUCCESS'
				         }
			       }
			   }
			 stage('READ GIT') {
                  steps {
		  	   script {
                           git url: gitProp.gitUrl,
                           branch: gitProp.branchName
                           echo 'READ SUCCESS'
				                    }
					               }
				             }
			      stage('SONAR SCAN') {
                  steps {
		  	            script {
			                configProp.scan()
				             }
				         }
			        }
			      stage('BUILD') {
                  steps {
		  	               script {
			                    configProp.build()
			                      }
				               }
			            }
			      stage('UPLOAD ARTIFACT') {
                             steps {
		  	                         script {
			                       configProp.artifactory()
			                                }
				                           }
			              }
			     stage('DEPLOY') {
                         steps {
		  	                     script {
			                        configProp.deploy()
			                       deleteDir()
			                 }
                }         
            }    
     	}
}
