# Uniandes Labs Orchestration System (ULOS)

## Author

- Esteban Gonzalez Ruales

# Project Description

The Uniandes Labs Orchestration System (ULOS) is a system that automates the execution of tasks in labs across the Uniandes campus.

You can download the project with the following command on whichever directory you want the repository to be placed at:

```zsh
git clone https://github.com/est-gonzalezr/uniandes-labs-orchestration-system
```

# Components

ULOS has many components that work together to make the whole system work.

![Proyecto de Grado - Components](https://github.com/est-gonzalezr/uniandes-labs-orchestration-system/assets/74991415/a270ffcb-8ab8-484f-a962-700b890fd5f0)

## Global Processing Engine

![Proyecto de Grado - GPE Messaging Layout](https://github.com/est-gonzalezr/uniandes-labs-orchestration-system/assets/74991415/1ed5277c-20f1-4ba9-aabd-06c72984cec3)

The Global Processing Engine (GPE) is the main distribution point of the orchestration system. The GPE receives messages sent from (possibly) an API that has received requests to process tasks from its users. The tasks are rerouted to processing clusters that take care of processing the tasks and sending back the result. Having the result the GPE can reroute the results back to the API for storage and notification to the users.

## Processing Cluster

A Processing Cluster (PC) is a unit of ULOS that takes care of processing the tasks that are sent to it. The PC takes care of doing everything necessary to process the file. Each PC is divided in multiple elements to ensure that this can be done.

![Proyecto de Grado - PC Messaging Layout](https://github.com/est-gonzalezr/uniandes-labs-orchestration-system/assets/74991415/a3000084-dd17-4120-9f66-92e7c284d9b6)

### Messaging

Each PC has a Messaging component that takes care of the internal distribution of the tasks to ensure that all steps to process a task are followed. The Messaging component consists on a RabbitMQ broker and a Scala companion to set it up. The component first sets up a consumer to consume tasks from the GPE. Afterwards it sets up all the message queues that it needs to store the messages it gets from the GPE and from internal components of the CP. The decision to make use of a message broker like RabbitMQ was meant to let the different internal components of the PC to execute at different speeds and avoid bottlenecks. This allows that at times of demand, the system can process many tasks without having to worry about its capacity because the messages are stored until they are processed. Also, since there can be many PCs the load will be balanced between the PCs that are instantiated.

### FTP Downloading

The FTP Downloading (FTPD) component is the first component to process the tasks that arrive at the PC. It downloads the necessary task files sent by the user from an FTP server so that the PC can process it. This was meant to avoid heavy files passed on as messages and instead make each message carry only essential information about the task. After downloading the necessary files the message is sent to the next step of processing via the Messaging component.

### Processing

The Processing component consumes messages left by the FTP Downloading component and processes them. It takes care of making sure that the files can be read and that the inner layout of the files (folder layout, necessary files) matches the layout needed for processing a task. Although the task is supposed to be executed and the result returned, the functionality is not yet implemented so the component messages if the files meet the criteria to be executed.

# Deployment

ULOS has many deployment combinations that can work but not all have been tested. Here we show the ones that have been tested and talk about some other combinations that can be theoretically be achieved.

## Production Deployment

The production deployment is meant for production use although it can also be used for testing. The deployment is done using Docker so that we can automatically deploy instances.

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

FTP_CONSUMER_QUANTITY = 2
PROCESSING_CONSUMER_QUANTITY = 5
```

It is advised to leave the `LOCAL_RABBITMQ_HOST` and `LOCAL_RABBITMQ_PORT` variables as they are unless you know how to modify these attributes from the Docker file and are able to use them effectively as these variables make the direct communication with the processing cluster's RabbitMQ service possible. It is also advised to not modify the `LOCAL_RABBITMQ_USERNAME` and `LOCAL_RABBITMQ_PASSWORD` unless you know how to change the RabbitMQ username and password on the Dockerfile.

##### GPE RabbitMQ Federation

On the other hand, you should change all of the `UPSTREAM_RABBITMQ` variables to connect to an upstream from which messages will be consumed. The upstream for the PC is the GPE from where the PC will consume messages from. The GPE has a RabbitMQ broker that publishes the tasks to a queue called `federated_global_processing_queue` to allow [federated queues](https://www.rabbitmq.com/docs/federated-queues) between RabbitMQ brokers. If this is done correctly on the upstream the only thing necessary to do in the PC env variables is to set the `UPSTREAM_RABBITMQ` parameters to the specifications of the machine where the upstream RabbitMQ server is located. The configuration on the PC host machine is taken care of automatically.

Doing this will permit the PC to consume the messages that the upstream service publishes to the queue.

### Further Configuration

At this point most of the configuration has already been done but some things are still missing. We still need to configure how the GPE will receive messages from all the processing clusters that are initialized. To do this you have to add the different PCs as upstreams. You can do this with the following commands on the docker console where the GPE's RabbitMQ service is running:

```zsh
rabbitmqctl set_parameter federation-upstream <cluster-name> "{\"uri\":\"amqp://<username>:<password>@<host>:<port>\"}"
rabbitmqctl set_parameter federation-upstream-set processing_clusters "[{\"upstream\": \"<cluster-name>\"}]"
```

These commands are responsible for defining a new upstream and adding it to the `processing_clusters`. The `processing_clusters` set is part of an automatically defined policy that federates queues from the PCs towards the GPE. Unfortunately, RabbitMQ doesn't yet have a functionality to clusters to a set without having to redefine the whole set so if you want to add a new upstream you also have to redefine the `processing_clusters` parameter. An example of this can be seen below:

```zsh
rabbitmqctl set_parameter federation-upstream processing-cluster-1 "{\"uri\":\"amqp://guest:guest@host.docker.internal:5673\"}"
rabbitmqctl set_parameter federation-upstream-set processing_clusters "[{\"upstream\": \"processing-cluster-1\"}]"

# If later I want to add more upstreams I would have to do the following:

rabbitmqctl set_parameter federation-upstream processing-cluster-2 "{\"uri\":\"amqp://guest:guest@192.168.2.10:5672\"}"
rabbitmqctl set_parameter federation-upstream-set processing_clusters "[{\"upstream\": \"processing-cluster-1\"}, {\"upstream\": \"processing-cluster-2\"}]"
```

If you know how many PCs you would have from the start (you can always add more later anyways) you could define them all at once.

```zsh
rabbitmqctl set_parameter federation-upstream processing-cluster-1 "{\"uri\":\"amqp://guest:guest@host.docker.internal:5673\"}"
rabbitmqctl set_parameter federation-upstream processing-cluster-2 "{\"uri\":\"amqp://guest:guest@192.168.2.10:5672\"}"
rabbitmqctl set_parameter federation-upstream-set processing_clusters '[{"upstream": "processing-cluster-1"}, {"upstream": "processing-cluster-2"}]'
```

With this the queues will federate automatically from the PCs defined to the GPE.

Lastly, on whatever service is implemented before the GPE, it should also set up federation policies with the GPE. By default, the GPE already has a queue meant to be federated called `federated_user_results_queue` that stores the results of the tasks that the users send. To federate the queues from the GPE to the implemented service it is necessary to run the following commands wherever the implemented service runs:

```zsh
rabbitmqctl set_parameter federation-upstream global_processing "{\"uri\":\"amqp://guest:guest@<gpe_host>:5672\"}"
rabbitmqctl set_policy --apply-to queues federated-user-results-queue "^federated_user_results_queue" "{\"federation-upstream\":\"global-processing\"}"
```

This will allow for the federation to take place and the service before the GPE to receive messages from the GPE.
