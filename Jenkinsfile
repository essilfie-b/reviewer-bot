def appname = 'amali-ai'
def deploy_group_dev = 'mcp-dev'
def deploy_group_prod = 'mcp-prod'
def s3_bucket = 'amali-ai-src-bucket'
def devS3Bucket = 'amali-ai-src-bucket-dev'
def s3_filename = 'amaliai-codedeploy-src-mcp'
def imageRegistry = '867344436491.dkr.ecr.eu-west-1.amazonaws.com'
def devImageRegistry = '160450754406.dkr.ecr.eu-west-1.amazonaws.com'
def awsRegion = 'eu-west-1'
def awsCreds = 'ai-aws-cred'
def devAwsCreds = 'ai-aws-cred-dev'

pipeline {
  agent any

  tools { 
    jdk 'jdk21'
    maven 'Maven 3.9.9'
  }

  stages {

    stage('Maven Build & Test') {
      steps {
        sh 'mvn clean verify -Dskip.sonar=true'
      }
    }

    stage('SonarQube Analysis') {
      steps {
        script {
          withSonarQubeEnv('SonarQube') {
            def mvnHome = tool 'Maven 3.9.9'
            sh """
              ${mvnHome}/bin/mvn sonar:sonar \
              -Dsonar.projectKey=Amali-Tech_amaliai-mcp_5b2eef4c-f034-4331-ba8f-603743761df0 \
              -Dsonar.projectName=amaliai-mcp
            """
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
        script {
          def isProd = env.BRANCH_NAME == 'main'
          def activeCreds = isProd ? awsCreds : devAwsCreds
          withAWS(region: awsRegion, credentials: activeCreds) {
            def gitSha = sh(
              script: 'git log -n1 --format=format:"%H"',
              returnStdout: true
            ).trim()

            def envName = isProd ? 'prod' : 'dev'
            def imageName = "ai-mcp-${envName}"
            def activeRegistry = isProd ? imageRegistry : devImageRegistry

            env.imageTag = "${activeRegistry}/${imageName}:${gitSha}"

            sh """
              docker build -t ${env.imageTag} .
              aws ecr get-login-password --region ${awsRegion} | docker login --username AWS --password-stdin ${activeRegistry}
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
          def isProd = env.BRANCH_NAME == 'main'
          def activeRegistry = isProd ? imageRegistry : devImageRegistry

          sh """
            chmod +x scripts/update-deployment-artifact.sh
            ./scripts/update-deployment-artifact.sh ${env.imageTag} ${activeRegistry}
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
        script {
          def isProd = env.BRANCH_NAME == 'main'
          def activeCreds = isProd ? awsCreds : devAwsCreds
          withAWS(region: awsRegion, credentials: activeCreds) {
            def gitSha = sh(
              script: 'git log -n1 --format=format:"%H"',
              returnStdout: true
            ).trim()

            def versionedFile = "${s3_filename}-${gitSha}"
            env.DEPLOY_FILE = versionedFile

            def activeS3Bucket = isProd ? s3_bucket : devS3Bucket

            sh """
              aws deploy push \
                --application-name ${appname} \
                --description "Revision ${appname}-${gitSha}" \
                --ignore-hidden-files \
                --s3-location s3://${activeS3Bucket}/${versionedFile}.zip \
                --source artifact/
            """
          }
        }
      }
    }

    stage('Deploy to Development') {
      when { branch 'develop' }
      steps {
        withAWS(region: awsRegion, credentials: devAwsCreds) {
          sh """
            aws deploy create-deployment \
              --application-name ${appname} \
              --deployment-config-name CodeDeployDefault.OneAtATime \
              --deployment-group-name ${deploy_group_dev} \
              --file-exists-behavior OVERWRITE \
              --s3-location bucket=${devS3Bucket},key=${env.DEPLOY_FILE}.zip,bundleType=zip
          """
        }
      }
    }

    stage('Deploy to Production') {
      when { branch 'main' }
      steps {
        withAWS(region: awsRegion, credentials: awsCreds) {
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
