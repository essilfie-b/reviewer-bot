def appname = 'amali-ai'
def deploy_group_dev = 'mcp-dev'
def deploy_group_prod = 'mcp-prod'
def s3_bucket = 'amali-ai-src-bucket'
def s3_filename = 'amaliai-codedeploy-src-mcp'
def imageRegistry = '867344436491.dkr.ecr.eu-west-1.amazonaws.com'
def awsRegion = 'eu-west-1'

pipeline {
  agent any

  tools { 
    jdk 'jdk21'
    maven 'Maven 3.9.9'
  }

  stages {

    stage('Build (Maven)') {
      steps {
        sh 'chmod +x mvnw'
        sh './mvnw clean package -Dmaven.test.skip'
      }
    }

    stage('SonarQube Analysis') {
      steps {
        script {
          withSonarQubeEnv('SonarQube') {
            sh 'mvn sonar:sonar'
          }
        }
      }
    }

    stage('Build and Push Docker Image') {
      when {
        anyOf {
          branch 'develop'
          branch 'main'
        }
      }
      steps {
        withAWS(region: awsRegion, credentials: 'ai-aws-cred') {
          script {
            def gitSha = sh(
              script: 'git log -n1 --format=format:"%H"',
              returnStdout: true
            ).trim()

            def isProd = env.BRANCH_NAME == 'main'
            def envName = isProd ? 'prod' : 'dev'
            def imageName = "ai-mcp-${envName}"

            env.imageTag = "${imageRegistry}/${imageName}:${gitSha}"

            sh """
              docker build -t ${env.imageTag} .
              aws ecr get-login-password --region ${awsRegion} | docker login --username AWS --password-stdin ${imageRegistry}
              docker push ${env.imageTag}
              docker logout
              docker rmi ${env.imageTag}
            """
          }
        }
      }
    }

    stage('Create Deployment Artifact') {
      when {
        anyOf {
          branch 'develop'
          branch 'main'
        }
      }
      steps {
        script {
          sh """
            chmod +x scripts/update-deployment-artifact.sh
            ./scripts/update-deployment-artifact.sh ${env.imageTag}
          """

          sh '''
            rm -rf artifact
            mkdir artifact
            cp -r scripts/ artifact/
            cp docker-compose.yml appspec.yml artifact/
          '''
        }
      }
    }

    stage('Prepare to Deploy') {
      when {
        anyOf {
          branch 'develop'
          branch 'main'
        }
      }
      steps {
        withAWS(region: awsRegion, credentials: 'ai-aws-cred') {
          script {
            def gitSha = sh(
              script: 'git log -n1 --format=format:"%H"',
              returnStdout: true
            ).trim()

            def versionedFile = "${s3_filename}-${gitSha}"
            env.DEPLOY_FILE = versionedFile

            sh """
              aws deploy push \
                --application-name ${appname} \
                --description "Revision ${appname}-${gitSha}" \
                --ignore-hidden-files \
                --s3-location s3://${s3_bucket}/${versionedFile}.zip \
                --source artifact/
            """
          }
        }
      }
    }

    stage('Deploy to Development') {
      when { branch 'develop' }
      steps {
        withAWS(region: awsRegion, credentials: 'ai-aws-cred') {
          sh """
            aws deploy create-deployment \
              --application-name ${appname} \
              --deployment-config-name CodeDeployDefault.OneAtATime \
              --deployment-group-name ${deploy_group_dev} \
              --file-exists-behavior OVERWRITE \
              --s3-location bucket=${s3_bucket},key=${env.DEPLOY_FILE}.zip,bundleType=zip
          """
        }
      }
    }

    stage('Deploy to Production') {
      when { branch 'main' }
      steps {
        withAWS(region: awsRegion, credentials: 'ai-aws-cred') {
          sh """
            aws deploy create-deployment \
              --application-name ${appname} \
              --deployment-config-name CodeDeployDefault.OneAtATime \
              --deployment-group-name ${deploy_group_prod} \
              --file-exists-behavior OVERWRITE \
              --s3-location bucket=${s3_bucket},key=${env.DEPLOY_FILE}.zip,bundleType=zip
          """
        }
      }
    }
  }

  post {
    always {
      cleanWs()
    }
    success {
      echo 'Pipeline executed successfully'
    }
    failure {
      echo 'Pipeline execution failed'
    }
  }
}
