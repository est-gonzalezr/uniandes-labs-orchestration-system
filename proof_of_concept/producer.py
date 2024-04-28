import pika
import time
import json
import random

from pika.spec import Exchange

# RabbitMQ connection parameters
RABBITMQ_HOST = 'localhost'
RABBITMQ_PORT = 5672
RABBITMQ_USER = 'guest'
RABBITMQ_PASS = 'guest'
RABBITMQ_VHOST = '/'

credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
parameters = pika.ConnectionParameters(RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_VHOST, credentials)
connection = pika.BlockingConnection(parameters)
channel = connection.channel()

exchange = "user_tasks_exchange"
queue = "federated_user_tasks_queue"
routing_key = "user.task"

channel.exchange_declare(exchange=exchange, exchange_type="topic")
channel.queue_declare(queue=queue)
channel.queue_bind(exchange=exchange, queue=queue, routing_key=routing_key)

while True:
    # input()
    message = {
        "task_id": f"{random.randint(1000000, 10000000)}",
        "task_owner": f"{random.randint(1000000, 3000000)}",
        "task_type_id": f"{random.randint(1, 4)}",
        "ftp_file_path": f"/home/fedora/{random.choice(["email.zip", "facebook.zip", "email_uncomplete.zip", "facebook_uncomplete.zip"])}",
        # "random_string": "aksdjflaskdjflksadjflksadjflsdkjflsakdjflsakjdfosadjfoasdkjfopsadkfjopsdjfosadkfjoadkfj"
    }
    message = json.dumps(message)
    print(f" [x] Sending {message}")
    # channel.basic_publish(exchange="local_parsing_exchange", routing_key="processing.parsing.download", body=message)
    channel.basic_publish(exchange=exchange, routing_key=routing_key, body=message)
    time.sleep(5)
