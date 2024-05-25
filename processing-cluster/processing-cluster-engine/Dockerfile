FROM sbtscala/scala-sbt:eclipse-temurin-alpine-22_36_1.10.0_3.4.2

# Create dir
RUN mkdir -p /app/src

# Set the working directory
WORKDIR /app

COPY ./src /app/src
COPY ./build.sbt /app

# Update dependencies and compile the project
RUN sbt update &&\
    sbt compile

# Run the project
CMD ["sbt", "run"]
