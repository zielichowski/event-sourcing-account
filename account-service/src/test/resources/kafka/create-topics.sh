#!/bin/bash

KAFKA_BROKER="broker1:29092,broker2:29092,broker3:29092"  # Match internal Kafka broker hostnames
TOPICS=(
  "account.events"
)

for TOPIC in "${TOPICS[@]}"; do
  echo "Creating topic: $TOPIC"
  kafka-topics --create --if-not-exists \
    --bootstrap-server $KAFKA_BROKER \
    --replication-factor 1 \
    --partitions 6 \
    --topic "$TOPIC"
done
