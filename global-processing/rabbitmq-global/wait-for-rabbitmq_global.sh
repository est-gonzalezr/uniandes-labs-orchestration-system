#!/bin/bash

# Function to check RabbitMQ status
check_rabbitmq_status() {
    rabbitmqctl status
    return $?
}

# Set the maximum number of attempts
max_attempts=5

# Initialize attempt counter
attempt_num=1

# Loop until the command is successful or the maximum number of attempts is reached
while ! check_rabbitmq_status && [ $attempt_num -le $max_attempts ]; do
    echo "Attempt $attempt_num to check RabbitMQ status failed. Trying again..."
    sleep 5
    attempt_num=$(( attempt_num + 1 ))
done

# Check if RabbitMQ is running
if check_rabbitmq_status; then
    user_tasks_upstream_name="user-processing"
    user_tasks_policy_name="federated-user-tasks-queue"
    user_tasks_federated_queue="federated_user_tasks_queue"

    # Set federation for the messages that the users send
    rabbitmqctl set_parameter federation-upstream $user_tasks_upstream_name "{\"uri\":\"amqp://$UPSTREAM_RABBITMQ_USERNAME:$UPSTREAM_RABBITMQ_PASSWORD@$UPSTREAM_RABBITMQ_HOST:$UPSTREAM_RABBITMQ_PORT\"}"
    rabbitmqctl set_policy --apply-to queues $user_tasks_policy_name "^$user_tasks_federated_queue" "{\"federation-upstream\":\"$user_tasks_upstream_name\"}"

    global_results_cluster_name="processing-clusters"
    global_results_policy_name="federated-global-results-queue"
    global_results_federated_queue="federated_global_results_queue"

    # Set federation for the messages that the processing clusters sends to the global processing engine
    rabbitmqctl set_policy --apply-to queues $global_results_policy_name "^$global_results_federated_queue" "{\"federation-upstream-set\":\"$global_results_cluster_name\"}"
else
    echo "RabbitMQ did not start after $max_attempts attempts."
fi
