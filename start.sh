#!/bin/bash

/home/martin/.jdks/corretto-17.0.6/bin/java \
  --add-opens java.base/java.util=ALL-UNNAMED \
  -Dfile.encoding=UTF-8 \
  -jar /home/martin/pro/avelios/backend/snowstorm/target/snowstorm-7.11.0.jar \
  --elasticsearch.urls=http://localhost:9200
