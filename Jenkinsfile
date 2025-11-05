pipeline {
    agent any

    tools {
        jdk 'JAVA_HOME'
        maven 'M2_HOME'
    }

    stages {
        stage('GIT') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/nizar1920/devsecops.git'
            }
        }

        stage('Compile Stage') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('SONARQUBE') {
    environment {
        SONAR_HOST_URL = 'http://192.168.33.10:9000/'
        SONAR_AUTH_TOKEN = credentials('sonarqube')
    }

    steps {
        sh 'mvn sonar:sonar -Dsonar.projectKey=devops-test -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.token=$SONAR_AUTH_TOKEN'
    }
}

    }
}
