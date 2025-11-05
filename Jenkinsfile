pipeline {
    agent any

    tools {
        jdk 'JAVA_HOME'
        maven 'M2_HOME'
    }

    environment {
        SONAR_HOST_URL = 'http://192.168.33.10:9000/'
        SONAR_AUTH_TOKEN = credentials('sonarqube')
        GITLEAKS_URL = 'https://github.com/zricethezav/gitleaks/releases/latest/download/gitleaks-linux-amd64'
    }

    stages {

        /*************** 1. CLONAGE DU REPO ***************/
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/nizar1920/devsecops.git'
            }
        }

        /*************** 2. SECURITE DEVELOPPEUR (SHIFT-LEFT) ***************/
        stage('Pre-commit Security Hooks') {
            steps {
                script {
                    sh '''
                    echo "Installation et exécution des hooks de sécurité (pre-commit)..."
                    if ! command -v pre-commit &> /dev/null; then
                        python3 -m venv venv
                        . venv/bin/activate
                        pip install pre-commit
                    fi
                    pre-commit install
                    pre-commit run --all-files
                    '''
                }
            }
        }

        /*************** 3. COMPILATION & TESTS ***************/
        stage('Build & Test') {
            steps {
                sh 'mvn clean compile test'
            }
        }

        /*************** 4. SAST - SCAN CODE SOURCE ***************/
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

        /*************** 5. SCA - DEPENDENCY SCAN ***************/
        stage('SCA - Dependency Check') {
            steps {
                script {
                    echo "Analyse des dépendances avec OWASP Dependency-Check..."
                    sh 'mvn org.owasp:dependency-check-maven:check -Dformat=HTML'
                }
            }
        }

        /*************** 6. SECRETS SCAN ***************/
        stage('Secrets Scan - Gitleaks') {
            steps {
                script {
                    sh '''
                    echo "Téléchargement et exécution de Gitleaks..."
                    if [ ! -f ./gitleaks ]; then
                        curl -sSL $GITLEAKS_URL -o gitleaks
                        chmod +x gitleaks
                    fi
                    ./gitleaks detect --source . --report-format json --report-path gitleaks-report.json || true
                    '''
                }
            }
        }

        /*************** 7. DOCKER IMAGE BUILD & SCAN ***************/
        stage('Docker Build & Scan') {
            steps {
                script {
                    sh '''
                    echo "Construction de l'image Docker..."
                    docker build -t devsecops-app .

                    echo "Scan de l'image avec Trivy..."
                    if ! command -v trivy &> /dev/null; then
                        echo "Installation de Trivy..."
                        curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh
                    fi

                    ./trivy image --exit-code 0 --severity MEDIUM,HIGH,CRITICAL --format json --output trivy-report.json devsecops-app
                    '''
                }
            }
        }

        /*************** 8. DAST - SCAN EN STAGING ***************/
        stage('DAST - OWASP ZAP Scan') {
            steps {
                script {
                    sh '''
                    echo "Lancement d'un scan DAST avec OWASP ZAP (en mode baseline)..."
                    docker run --rm -v $(pwd):/zap/wrk/:rw owasp/zap2docker-stable zap-baseline.py \
                        -t http://localhost:8080 -r zap-report.html || true
                    '''
                }
            }
        }
}
