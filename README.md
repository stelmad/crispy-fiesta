# Voting Service

A Spring Boot application for processing votes from shareholder meetings. The service provides thread-safe vote processing with validation rules for vote acceptance and modification.

## Overview

The Voting Service is designed to handle vote submissions for shareholder meetings with the following key features:

### Key Business Rules

- New votes (from shareholders who haven't voted) are always accepted
- Vote changes are only allowed if the current date is before the meeting's record date
- Vote changes on or after the record date are rejected
- Invalid proposals (not valid for the given meeting) are rejected

## Prerequisites

- Java 25 or higher
- Maven 3.6+ (or use the included Maven wrapper `mvnw`)

## Building the Project

To build the project, run:

```bash
./mvnw clean install
```

Or on Windows:

```bash
mvnw.cmd clean install
```

This will compile the source code, run all tests, and package the application as a JAR file.

## Running Tests

### Run All Tests

To run all tests (unit and integration tests):

```bash
./mvnw test
```

Or on Windows:

```bash
mvnw.cmd test
```

### Run Only Unit Tests

To run only the unit tests:

```bash
./mvnw test -Dtest=DefaultVotingServiceImplTest
```

### Run Only Integration Tests

To run only the integration tests:

```bash
./mvnw test -Dtest=VotingServiceIntegrationTest
```

### Run a Specific Test Method

To run a specific test method:

```bash
./mvnw test -Dtest=DefaultVotingServiceImplTest#shouldAcceptAndReturnAllVotes
```

## Project Structure

```
src/
├── main/
│   ├── java/dev/stelmad/voting/
│   │   ├── MainApplication.java          # Spring Boot application entry point
│   │   ├── models/
│   │   │   └── Vote.java                 # Vote model
│   │   ├── services/
│   │   │   ├── VotingService.java        # Service interface
│   │   │   └── DefaultVotingServiceImpl.java  # Service implementation
│   │   └── exceptions/
│   │       ├── VotingException.java      # Base exception
│   │       └── InvalidProposalException.java  # Invalid proposal exception
│   └── resources/
│       └── application.yaml              # Application configuration
└── test/
    └── java/dev/stelmad/voting/services/
        ├── DefaultVotingServiceImplTest.java      # Unit tests
        └── VotingServiceIntegrationTest.java      # Integration tests
```