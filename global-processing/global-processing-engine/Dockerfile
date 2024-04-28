FROM sbtscala/scala-sbt:eclipse-temurin-alpine-17.0.10_7_1.9.9_3.4.0

# Create dir
RUN mkdir -p /app/src

# Set the working directory
WORKDIR /app

COPY ./src /app/src
COPY build.sbt /app

RUN sbt update &&\
    sbt compile

# Run the project
CMD ["sbt", "run"]
