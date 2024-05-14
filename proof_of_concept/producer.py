import pika
import time
import json
import random
from ftplib import FTP
from typing import BinaryIO, Optional
import uuid

from pika.spec import Exchange
from pika.adapters.blocking_connection import BlockingChannel

# RabbitMQ connection parameters
RABBITMQ_HOST = 'localhost'
RABBITMQ_PORT = 5672
RABBITMQ_USER = 'guest'
RABBITMQ_PASS = 'guest'
RABBITMQ_VHOST = '/'

exchange = "user_tasks_exchange"
queue = "federated_user_tasks_queue"
routing_key = "user.task"

ftp_host = "192.168.0.5"
ftp_user = "fedora"
ftp_pass = "fedora"

def file_from_system(ftp_path: str) -> Optional[BinaryIO]:
    try:
        return open(ftp_path, "rb")
    except Exception as e:
        print(f"Error: {e}")
        return None

def ftp_connection(host: str, user: str, passwd: str) -> Optional[FTP]:
    try:
        ftp = FTP(host)
        ftp.set_pasv(True)
        ftp.login(user, passwd)
        return ftp
    except Exception as e:
        print(f"Error: {e}")
        return None

def upload_file_to_ftp(ftp: FTP, file_bytes: BinaryIO) -> Optional[str]:
    try:
        # file_name = f"for_execution/{uuid.uuid4()}.zip"
        file_name = f"{uuid.uuid4()}.zip"
        ftp.storbinary(f"STOR {file_name}", file_bytes)
        return file_name
    except Exception as e:
        print(f"Error: {e}")
        return None

def configure_rabbitmq(host: str, port: int, user: str, passwd: str) -> Optional[BlockingChannel]:
    try:
        credentials = pika.PlainCredentials(user, passwd)
        parameters = pika.ConnectionParameters(host, port, "/", credentials)
        connection = pika.BlockingConnection(parameters)

        channel = connection.channel()

        channel.exchange_declare(exchange=exchange, exchange_type="topic")
        channel.queue_declare(queue=queue)
        channel.queue_bind(exchange=exchange, queue=queue, routing_key=routing_key)

        return channel

    except Exception as e:
        print(f"Error: {e}")
        return None

def message_loop(ftp: FTP, channel: BlockingChannel) -> None:
    while True:
        file_path = input("Enter the file path: ")
        file = file_from_system(file_path)
        if file:
            file_name = upload_file_to_ftp(ftp, file)
            file.close()
            if file_name:
                message = {
                    "task_id": f"{file_name}",
                    "task_owner": f"Esteban",
                    # mandar las cosas sin '/' al principio
                    "ftp_downloading_path": f"{file_name}",
                    # "random_string": "aksdjflaskdjflksadjflksadjflsdkjflsakdjflsakjdfosadjfoasdkjfopsadkfjopsdjfosadkfjoadkfj"
                }
                message = json.dumps(message)
                print(f" [x] Sending {message}")
                channel.basic_publish(exchange=exchange, routing_key=routing_key, body=message)

def main() -> None:
    ftp = ftp_connection(ftp_host, ftp_user, ftp_pass)
    if ftp:
        print("FTP connection established")
        channel = configure_rabbitmq(RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASS)
        if channel:
            print("RabbitMQ connection established")
            message_loop(ftp, channel)
        else:
            print("Error connecting to RabbitMQ")
    else:
        print("Error connecting to FTP")


if __name__ == "__main__":
    main()
