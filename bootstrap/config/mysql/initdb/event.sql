CREATE DATABASE IF NOT EXISTS equipment;

USE equipment;

CREATE TABLE IF NOT EXISTS event
(
    event_id        INT AUTO_INCREMENT,
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    equipment_id    INT,
    equipment_type  VARCHAR(20),
    location        VARCHAR(20),
    temperature     DECIMAL(5, 2),
    pressure        DECIMAL(5, 2),
    PRIMARY KEY (event_id)
);
