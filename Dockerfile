#
# build in a temporary container
#

FROM adoptopenjdk:12-jdk-hotspot AS build

COPY ./ /opt/hedera-did

WORKDIR /opt/hedera-did

#RUN ./gradlew --no-daemon

RUN ./gradlew --no-daemon assemble

#
# run in a fresh container, copying files from build container above
#

FROM adoptopenjdk:12-jre-hotspot

# make a place to put our built JAR and copy it to there
WORKDIR /srv
COPY --from=build /opt/hedera-did/examples/appnet-api-server/build/libs/appnet-api-server.jar /srv/appnet-api-server.jar
COPY --from=build /opt/hedera-did/examples/.env /srv/.env
COPY --from=build /opt/hedera-did/previewnet.json /srv/previewnet.json
COPY --from=build /opt/hedera-did/testnet.json /srv/testnet.json
COPY --from=build /opt/hedera-did/mainnet.json /srv/mainnet.json

VOLUME /srv/data

# run the micro service
CMD java "-jar" "appnet-api-server.jar"

EXPOSE 5050
