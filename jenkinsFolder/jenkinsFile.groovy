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
			     emailNotification = readProperties file:'config/reUsable.groovy'
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
	stage("Notify Build") {
            	steps {
                	script {
                    	subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
                        summary = "${subject} (${env.BUILD_URL})"
                        details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                      <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>
                      <p>Please give input to deploy"<a href="${env.JENKINS_URL}/job/${env.JOB_NAME}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>"""
             emailext (
                subject: subject,
                body: details,
                to: commonProp.recipients
               )
	       echo "EMAIL NOTIFICATION SUCCESS"
                             }
		         }
	           }
	                        		
            }
    }
