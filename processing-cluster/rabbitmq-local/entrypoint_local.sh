#!/bin/bash

# Path to the existing script
wait_for_rabbitmq="/usr/local/bin/wait-for-rabbitmq_local.sh"

# Run the existing script in detached mode
nohup bash "$wait_for_rabbitmq" -detached &

# Start RabbitMQ server
rabbitmq-server
