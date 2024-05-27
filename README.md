# Uniandes Labs Orchestration System

Poner que todas las llaves tienen que ser strings

## Author

- Esteban Gonzalez Ruales

# Project Description

The Uniandes Labs Orchestration System (ULOS for short) is a system that automates the execution of tasks in labs across the Uniandes campus.

You can clone this repository to get the source code and run the project locally.

# Components

ULOS has many components that work together to make the whole system work. On the following diagram you can see the components that make up the whole system.

![Components Diagram](/diagrams/components.svg)

For the implementation you can see the used technologies in the following diagram.

![Technologies Diagram](/diagrams/components_technologies.svg)

THE ULOS consists of three main components: the Global Processing Component, the Processing Cluster(s) and the FTP Storage. The Global Processing Component is the main distribution point of the system and takes care of distributing the tasks to the Processing Clusters. The Processing Cluster is the unit that takes care of processing the tasks that are sent to it and sending back the results. The FTP storage is the storage component that takes care of storing the files that are necessary to process the tasks.

## Global Processing Component (GPC)

The Global Processing Component is the main entry point of the orchestration system. The GPC receives tasks in the form of messages and distribuites them to the Processing Clusters. The GPC is divided in two main components: the Global Processing Engine and the Global Prcessing Broker.

### Global Processing Engine (GPE)

The Global Processing Engine is the main component of the GPC. The GPE takes care of configuring up all the necesary messaging infrastructure on the Global Processing Broker to distribute the tasks to the Processing Clusters. It communicates directly with the message broker to set up the necessary queues and exchanges to allow the distribution of tasks. It also starts all the necessary consumers necessary to allow the federation of queues over the system as a whole to work.

### Global Processing Broker (GPB)

The Global Processing Broker is the messaging component of the GPC. The GPB is responsible for the deployment of all the messaging infrastructure necessary to allow the distribution of tasks to the Processing Clusters and back. The GPB receives commands from the GPE to set up the necessary queues and exchanges to allow the distribution of tasks. It also allows the federation of queues from and to the Processing Clusters to allow the distribution of tasks to be done.

For the specific messaging layout of the GPE you can see the following diagram.

![GPC Messaging Layout](/diagrams/gp_messaging.svg)


## Processing Cluster Component (PCC)

The Processing Cluster Component is the component that takes care of processing the tasks that are sent to the ULOS. The PCC is divided in two main components: the Processing Cluster Engine and the Processing Cluster Broker.

### Processing Cluster Engine (PCE)

The Processing Cluster Engine is the main component of the PCC. Generally, it takes care of processing the tasks that it receives from the GPC and sending back the results. The PCE is further divided into three main components: the Messaging Manager, the FTP Downloader and Uploader, and the Task Processor.

#### Messaging Manager

The Messaging Manager is the component that takes care of the internal distribution of the tasks to ensure that all steps to process a task are followed. It is analogous to the GPE on the GPC but for the processing cluster. It communicates directly to the Processing Cluster Broker to set up the necessary queues, exchanges and consumers to allow the consumption and correct processing steps for the tasks that get to the processing cluster. It also configures the federation policies to allow federation from the GPC.

#### FTP Downloader and Uploader

The FTP Downloader and Uploader is the component that takes care of downloading the necessary files to process a task from an FTP server and uploading the results back to the FTP server. The component is meant to avoid heavy files passed on as messages and instead make each message carry only essential information about the task. This is possible by having the message that arrives at the processing cluster have the name of the file to download from the FTP server. After downloading the necessary files the message is sent to the next step of processing via the messaging services. When there is a file to upload the component receives the name of the file to upload and uploads it to the FTP server fro the local file system. Subsequently, the message is sent to the GPC to notify that the task has been completed.

#### Task Processor

The Task Processor is the component that takes care of processing the tasks that arrive at the processing cluster. It takes care of making sure that the files can be read and that the inner layout of the files (folder layout, necessary files) matches the layout needed for processing a task. After the task is processed the component saves the results to the local file system and sends a message with the file name to the FTP Downloader and Uploader to upload the results to the FTP server. If the execution of the task fails the component sends a message to the GPC to notify that the task has failed with the error message.

### Processing Cluster Broker (PCB)

The Processing Cluster Broker is the messaging component of the PCC. The PCB is responsible for the deployment of all the messaging infrastructure necessary to allow the proper execution steps of tasks inside of the  Processing Clusters. The PCB receives commands from the Messaging Manager to set up the necessary queues and exchanges of the cluster. It also allows the federation of queues from and to the GPC to allow the consumption of tasks from the GCP and the sending of results back to the GPC.

For the specific messaging layout of the PCE you can see the following diagram.

![PCC Messaging Layout](/diagrams/pc_messaging.svg)

For an overall messaging layout of the system you can see the following diagram.

![Overall Messaging Layout](/diagrams/overall_messaging.svg)

### FTP Storage (FTPS)

The FTP Storage is the storage component of the ULOS. The FTPS is responsible for storing the files that are sent to the system and the results of the tasks that are processed.

## Deployment

The deployment of the ULOS is currently done manually but can be automated with the use of Docker in the future. The current deployment requires the manual setup of the components and the configuration of variables (in configuration file) to allow the components to communicate with each other and work properly. The necessary code to allow the deployment with docker and env variables is already in place but further configuration to the docker files is necessary to allow the containers to have the necesarry dependencies to run.


### Dependencies

You should have installed the following dependencies on the machine(s) you plan to run the ULOS GPC, PCC or both:

- Base Software
  - RabbitMQ (Message Broker)
  - Scala (Programming Language)
  - SBT (Scala Build Tool)
  - Temurin (JDK)
- Processing Clusters
  - NodeJS (JavaScript Runtime)
  - Cypress (End-to-End Testing Framework)

As more execution environments are added to the ULOS more dependencies for the processing clusters will be added.

### Startup Process

It is necessary to start both the GPC and the PCC to allow the ULOS to work. For both you will have to configure the necessary variables to allow for the proper startup of the components and for the communication between the components.

On both the GPC and the PCC navigate to the `src/main/resources/application.conf` file and configure the variables that are listed there. Most variables are self-explanatory, but the the `task_type` variable is the one that you would have to change to change the type of tasks that are being processed.

For now the available task types are:
- `Web`

The plan is to have all of the following task types available in the future:
- `Web`
- `Mobile`
- `RoboticArm`
- `Printer3D`
- `ElectricalGrid`

Having set up the variables for the GPC and the PCC you can start the components by running the following commandss:

```zsh
sbt
run --local
```

If you configured the variables correctly and both the RabbitMQ server and the FTP server are running you should be able to see the components starting up successfully.

If you are running the whole system on the same machine you have no need for federation between the GPC and the PCC so you can go ahead and open the python scripts that are in the `proof_of_concept` folder, change the variables inside each script to match the variables in the `application.conf` file and run the scripts to see the system in action. You should run both the `producer.py` and the `consumer.py` scripts to both be able to send and receive messages from the ULOS. The `producer.py` script will send a message to the GPC and upload a file to the FTP server and the `consumer.py` script will receive the message from the GPC and download the file from the FTP server if the execution of the task was successful. For running the scripts you will need to have Python installed on your machine and probably create a virtual environment to install the necessary dependencies. For the python dependencies you will only need `pika` to allow the scripts to communicate with the RabbitMQ service.

#### Startup for components in different machines

If you are deploying the components in different machines you will have to configure the federation between the GPC and the PCC. To do this you will have to follow the previous steps and some extra steps to allow the federation of messages between components to work.

##### Federation between GPC and PCC

To allow the federation between the GPC and the PCC you will have to run some commands on the terminal to allow the federation to work. On the machine where a processing cluster is running you will have to run the following commands:

```zsh
# Change the variables to match the variables on the GPC configuration file
upstream_rabbitmq_host="192.168.0.2"
upstream_rabbitmq_port="5672"
upstream_rabbitmq_username="guest"
upstream_rabbitmq_password="guest"

global_processing_upstream_name="global-processing"
global_processing_policy_name="federated-global-processing-queue"
global_processing_federated_queue="federated_global_processing_queue"

# Set federation for the messages that each processing cluster takes from the global processing component
rabbitmqctl set_parameter federation-upstream $global_processing_upstream_name "{\"uri\":\"amqp://$upstream_rabbitmq_username:$upstream_rabbitmq_password@$upstream_rabbitmq_host:$upstream_rabbitmq_port\"}"

rabbitmqctl set_policy --apply-to queues $global_processing_policy_name "^$global_processing_federated_queue" "{\"federation-upstream\":\"$global_processing_upstream_name\"}"
```

This will allow the processing clusters to configure an upstream to the global processing engine and to set a policy to allow the federation of messages from the global processing engine to the processing clusters.

On the machine where the GPC is running you will have to run the following commands:

```zsh
global_results_cluster_name="processing-clusters"
global_results_policy_name="federated-global-results-queue"
global_results_federated_queue="federated_global_results_queue"

# Set federation for the messages that the processing clusters sends to the global processing component
rabbitmqctl set_policy --apply-to queues $global_results_policy_name "^$global_results_federated_queue" "{\"federation-upstream-set\":\"$global_results_cluster_name\"}"

# Set a name for the processing cluster
# For each processing cluster you will have to run this command with a different name and the parameters for the processing cluster
rabbitmqctl set_parameter federation-upstream <cluster-name> "{\"uri\":\"amqp://<pc-username>:<pc-password>@<pc-host>:<pc-port>\"}"

# Add the processing cluster to the federation upstream set
rabbitmqctl set_parameter federation-upstream-set processing-clusters "[{\"upstream\": \"<cluster-name>\"}]"
```

This will allow the global processing engine to configure an upstream to the processing clusters and to add the processing clusters to the federation upstream set. If you have multiple processing clusters you will have to run the last commands multiple times to add all of the processing clusters to the federation upstream set. You can see an example of this in the following snippet.

```zsh
rabbitmqctl set_parameter federation-upstream processing-cluster-1 "{\"uri\":\"amqp://guest:guest@host.docker.internal:5674\"}"

rabbitmqctl set_parameter federation-upstream processing-cluster-2 "{\"uri\":\"amqp://guest:guest@192.168.0.10:5672\"}"

rabbitmqctl set_parameter federation-upstream-set processing-clusters '[{"upstream": "processing-cluster-1"}, {"upstream": "processing-cluster-2"}]'
```

In the example above we are configuring two processing clusters to be part of the federation upstream set of the global processing engine.

##### Federation between Scripts and GPC

If you are running the scripts in a different machine than the GPC you will have to configure the federation between the scripts and the GPC. To do this you will have to run the following commands on the machine where the GPC is running:

```zsh
# Change the variables to match the variables on the scripts' machine
upstream_rabbitmq_host="192.168.0.2"
upstream_rabbitmq_port="5672"
upstream_rabbitmq_username="guest"
upstream_rabbitmq_password="guest"

user_tasks_upstream_name="user-processing"
user_tasks_policy_name="federated-user-tasks-queue"
user_tasks_federated_queue="federated_user_tasks_queue"

# Set federation for the messages that the scripts send to the global processing component
rabbitmqctl set_parameter federation-upstream $user_tasks_upstream_name "{\"uri\":\"amqp://$upstream_rabbitmq_username:$upstream_rabbitmq_password@$upstream_rabbitmq_host:$upstream_rabbitmq_port\"}"

rabbitmqctl set_policy --apply-to queues $user_tasks_policy_name "^$user_tasks_federated_queue" "{\"federation-upstream\":\"$user_tasks_upstream_name\"}"
```

This will allow the GPC to configure an upstream to the machine where the scripts are running and to set a policy to allow the federation of messages from the scripts' machine to the GPC.

On the machine where the scripts are running you will have to run the following commands:

```zsh
# Change the variables to match the variables on the GPC configuration file
upstream_rabbitmq_host="192.168.0.2"
upstream_rabbitmq_port="5672"
upstream_rabbitmq_username="guest"
upstream_rabbitmq_password="guest"

rabbitmqctl set_parameter federation-upstream global-processing "{\"uri\":\"amqp://$upstream_rabbitmq_username:$upstream_rabbitmq_password@$upstream_rabbitmq_host:$upstream_rabbitmq_port\"}"

rabbitmqctl set_policy --apply-to queues federated-user-results-queue "^federated_user_results_queue" "{\"federation-upstream\":\"global-processing\"}"
```

This will allow the scripts' machine to configure an upstream to the GPC and to set a policy to allow the federation of messages from the GPC to the scripts' machine. This will allow the scripts' machine to receive the results from the GPC.


## ⚠️ Warnings and Consideratios

If you plan to make changes to the code of the project you should be aware of the following considerations:
- Limit the messages that are sent to the ULOS to json messages with String type keys and values. This is because the ULOS is not able to handle messages with other types of keys and values.

## Further Work

Further work can be done to improve the project. The recommended additions and improvements can be found in the [poster](/diagrams/poster.pdf) that was created for the project.

### Dependencies

The dependencies for the project are:

- Docker (Everything needed to run docker containers)

Make sure you have these dependencies installed before attempting to run the project. For macOS you can install these dependencies with Homebrew by running the following commands:

```zsh
brew update
brew install --cask docker
```

These commands will make sure that you can run the project locally without having to worry about the dependencies.

Having Docker (Docker Desktop) installed make sure to open it. On macOS, when you open Docker Desktop a Docker icon will appear on the Menu Bar that will show the status of Docker. If everything started successfully you should be able to continue with the rest of the instructions.

### Startup Process

Since the instances of ULOS are going to be containerized it is important to specify some attributes to let communication between containers be possible.

#### Startup for Global Processing Engine

Go to the folder named `global-processing`. From there you should be able to find everything necessary to start the GPE.

For the GPE these environment variables are necessary:

```
LOCAL_RABBITMQ_HOST = "rabbitmq-global"
LOCAL_RABBITMQ_PORT = 5672
LOCAL_RABBITMQ_USERNAME = "guest"
LOCAL_RABBITMQ_PASSWORD = "guest"

UPSTREAM_RABBITMQ_HOST = "host.docker.internal"
UPSTREAM_RABBITMQ_PORT = 5672
UPSTREAM_RABBITMQ_USERNAME = "guest"
UPSTREAM_RABBITMQ_PASSWORD = "guest"
```

It is advised to leave the `LOCAL_RABBITMQ_HOST` and `LOCAL_RABBITMQ_PORT` variables as they are unless you know how to modify these attributes from the Docker file and are able to use them effectively as these variables make the direct communication with the processing cluster's RabbitMQ service possible. It is also advised to not modify the `LOCAL_RABBITMQ_USERNAME` and `LOCAL_RABBITMQ_PASSWORD` unless you know how to change the RabbitMQ username and password on the Dockerfile.

##### GPE RabbitMQ Federation

On the other hand, you should change all of the `UPSTREAM_RABBITMQ` variables to connect to an upstream from which messages will be consumed. The upstream for the GPE is the API or whatever service is implemented to receive user requests and tasks. On whatever is implemented, it should have a RabbitMQ broker that publishes the tasks to a queue called `federated_user_tasks_queue` to allow [federated queues](https://www.rabbitmq.com/docs/federated-queues) between RabbitMQ brokers. If this is done correctly on the upstream the only thing necessary to do in the GPE env variables is to set the `UPSTREAM_RABBITMQ` parameters to the specifications of the machine where the upstream RabbitMQ server is located. The configuration on the GPE host machine is taken care of automatically.

Doing this will permit the GPE to consume the messages that the upstream service publishes to the queue.

##### Docker YAML Considerations

Since the deployment is automated you shouldn't have to worry about much when running the `docker compose` commands. Still, you should check the `compose.yaml` file to see the ports that the RabbitMQ service is exposing.

```yaml
ports:
  - 15673:15672
  - 5673:5672
```

For example, in the above example the docker container port `15672` is being mapped to the host machine port `15673`. As you could notice, the correspondence of ports is not the same and this is not required. This can be used as an advantage if you want to deploy more than one component to the same machine. For example, you could deploy the GPE's RabbitMQ service to port `5672` of a machine and the PC's RabbitMQ service to port `5673` of the same machine. You can leverage this if you don't have many machines to deploy the service on or if you don't need to deploy many processing clusters.

After you have looked at the `compose.yaml` and started the docker service head to the project folder (`global-processing`) and run the following commands:

```zsh
docker compose up -d
```

This command will automatically signal docker to start the RabbitMQ server and the GPE to set up the project. In Docker Desktop you can see the compose.

The `rabbitmq-global` container exposes 2 ports, commonly `5672` and `15672` if you didn't configure other ones. On whichever machine you deploy the container you should be able to access the messaging service on port `5672` and access its management interface on port `15672`. This can be done to check on operations, testing, or debugging. Having this set you should be able to connect from any machine to the machine hosting the docker container if you know the IP address of the machine and the container port. Direct messaging between brokers is not advised and has not been tested since RabbitMQ recommends the [Federation plugin](https://www.rabbitmq.com/docs/federation) to transmit messages between brokers without requiring clustering.

#### Startup for Processing Cluster

Go to the folder named `processing-cluster`. From there you should be able to find everything necessary to start the PC.

For the PC these environment variables are necessary:

```
LOCAL_RABBITMQ_HOST = "rabbitmq-local"
LOCAL_RABBITMQ_PORT = 5672
LOCAL_RABBITMQ_USERNAME = "guest"
LOCAL_RABBITMQ_PASSWORD = "guest"

UPSTREAM_RABBITMQ_HOST = "host.docker.internal"
UPSTREAM_RABBITMQ_PORT = 5673
UPSTREAM_RABBITMQ_USERNAME = "guest"
UPSTREAM_RABBITMQ_PASSWORD = "guest"

FTP_HOST = "192.168.2.13"
FTP_PORT = 21
FTP_USERNAME = "fedora"
FTP_PASSWORD = "fedora"

FTP_DOWNLOADING_CONSUMER_QUANTITY = 2
FTP_UPLOADING_CONSUMER_QUANTITY = 2
PROCESSING_CONSUMER_QUANTITY = 5

TASK_TYPE = "Web"
```

You can change the `FTP_DOWNLOADING_CONSUMER_QUANTITY`, `FTP_UPLOADING_CONSUMER_QUANTITY` and `PROCESSING_CONSUMER_QUANTITY` env variables to change how many consumers will be started on each queue. If you define a number less than 1 the system will default to 1. The `TASK_TYPE` variable is used to define the type of task that the PC will process. This is used set at runtime what types of tasks are going to be processed.

It is advised to leave the `LOCAL_RABBITMQ_HOST` and `LOCAL_RABBITMQ_PORT` variables as they are unless you know how to modify these attributes from the Docker file and are able to use them effectively as these variables make the direct communication with the processing cluster's RabbitMQ service possible. It is also advised to not modify the `LOCAL_RABBITMQ_USERNAME` and `LOCAL_RABBITMQ_PASSWORD` unless you know how to change the RabbitMQ username and password on the Dockerfile.

##### GPE RabbitMQ Federation

On the other hand, you should change all of the `UPSTREAM_RABBITMQ` variables to connect to an upstream from which messages will be consumed. The upstream for the PC is the GPE from where the PC will consume messages from. The GPE has a RabbitMQ broker that publishes the tasks to a queue called `federated_global_processing_queue` to allow [federated queues](https://www.rabbitmq.com/docs/federated-queues) between RabbitMQ brokers. If this is done correctly on the upstream the only thing necessary to do in the PC env variables is to set the `UPSTREAM_RABBITMQ` parameters to the specifications of the machine where the upstream RabbitMQ server is located. The configuration on the PC host machine is taken care of automatically.

Doing this will permit the PC to consume the messages that the upstream service publishes to the queue.

### Further Configuration

At this point most of the configuration has already been done but some things are still missing. We still need to configure how the GPE will receive messages from all the processing clusters that are initialized. To do this you have to add the different PCs as upstreams. You can do this with the following commands on the docker console where the GPE's RabbitMQ service is running:

```zsh
rabbitmqctl set_parameter federation-upstream <cluster-name> "{\"uri\":\"amqp://<username>:<password>@<host>:<port>\"}"
rabbitmqctl set_parameter federation-upstream-set processing-clusters "[{\"upstream\": \"<cluster-name>\"}]"
```

These commands are responsible for defining a new upstream and adding it to the `processing-clusters`. The `processing-clusters` set is part of an automatically defined policy that federates queues from the PCs towards the GPE. Unfortunately, RabbitMQ doesn't yet have a functionality to clusters to a set without having to redefine the whole set so if you want to add a new upstream you also have to redefine the `processing-clusters` parameter. An example of this can be seen below:

```zsh
rabbitmqctl set_parameter federation-upstream processing-cluster-1 "{\"uri\":\"amqp://guest:guest@host.docker.internal:5674\"}"
rabbitmqctl set_parameter federation-upstream-set processing-clusters "[{\"upstream\": \"processing-cluster-1\"}]"

# If later I want to add more upstreams I would have to do the following:

rabbitmqctl set_parameter federation-upstream processing-cluster-2 "{\"uri\":\"amqp://guest:guest@192.168.2.10:5674\"}"
rabbitmqctl set_parameter federation-upstream-set processing-clusters "[{\"upstream\": \"processing-cluster-1\"}, {\"upstream\": \"processing-cluster-2\"}]"
```

If you know how many PCs you would have from the start (you can always add more later anyways) you could define them all at once.

```zsh
rabbitmqctl set_parameter federation-upstream processing-cluster-1 "{\"uri\":\"amqp://guest:guest@host.docker.internal:5674\"}"
rabbitmqctl set_parameter federation-upstream processing-cluster-2 "{\"uri\":\"amqp://guest:guest@192.168.2.10:5674\"}"
rabbitmqctl set_parameter federation-upstream-set processing-clusters '[{"upstream": "processing-cluster-1"}, {"upstream": "processing-cluster-2"}]'
```

With this the queues will federate automatically from the PCs defined to the GPE.

Lastly, on whatever service is implemented before the GPE, it should also set up federation policies with the GPE. By default, the GPE already has a queue meant to be federated called `federated_user_results_queue` that stores the results of the tasks that the users send. To federate the queues from the GPE to the implemented service it is necessary to run the following commands wherever the implemented service runs:

```zsh
rabbitmqctl set_parameter federation-upstream global-processing "{\"uri\":\"amqp://guest:guest@<gpe_host>:5672\"}"
rabbitmqctl set_policy --apply-to queues federated-user-results-queue "^federated_user_results_queue" "{\"federation-upstream\":\"global-processing\"}"
```

This will allow for the federation to take place and the service before the GPE to receive messages from the GPE.
