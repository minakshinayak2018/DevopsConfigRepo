pipeline {
    agent any
        stages {
           stage('load properties file..') {
                  steps {
                       script {
                             gitProp = readProperties file:'propertiesFiles/git.properties'
			     commonProp = readProperties file:'propertiesFiles/common.properties'
			     artifactoryProp = readProperties file:'propertiesFiles/artifactory.properties'
			     deployProps = readProperties file:'propertiesFiles/deploy.properties'
			     emailNotification = load file:'config/reUsable.groovy'
                             echo 'LOAD SUCCESS'
                             }
                      }    
               }
               stage('read git url file..') {
                  steps {
                         git url: gitProp.gitUrl,
                         branch: gitProp.branchName
                         echo 'READ SUCCESS'
                        }    
                   }
              stage('sonar scan..') {
                  steps {
                         sh commonProp.buildSonarScan
                         echo 'SONAR SCAN SUCCESS'
                        }    
                   }
              stage('maven build..') {
                  steps {
                         sh commonProp.mavenClean
                         echo 'BUILD SUCCESS'
                        }    
                   } 
	      stage('UPLOAD ARTIFACT') {
                  steps {
			script {
				server = Artifactory.server artifactoryProp.ARTIFACTORY_ID
				uploadSpec = """{
                		"files": [
                    		{
                       			"pattern": "target/*.war",
                        		"target": "app-repo/target/"
                    		}
                    		]
                		}"""
            			server.upload(uploadSpec)
				}
				echo 'UPLOAD ARTIFACT SUCCESS'
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
					echo deployProps.dockerImageDeploy
					sh deployProps.dockerImageDeploy
					echo deployProps.dockerRestart
					sh deployProps.dockerRestart
					echo 'DEPLOY SUCCESS'
				     	}	
                               }
                       }
	     stage('Sending email'){
		     steps {
			     script {
                              try{
                                sh emailNotification.triggerEmail();
                               } catch (e) {
                             currentBuild.result = "FAILED"
                            sh emailNotification.triggerEmail();
                         throw e
                         }
	                 }
		     }
                }
	                        		
            }
    }
