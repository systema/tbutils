FROM openjdk:11-jdk-stretch

WORKDIR /app/

COPY ./build/distributions/* ./

# extract tar archive
RUN tar -xf ./iot-app-*.tar
# move contents one level up to get rid of version number in directory name
RUN mv ./iot-app-*/* ./ && rm -r iot-app*

HEALTHCHECK --interval=5s --retries=40 \
    CMD ["test", "-f", "UP"]

ENTRYPOINT ./bin/iot-app
# --thingsboard-url "${TB_URL}" --user ${TB_USER} --password ${TB_PW} --profile "${DEVICE_PROFILE}"
#--device "${DEVICE_NAME}"
