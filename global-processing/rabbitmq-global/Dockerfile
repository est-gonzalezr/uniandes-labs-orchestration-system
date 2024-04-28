# Get lastest rabbitmq image
FROM rabbitmq:3-management

# Install required plugins in offline mode
RUN rabbitmq-plugins enable --offline rabbitmq_management rabbitmq_federation rabbitmq_federation_management rabbitmq_shovel rabbitmq_shovel_management

# Copy configuration files to start rabbitmq federation policy automatically
COPY wait-for-rabbitmq_global.sh /usr/local/bin/

# Set permissions to execute the script
RUN chmod +x /usr/local/bin/wait-for-rabbitmq_global.sh

# Set the entrypoint to execute the script
COPY entrypoint_global.sh /usr/local/bin/

# Set permissions to execute the script
RUN chmod +x /usr/local/bin/entrypoint_global.sh

# Set the entrypoint to execute the script
CMD ["entrypoint_global.sh"]
