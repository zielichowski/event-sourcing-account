CREATE TABLE Event(
    aggregateId UUID NOT NULL,
    sequenceNumber BIGINT NOT NULL,
    transactionId UUID NOT NULL,
    eventType VARCHAR NOT NULL,
    payload BYTEA,
    PRIMARY KEY(aggregateId, sequenceNumber)
);

CREATE INDEX idx_transaction ON Event(aggregateId, transactionId);
