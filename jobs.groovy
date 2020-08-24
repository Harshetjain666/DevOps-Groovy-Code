job('job1') {
    description('Copy the code from github and also create and push an image in dockerHub')
    label('rhel')
    scm {
        git {                      
            remote {
                url('https://github.com/Harshetjain666/DevopsTask-6.git')
            }
            branch('*/' + 'master')
        }
    }
    steps{
        shell('mkdir /root/workspace ; cp -rf * /root/workspace/')
        dockerBuilderPublisher {
            dockerFileDirectory('/root/workspace/')
            cloud('')
            fromRegistry {
                credentialsId('')
                url('')
            }
            pushOnSuccess(true)
            cleanImages(false)
            cleanupWithJenkinsJobDelete(false)
            pushCredentialsId('424dca0c-5cf9-4bee-ae01-0d373301bf14')
            tagsString('harshetjain/html-environment')
        }    
    }
}

job('job2') {
    description('Make Environment Ready And Updated')
    label('rhel')
    scm {
        git {
            remote {
                url('https://github.com/Harshetjain666/DevopsTask-6.git')
            }
            branch('*/' + 'master')
        }
    }
    triggers {
        upstream('job1', 'SUCCESS')
    }
    steps {
        shell('''mkdir /root/workspace ; cp -rf * /root/workspace/ 
            deployname=$(kubectl get deployment --selector=type=html --output=jsonpath={.items..metadata.name})
            if [ $deployname =="" ]
            then
            kubectl create -f /root/workspace/html.yml
            else
            echo "Environment updating ..."
            fi
            kubectl set image deployment  *=harshetjain/html-environment --selector=type=html --record''')
    }
}

job('job3') {
    description('Test the webpage and send mail if job fails')
    label('rhel')
    triggers {
        upstream('job2', 'SUCCESS')
        pollSCM {
            scmpoll_spec(* * * * *)
        }
    }
    steps {
        shell('''status=$(curl -o /dev/null -s -w "%{http_code}" http://192.168.99.100:31000)
            if [ $status == 200 ]
            then 
            exit 0
            else 
            exit 1
            fi''')
    }
    publishers {
        mailer('hjain8620.hj@gmail.com', false, false)
        downstreamParameterized {
            trigger('job4') {
                condition('UNSTABLE')
                
            }
        }

    }
}

job('job4') {
    description('Run Over Old Setup')
    label('rhel')
    steps {
        shell('''kubectl set image deployment  *=harshetjain/html-environment:v1 --selector=type=html --record''')
    }
}

buildPipelineView('Pipeline') {
    filterBuildQueue(true)
    filterExecutors(false)
    displayedBuilds(1)
    selectedJob('job1')
    alwaysAllowManualTrigger(true)
    showPipelineParameters()
    refreshFrequency(3)
}

    queue('job1')
