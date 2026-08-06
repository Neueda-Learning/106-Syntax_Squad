pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        COMPOSE_PROJECT_NAME = 'tms'
        COMPOSE_CMD_FILE = '.compose_cmd'
        WORK_DIR_FILE = '.workdir'
        ENV_FILE = '.env'
        REPO_URL = 'https://github.com/Neueda-Learning/106-Syntax_Squad.git'
        REPO_BRANCH = 'main'
        REPO_CREDENTIALS_ID = ''
    }

    stages {
        stage('Checkout Source') {
            steps {
                script {
                    def workDir = "repo-${env.BUILD_NUMBER}-${UUID.randomUUID().toString().substring(0, 8)}"

                    def repoUrl = env.REPO_URL?.trim()
                    def branch = env.REPO_BRANCH?.trim()
                    def credentialsId = env.REPO_CREDENTIALS_ID?.trim()

                    if (!repoUrl) {
                        error('REPO_URL is required.')
                    }

                    if (!branch) {
                        error('REPO_BRANCH is required.')
                    }

                    // Safety net: remove the target folder only if it happens to already exist.
                    sh "rm -rf '${workDir}' 2>/dev/null || true"

                    if (credentialsId) {
                        // For private repos, keep Jenkins-managed credentials support.
                        dir(workDir) {
                            git branch: branch, credentialsId: credentialsId, url: repoUrl
                        }
                    } else {
                        // For public repos, avoid Git plugin pre-clean behavior on stale folders.
                        sh "git clone --branch '${branch}' --single-branch '${repoUrl}' '${workDir}'"
                    }

                    // Persist the computed folder name to a file: env.WORK_DIR mutations here
                    // do NOT reliably survive into later stages, so every downstream stage
                    // reads this file instead of relying on the env var.
                    writeFile file: env.WORK_DIR_FILE, text: workDir

                    echo "Checked out into workspace subfolder: ${workDir}"
                }
            }
        }

        stage('Validate Agent Tooling') {
            steps {
                script {
                    if (!isUnix()) {
                        error('This pipeline requires a Linux Jenkins agent with git, curl, docker, and either docker compose or docker-compose installed.')
                    }

                    sh 'git --version'
                    sh 'docker --version'
                    sh 'curl --version'

                    def composeCmd = sh(
                        script: '''
if docker compose version >/dev/null 2>&1; then
    echo "docker compose"
elif docker-compose version >/dev/null 2>&1; then
    echo "docker-compose"
fi
''',
                        returnStdout: true
                    ).trim()

                    if (!composeCmd) {
                        error('Neither docker compose nor docker-compose is available on this Jenkins agent.')
                    }

                    writeFile file: env.COMPOSE_CMD_FILE, text: composeCmd + "\n"
                    sh "${composeCmd} version"
                }
            }
        }

        stage('Test Backend') {
            steps {
                script {
                    def workDir = readFile(env.WORK_DIR_FILE).trim()
                    dir("${workDir}/backend") {
                        sh '''
if [ -f mvnw ]; then
    chmod +x mvnw
    ./mvnw -B clean test
elif command -v mvn >/dev/null 2>&1; then
    mvn -B clean test
else
    echo "No Maven executable found. Expected ./mvnw in repo or mvn on Jenkins agent."
    exit 1
fi
'''
                    }
                }
            }
            post {
                always {
                    script {
                        def workDir = readFile(env.WORK_DIR_FILE).trim()
                        junit testResults: "${workDir}/backend/target/surefire-reports/*.xml", allowEmptyResults: true
                    }
                }
            }
        }

        stage('Build Backend') {
            steps {
                script {
                    def workDir = readFile(env.WORK_DIR_FILE).trim()
                    dir("${workDir}/backend") {
                        // Tests already ran and passed in the "Test Backend" stage above,
                        // so skip re-running them here to avoid doing the work twice.
                        sh '''
if [ -f mvnw ]; then
    chmod +x mvnw
    ./mvnw -B package -DskipTests
elif command -v mvn >/dev/null 2>&1; then
    mvn -B package -DskipTests
else
    echo "No Maven executable found. Expected ./mvnw in repo or mvn on Jenkins agent."
    exit 1
fi
'''
                    }
                }
            }
        }

        stage('Validate Frontend') {
            steps {
                script {
                    def workDir = readFile(env.WORK_DIR_FILE).trim()
                    dir("${workDir}/frontend") {
                        sh '''
if command -v npm >/dev/null 2>&1; then
    npm ci
    npm run build
else
    echo "npm is not installed on this Jenkins agent. Skipping local frontend validation; frontend will be built by Docker during deployment."
fi
'''
                    }
                }
            }
        }

        stage('Prepare Deployment Env') {
            steps {
                script {
                    def workDir = readFile(env.WORK_DIR_FILE).trim()
                    def envContent = """
MYSQL_ROOT_PASSWORD=${env.MYSQL_ROOT_PASSWORD ?: 'n3u3da!'}
MYSQL_DATABASE=${env.MYSQL_DATABASE ?: 'tms_db'}
MYSQL_USER=${env.MYSQL_USER ?: 'tms_user'}
MYSQL_PASSWORD=${env.MYSQL_PASSWORD ?: 'tms_password'}
JWT_SECRET=${env.JWT_SECRET ?: 'd83f5e2a7c1b94d6e8f0a2b4c6d8e0f2a4b6c8d0e2f4a6b8c0d2e4f6a8b0c2d4'}
""".trim() + "\n"

                    writeFile file: "${workDir}/${env.ENV_FILE}", text: envContent
                }
            }
        }

        stage('Deploy MySQL') {
            steps {
                script {
                    def workDir = readFile(env.WORK_DIR_FILE).trim()
                    def composeCmd = readFile(env.COMPOSE_CMD_FILE).trim()
                    dir(workDir) {
                        // Ensure no previous deployment stack still holds host ports
                        // (for example backend on 8082) before we bring services up.
                        sh "${composeCmd} --env-file .env down --remove-orphans || true"

                        // Start MySQL ONLY first and wait for it to be healthy. The backend's
                        // DataInitializer runs schema-dependent queries the moment it boots, so
                        // tables must exist BEFORE the backend container is started — starting
                        // the whole stack at once caused the backend to crash-loop and fail its
                        // own healthcheck before schema.sql could be applied.
                        sh "${composeCmd} --env-file .env pull mysql || true"
                        sh "${composeCmd} --env-file .env up -d mysql"
                        sh '''
                            for i in $(seq 1 30); do
                                status=$(docker inspect -f "{{.State.Health.Status}}" tms-mysql 2>/dev/null || echo "starting")
                                if [ "$status" = "healthy" ]; then
                                    echo "MySQL is healthy."
                                    break
                                fi
                                echo "Waiting for MySQL to become healthy... ($i/30)"
                                sleep 5
                            done
                        '''
                    }
                }
            }
        }

        stage('Deploy Application') {
            steps {
                script {
                    def workDir = readFile(env.WORK_DIR_FILE).trim()
                    def composeCmd = readFile(env.COMPOSE_CMD_FILE).trim()
                    dir(workDir) {
                        // MySQL is already up/healthy and schema is applied; now bring up
                        // (and build) backend + frontend on top of it.
                        sh "${composeCmd} --env-file .env pull || true"
                        sh "${composeCmd} --env-file .env up -d --build --remove-orphans"
                        sh "${composeCmd} --env-file .env ps"
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def workDir = readFile(env.WORK_DIR_FILE).trim()
                    def composeCmd = readFile(env.COMPOSE_CMD_FILE).trim()
                    dir(workDir) {
                        sh "${composeCmd} --env-file .env ps"
                    }
                    sh 'curl -fsS http://localhost:8082/api/actuator/health'
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment pipeline completed successfully.'
        }
        failure {
            echo 'Deployment pipeline failed. Check stage logs above.'
        }
        cleanup {
            script {
                if (fileExists(env.WORK_DIR_FILE)) {
                    def workDir = readFile(env.WORK_DIR_FILE).trim()
                    sh "rm -f '${workDir}/${env.ENV_FILE}'"
                }
                sh 'rm -f .compose_cmd .workdir'
            }
        }
    }
}