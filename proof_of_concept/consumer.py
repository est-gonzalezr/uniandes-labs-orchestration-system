
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
queue = "federated_user_results_queue"

channel.exchange_declare(exchange=exchange, exchange_type="topic")
channel.queue_declare(queue=queue)


# consume
def callback(ch, method, properties, body):
    print(f" [x] Received {body}")
    time.sleep(0.5)
    ch.basic_ack(delivery_tag=method.delivery_tag)


channel.basic_consume(queue=queue, on_message_callback=callback, auto_ack=False)

print(' [*] Waiting for messages. To exit press CTRL+C')
channel.start_consuming()
