podTemplate(label: '$name;format="lower,hyphen"$', containers: [
        containerTemplate(
                name: 'jnlp', image: '<put proper agent image name here>/jnlp-slave',
                resourceRequestMemory: '4Gi', resourceLimitMemory: '4Gi'
        )
]) {
    properties([
            parameters([
                    choice(choices: 'No\nYes',
                            description: 'Build feature-branch Docker images. Note: Master branch will ALWAYS build Docker images',
                            name: 'dockerImg')
            ])
    ])

    node('$name;format="lower,hyphen"$') {
        shouldBuild = true

        try {
            timestamps {
                ansiColor('gnome-terminal') {

                    stage('Checkout') {
                        checkout scm

                        result = sh(script: "git log -1 | grep 'Setting version to'", returnStatus: true)
                        if (result == 0) {
                            println('Detected VersionBump commit message - skipping build.')
                            shouldBuild = false
                        }
                    }

                    shouldBuild = shouldBuild || (params.dockerImg == 'Yes')

                    if (shouldBuild) {
                        isMasterBranch = ('master' == env.BRANCH_NAME)
                        stage('Versionbump') {
                            if (isMasterBranch) {
                                sh 'amm jenkins/bump_version.sc'
                            } else {
                                println("On branch \${env.BRANCH_NAME} - skipping Versionbump")
                            }
                        }

                        stage('Compile') {
                            sh 'sbt clean compile'
                        }

                        stage("Sbt Test") {
                            sh "LANG='en_US.UTF-8' sbt test"
                        }

                        stage('Docker') {
                            if (isMasterBranch || params.dockerImg == 'Yes') {
                                sh 'sbt docker:publish'
                            } else {
                                println("On branch \${env.BRANCH_NAME} - skipping Docker images build")
                            }
                        }

                        stage('Publish') {
                            if (isMasterBranch) {
                                sh 'amm jenkins/publish_version.sc'
                            } else {
                                println("On branch \${env.BRANCH_NAME} - skipping Publish")
                            }
                        }
                    }
                }
            }
        } catch (e) {
            println("Error happened: " + e.getMessage())
            sendErrorMail()
            sh "amm jenkins/cleanup.sc"
            throw e
        } finally {
            junit keepLongStdio: true, testResults: '**/target/test-reports/*.xml', allowEmptyResults: true
            deleteDir()
        }
    }

}

def sendErrorMail() {
    wrap([\$class: 'BuildUser']) {
        subject = "Build failed: Job '\${env.JOB_NAME} [\${env.BUILD_NUMBER}]'"
        body = """<p>Check console output at <a href='\${env.RUN_DISPLAY_URL}'>\${env.JOB_NAME} [\${env
                .BUILD_NUMBER}]</a></p>"""
        recipients = [culprits(), developers(), requestor(), brokenTestsSuspects(), brokenBuildSuspects(), upstreamDevelopers()]

        emailext(subject: subject, body: body, to: env.BUILD_USER_EMAIL, recipientProviders: recipients, mimeType: 'text/html')
    }
}
