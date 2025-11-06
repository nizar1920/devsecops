pipeline {
    agent any

    tools {
        jdk 'JAVA_HOME'
        maven 'M2_HOME'
    }

    environment {
        SONAR_HOST_URL = 'http://192.168.33.10:9000/'
        SONAR_AUTH_TOKEN = credentials('sonarqube')
        GITLEAKS_URL = 'https://github.com/gitleaks/gitleaks/releases/latest/download/gitleaks_8.29.0_linux_x64.tar.gz'
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

                    if ! dpkg -s python3-venv >/dev/null 2>&1; then
                        echo "Installation du module python3-venv..."
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

        /*************** 6. SECRETS SCAN - GITLEAKS ***************/
        stage('Secrets Scan - Gitleaks') {
            steps {
                script {
                    sh '''
                    echo "Téléchargement et exécution de Gitleaks..."

                    if [ ! -f ./gitleaks ]; then
                        echo "Téléchargement depuis $GITLEAKS_URL ..."
                        wget -q "$GITLEAKS_URL" -O gitleaks.tar.gz
                        tar -xzf gitleaks.tar.gz
                        chmod +x gitleaks || true
                    fi

                    ./gitleaks detect --source . \
                        --report-format json \
                        --report-path gitleaks-report.json || true

                    echo "Rapport Gitleaks généré : gitleaks-report.json"
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

                    ./trivy image --exit-code 0 \
                        --severity MEDIUM,HIGH,CRITICAL \
                        --format json \
                        --output trivy-report.json \
                        devsecops-app

                    echo "Rapport Trivy généré : trivy-report.json"
                    '''
                }
            }
        }

        /*************** 8. DAST - SCAN AVEC OWASP ZAP ***************/
        stage('DAST - OWASP ZAP Scan') {
            steps {
                script {
                    sh '''
                    echo "Lancement d'un scan DAST avec OWASP ZAP (mode baseline)..."
                    docker run --rm -v $(pwd):/zap/wrk/:rw owasp/zap2docker-stable zap-baseline.py \
                        -t http://localhost:8080 -r zap-report.html || true

                    echo "Rapport ZAP généré : zap-report.html"
                    '''
                }
            }
        }
    }

    /*************** 9. PUBLICATION DES RAPPORTS ***************/
    post {
        always {
            echo "Publication des rapports de sécurité..."

            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target',
                reportFiles: 'dependency-check-report.html',
                reportName: 'OWASP Dependency-Check Report'
            ])

            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: '.',
                reportFiles: 'zap-report.html',
                reportName: 'OWASP ZAP DAST Report'
            ])

            // Si tu veux convertir Gitleaks JSON en HTML, tu peux ajouter un script ici
            sh '''
            if [ -f gitleaks-report.json ]; then
                echo "<html><body><pre>" > gitleaks-report.html
                cat gitleaks-report.json >> gitleaks-report.html
                echo "</pre></body></html>" >> gitleaks-report.html
            fi
            '''
            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: '.',
                reportFiles: 'gitleaks-report.html',
                reportName: 'Gitleaks Secrets Scan Report'
            ])

            echo "Tous les rapports ont été publiés avec succès."
        }
    }
}

