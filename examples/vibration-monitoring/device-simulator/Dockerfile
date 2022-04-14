FROM openjdk:11-jdk-stretch

WORKDIR /app/

COPY ./build/distributions/* ./

# extract tar archive
RUN tar -xf ./device-simulator-*.tar
# move contents one level up to get rid of version number in directory name
RUN mv ./device-simulator-*/* ./ && rm -r device-simulator*

#ENTRYPOINT ./bin/device-simulator -h $TB_HOST -r $TB_REST_PORT -d $DEVICE_NAME -u $TB_USER -p $TB_PW -m $TB_MQTT_PORT --profile=$DEVICE_PROFILE -f $SAMPLE_FREQ
ENTRYPOINT ./bin/device-simulator
