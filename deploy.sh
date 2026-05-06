#!/bin/bash
mvn clean package -DskipTests && \
rm -rf MusicDashboard.app && \
JAVA_HOME=$(/usr/libexec/java_home -v 25) jpackage \
  --input target \
  --name MusicDashboard \
  --main-jar MusicDashboard-0.0.1-SNAPSHOT.jar \
  --type app-image \
  --icon /Users/sebastiandorata/IdeaProjects/MusicDashboard/src/main/resources/icons/MusicDashboard.icns \
  --java-options "-DDB_PASSWORD=fxjsl459322749335sd1994" \
  --java-options "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/MusicDashDB" && \
rm -rf /Applications/MusicDashboard.app && \
cp -r MusicDashboard.app /Applications/
