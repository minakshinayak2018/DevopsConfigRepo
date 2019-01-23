pipeline {
    agent { docker { image 'maven:3.3.3'}}
        stages {
           stage('load properties file..') {
                  steps {
                       script {
                             gitProp = readProperties file:'commonUtility/git.properties'
			     commonProp = readProperties file:'commonUtility/common.properties'
			     artifactoryProp = readProperties file:'commonUtility/artifactory.properties'
			     deployProps = readProperties file:'commonUtility/dockerDeployment.properties'
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
					sh deployProps.dockerDeploy
					sh deployProps.dockerRestart
					echo 'DEPLOY SUCCESS'	
				     	}	
                               }
                       }
                }
         }
