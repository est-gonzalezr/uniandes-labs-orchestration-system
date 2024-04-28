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

ULOS has many componentes that work together to make the whole system work.

Diagram here maybe???

## Global Processing Engine

The Global Processing Engine (GPE) is the main distribution point of the orchestration system. The GPE receives messages sent from (possibly) an API that has received requests to process tasks from its users. The tasks are rerouted to processing clusters that take care of processing the tasks and sending back the result. Having the result the GPE can reroute the results back to the General Manager for storage and notification of the users.

## Processing Cluster

A Processing Cluster (PC) is a unit of ULOS that takes care of processing the tasks that are sent to it. The PC takes care of doing everything necessary to process the file. Each PC is divided in multiple elements to ensure that this can be done.

Diagram here maybe???
### Messaging

Each PC has a Messaging component that takes care of the internal distribution of the tasks to ensure that all steps to process a task are followed. The Messaging component consists on a RabbitMQ broker and a Scala companion to set it up. The component first sets up a consumer to consume tasks from the GPE. Afterwards it sets up all the message queues that it needs to store the messages it gets from the GPE and from internal components of the CP. The decision to make use of a message broker like RabbitMQ was meant to let the different internal components of the PC to execute at different speeds and avoid bottlenecks. This allows that at times of demand, the system can process many tasks without having to worry about its capacity because the messages are stored until they are processed. Also, since there can be many PCs the load will be balanced between the PCs that are instantiated.

### FTP Downloading

The FTP Downloading (FTPD) component is the first component to process the tasks that arrive at the PC. It downloads the necessary task files sent by the user from an FTP server so that the PC can process it. This was meant to avoid heavy files passed on as messages and instead make each message carry only essential information about the task. After downloading the necessary files the message is sent to the next step of processing via the Messaging component.

### Processing

The Processing component consumes messages left by the FTP Downloading component and processes them. It takes care of making sure that the files can be read and that the inner layout of the files (folder layout, necessary files) matches the layout needed for processing a task. Although the task is supposed to be executed and the result returned, the functionality is not yet implemented so the component messages if the files meet the criteria to be executed.

## General Manager

Diagram here maybe???

### Database

### API
# Deployment

ULOS has many deployment combinations that can work but not all have been testes.
