def call() {
    node('ci-server') {

        stage('CodeCheckout') {

            sh "find ."
            sh "find . | sed -e '1d' |xargs rm -rf"
            if(env.TAG_NAME ==~ ".*") {
                env.branch_name = "refs/tags/${env.TAG_NAME}"
            } else {
                env.branch_name = "${env.BRANCH_NAME}"
            }
            checkout scmGit(
                    branches: [[name: "${branch_name}"]],
                    userRemoteConfigs: [[url: "https://github.com/raghudevopsb78/roboshop-${component}"]]
            )
        }

        if (env.TAG_NAME ==~ '.*') {
            stage('Build Code') {
                sh 'docker build -t 633788536644.dkr.ecr.us-east-1.amazonaws.com/roboshop-${component}:${TAG_NAME} .'
            }
            stage('Release Software') {
                sh 'aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 633788536644.dkr.ecr.us-east-1.amazonaws.com'
                sh 'docker push 633788536644.dkr.ecr.us-east-1.amazonaws.com/roboshop-${component}:${TAG_NAME}'
            }
            stage('Deploy to Dev') {
                sh 'aws eks update-kubeconfig --name dev-eks'
                sh 'argocd login argocd-dev.rdevops6a.online --username admin --password $(argocd admin initial-password -n argocd | head -1) --insecure --grpc-web'
                sh 'argocd app set ${component} --parameter appVersion=${TAG_NAME}'
                sh 'argocd app sync ${component}'
            }
        } else {
            stage('Lint Code') {
                print 'OK'
            }
            if(env.BRANCH_NAME != 'main') {
                stage('Run Unit tests') {
                    print 'OK'
                }
                stage('Run Integration tests') {
                    print 'OK'
                }
            }
            stage('Sonar Scan Code Review') {
                print 'OK'
            }

        }

    }
}