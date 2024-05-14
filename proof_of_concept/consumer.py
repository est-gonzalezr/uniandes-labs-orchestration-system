
import pika
import time
import json
import random
from ftplib import FTP
from typing import BinaryIO, Optional

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

ftp_host = "192.168.0.5"
ftp_user = "fedora"
ftp_pass = "fedora"

channel.exchange_declare(exchange=exchange, exchange_type="topic")
channel.queue_declare(queue=queue)

def ftp_connection(host: str, user: str, passwd: str) -> Optional[FTP]:
    try:
        ftp = FTP(host)
        ftp.set_pasv(True)
        ftp.login(user, passwd)
        return ftp
    except Exception as e:
        print(f"Error: {e}")
        return None

def download_file_from_ftp(ftp: FTP, file_name: str) -> None:
    try:
        with open(f"incoming_files/{file_name}", "wb") as f:
            ftp.retrbinary(f"RETR {file_name}", f.write)
    except Exception as e:
        print(f"Error: {e}")

# consume
def callback(ch, method, properties, body):
    # parse body into json
    body = json.loads(body)
    print(f" [x] Received {body}")
    file_name = body["ftp_uploading_path"]
    ftp = ftp_connection(ftp_host, ftp_user, ftp_pass)
    if ftp:
        download_file_from_ftp(ftp, file_name)

    # ch.basic_ack(delivery_tag=method.delivery_tag)


channel.basic_consume(queue=queue, on_message_callback=callback, auto_ack=True)

print(' [*] Waiting for messages. To exit press CTRL+C')
channel.start_consuming()
