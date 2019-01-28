def scan(){                  
		dir(gitProps.path){
			sh commonProps.buildSonarScan
			echo 'SONAR SCAN SUCCESS'
			}
          }    
            
def build(){
			try{
				dir(gitProps.path){
					sh commonProps.mavenClean
					status = "SUCCESSFUL"
					echo 'BUILD SUCCESS'
						}
					}
			catch (e) {
					status = "FAILED"
						} 
			finally {
					notifyBuild(status)
					} 
			}
					

def notifyBuild(String buildStatus)
{
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>
    <p>Please give input to deploy"<a href="${env.JENKINS_URL}/job/${env.JOB_NAME}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>"""
	
 emailext (
      subject: subject,
      body: details,
      to: commonProps.recipients
    )
 
}

def artifactory(){
				server = Artifactory.server artifactoryProps.artifactServer
				uploadSpec = """{
                		"files": [
                    		{	
 					"build": "${env.BUILD_NUMBER}",
                       			"pattern": "target/*.war",
                        		"target": "demo-java/target/${env.BUILD_NUMBER}/"
                    		}
                    		]
                		}"""
            			server.upload(uploadSpec)
				echo 'ARTIFACT SUCCESS'
				}
				
def deploy(){
			try	{
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
return this
