pipeline {
    agent any

    tools {
        jdk 'JAVA_HOME'
        maven 'M2_HOME'
    }

    environment {
        SONAR_HOST_URL = 'http://192.168.33.10:9000/'
        SONAR_AUTH_TOKEN = credentials('sonarqube')
        GITLEAKS_URL = 'https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz'
    }

    stages {

        /*************** DEVELOPMENT PHASE ***************/
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/nizar1920/devsecops.git'
            }
        }

        stage('Pre-commit Security Hooks') {
            steps {
                script {
                    sh '''
                    echo "Installation et exécution des hooks de sécurité (pre-commit)..."
                    if ! dpkg -s python3-venv >/dev/null 2>&1; then
                        sudo apt update && sudo apt install -y python3-venv python3-pip
                    fi
                    git config --unset-all core.hooksPath || true
                    if ! command -v pre-commit &> /dev/null; then
                        python3 -m venv venv
                        . venv/bin/activate
                        pip install pre-commit
                    fi
                    pre-commit install
                    pre-commit run --all-files || true
                    '''
                }
            }
        }

        stage('Build, Test & Package') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Verify JAR') {
            steps {
                sh 'ls -l target/gestion-station-ski-1.0.jar'
            }
        }

        stage('JaCoCo Report') {
            steps {
                sh 'mvn jacoco:report'
            }
        }

        stage('JaCoCo coverage report') {
            steps {
                step([$class: 'JacocoPublisher',
                      execPattern: '**/target/jacoco.exec',
                      classPattern: '**/classes',
                      sourcePattern: '**/src',
                      exclusionPattern: '*/target/**/,**/*Test*,**/*_javassist/**'
                ])
            }
        }

        /*************** ACCEPTANCE / QA PHASE ***************/
        stage('SAST - SonarQube Analysis') {
            steps {
                script {
                    sh '''
                    echo "Analyse SAST avec SonarQube..."
                    mvn sonar:sonar \
                        -Dsonar.projectKey=devsecops \
                        -Dsonar.host.url=$SONAR_HOST_URL \
                        -Dsonar.token=$SONAR_AUTH_TOKEN
                    '''
                }
            }
        }

        stage('SCA - Dependency Check') {
            steps {
                script {
                    echo "Analyse des dépendances avec OWASP Dependency-Check..."
                    sh 'mvn org.owasp:dependency-check-maven:check -Dformat=HTML || true'
                }
                publishHTML(target: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'target',
                    reportFiles: 'dependency-check-report.html',
                    reportName: 'OWASP Dependency-Check Report'
                ])
            }
        }

        stage('Secrets Scan - Gitleaks') {
            steps {
                script {
                    sh '''
                    echo "Téléchargement et exécution de Gitleaks..."
                    rm -f gitleaks gitleaks.tar.gz
                    curl -L -o gitleaks.tar.gz ${GITLEAKS_URL}
                    tar -xzf gitleaks.tar.gz
                    chmod +x gitleaks || mv gitleaks_*_linux_x64/gitleaks ./gitleaks
                    ./gitleaks detect --source . --no-git --report-format html --report-path gitleaks-report.html || true
                    echo "Rapport Gitleaks généré : gitleaks-report.html"
                    '''
                }
                // Publier le rapport HTML
                publishHTML(target: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: '.',                  // Le rapport est généré dans le workspace
                    reportFiles: 'gitleaks-report.html',
                    reportName: 'Gitleaks Report'
                ])
            }
        }

        stage('Docker Build & Scan') {
            steps {
                script {
                    sh '''
                    IMAGE_NAME="nizar101/gestion-station-ski:1.0.0"
                    echo "Construction de l'image Docker..."
                    docker build -t $IMAGE_NAME .
                    echo "Scan de l'image avec Trivy..."
                    if ! command -v trivy &> /dev/null; then
                        curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh
                        chmod +x ./bin/trivy
                    fi
                    ./bin/trivy image --timeout 20m \
                        --exit-code 0 \
                        --severity MEDIUM,HIGH,CRITICAL \
                        --format html \
                        --output trivy-report.html \
                        $IMAGE_NAME
                    echo "Rapport Trivy généré : trivy-report.html"
                    '''
                }
                // Publier le rapport HTML Trivy
                publishHTML(target: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: '.',                  
                    reportFiles: 'trivy-report.html',
                    reportName: 'Trivy Report'
                ])
            }
        }

        /*************** PRODUCTION PHASE ***************/
        stage('Deploy to Nexus') {
            steps {
                sh 'mvn deploy -DskipTests -DaltDeploymentRepository=deploymentRepo::default::http://192.168.33.10:8081/repository/maven-releases/'
            }
        }

        stage('Deploy Docker Image') {
            steps {
                withCredentials([string(credentialsId: 'dockerhub-jenkins-token', variable: 'dockerhub_token')]) {
                    sh "docker login -u nizar101 -p ${dockerhub_token}"
                    sh 'docker push nizar101/gestion-station-ski:1.0.0'
                }
            }
        }

        /*************** ACCEPTANCE / QA POST-DEPLOY ***************/
        stage('DAST - OWASP ZAP Scan') {
            steps {
                script {
                    sh '''
                    echo "Lancement du scan DAST OWASP ZAP..."
                    docker run --rm -v $(pwd):/zap/wrk/:rw \
                        owasp/zap2docker-stable zap-baseline.py \
                        -t http://localhost:8080 -r zap-report.html || true
                    '''
                }
            }
        }

        /*************** OPERATIONS PHASE ***************/
        stage('Start Monitoring Containers') {
            steps {
                sh 'docker start 489d14dd8ed7 || true'
                sh 'docker start a8bb77026230 || true'
            }
        }

        stage('Email Notification') {
            steps {
                mail bcc: '',
                     body: '''
Final Report: The pipeline has completed successfully. No action required.
''',
                     cc: '',
                     from: '',
                     replyTo: '',
                     subject: 'Succès de la pipeline DevOps Project',
                     to: 'nizartlili482@gmail.com'
            }
        }
    }

    /*************** POST-BUILD ACTIONS ***************/
    post {
        success {
            script {
                emailext(
                    subject: "Build Success: ${currentBuild.fullDisplayName}",
                    body: " Le build a réussi ! Consultez les détails à ${env.BUILD_URL}",
                    to: 'nizartlili482@gmail.com'
                )
            }
        }
        failure {
            script {
                emailext(
                    subject: " Build Failure: ${currentBuild.fullDisplayName}",
                    body: "Le build a échoué ! Vérifiez les détails à ${env.BUILD_URL}",
                    to: 'nizartlili482@gmail.com'
                )
            }
        }
        always {
            script {
                emailext(
                    subject: " Build Notification: ${currentBuild.fullDisplayName}",
                    body: "Consultez les détails du build à ${env.BUILD_URL}",
                    to: 'nizartlili482@gmail.com'
                )
            }
        }
    }
}

