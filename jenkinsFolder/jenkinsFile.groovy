pipeline {
    agent { docker { image 'maven:3.3.3'}}
        stages {
           stage('load properties file..') {
                  steps {
                       script {
                             gitProp = readProperties file:'propertiesFiles/git.properties'
			     commonProp = readProperties file:'propertiesFiles/common.properties'
			     artifactoryProp = readProperties file:'propertiesFiles/artifactory.properties'
			     deployProps = readProperties file:'propertiesFiles/deploy.properties'
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
			  	echo env.WORKSPACE
				}
			}
	   
		stage('DEPLOY') {
                  steps {
		  	script {
				  try   {
					 echo env.WORKSPACE
					sh deployProps.dockerContainerId
					output=readFile('result').trim()
					 echo env.WORKSPACE
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
              }
    }
