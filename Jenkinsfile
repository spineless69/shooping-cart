pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        stage('Docker Build') {
            steps {
                sh 'docker build -t shopping-cart-java .'
            }
        }
        stage('Run Container') {
            steps {
                sh 'docker run -d -p 8070:8070 shopping-cart-java'
            }
        }
    }
}
