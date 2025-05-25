# Ticket Selling System

This application simulates a ticket selling system, allowing for the management of events, customers, and ticket purchases. It's designed with a focus on demonstrating a multithreaded approach to generating unique prime IDs for entities to enhance performance under concurrent load.

## How it Works

The system is composed of several key components:

*   **`TicketShop`**: Acts as the main facade. Client code interacts with the `TicketShop` to access various services for managing events, customers, and tickets.
*   **`EventService`**: Handles all event-related operations, including creating new events, updating existing ones, deleting events, and retrieving event information. Events are stored in a thread-safe `ConcurrentHashMap`.
*   **`CustomerService`**: Manages customer data. This includes adding new customers, updating their details, deleting customers, and fetching customer information. Customer data is also stored in a `ConcurrentHashMap`.
*   **`TicketService`**: Responsible for the lifecycle of tickets. This includes purchasing tickets (which links a customer to an event), validating tickets, and deleting tickets. Ticket information is managed in a `ConcurrentHashMap`.
*   **`SharedIDService`**: A crucial backend service that generates unique, 10-digit prime numbers to be used as IDs for events, customers, and tickets. This service is designed for high performance in concurrent environments (see details below).

Users can interact with the system in a couple of ways:

*   **`CLIClient`**: A command-line interface that allows users to manually manage events, customers, and tickets through text-based commands.
*   **`PerformanceClient`**: A utility (currently run by default in `Main.java`) designed to simulate load on the system by creating multiple events and customers, and then simulating ticket purchases. This helps in evaluating the system's behavior under stress.

## Running the Application

Follow these steps to build and run the application:

**1. Build Instructions:**

To build the project, navigate to the root directory of the project in your terminal and run the following Maven command:

```bash
mvn clean install
```

This command will compile the source code, run any tests, and package the application into a JAR file. The resulting JAR file will be located in the `target/` directory (e.g., `EVA-1.0-SNAPSHOT.jar`).

**2. Execution:**

To run the application, use the following command in your terminal from the project's root directory:

```bash
java -jar target/EVA-1.0-SNAPSHOT.jar
```

By default, the `Main.java` class is configured to run the `PerformanceClient`, which will execute a series of automated operations to simulate load and test the system's performance.

If you wish to interact with the system manually using the command-line interface, you will need to modify the `Main.java` file to instantiate and start `CLIClient` instead of `PerformanceClient`.

## Performance Improvement with Multithreading

A key feature of this application is its approach to generating unique IDs, which is handled by the `SharedIDService`.

**Role of `SharedIDService`:**
The `SharedIDService` is responsible for providing unique, 10-digit prime number IDs for all entities (events, customers, tickets). Using prime numbers as IDs is a specific requirement, and generating them efficiently is critical for system performance.

**Challenge Addressed:**
Generating a new prime number on-demand every time an event, customer, or ticket is created can be computationally expensive and slow. This can quickly become a performance bottleneck, especially when multiple users or system processes are trying to create entities concurrently.

**Solution - Producer-Consumer Architecture with Background Prime Generation:**

The `SharedIDService` implements a producer-consumer pattern to mitigate this bottleneck:

1.  **ID Queue (`idQueue`)**: A `java.util.concurrent.BlockingQueue<Long>` (specifically, an `ArrayBlockingQueue`) is used to store a pool of pre-generated prime IDs. This queue acts as a buffer between the ID generation process and the consuming services.

2.  **ID Consumption (`getNew()`)**: When a service (like `EventService`) needs a new ID, it calls `SharedIDService.getInstance().getNew()`. This method attempts to take an ID from the `idQueue`.
    *   If the queue is not empty, an ID is returned almost immediately.
    *   If the queue is empty, the calling thread blocks and waits until a new prime ID is generated and added to the queue.

3.  **Background Prime Generation (Producers)**:
    *   **Orchestrator (`primeGeneratorExecutor`)**: A dedicated single-thread executor manages the prime generation process. It continuously monitors the `idQueue`.
    *   **Worker Pool (`primeSearcherPool`)**: A fixed-size thread pool (its size dynamically configured based on the number of available processors) is responsible for the actual prime searching.
    *   **Replenishment Logic**: When the number of IDs in the `idQueue` drops below a defined threshold (`QUEUE_LOW_WATER_MARK`), the orchestrator triggers the worker pool to generate more primes.
    *   **Segmented Sieve of Eratosthenes**: Each worker thread in the `primeSearcherPool` is assigned a specific numerical range (a "segment"). It then uses the `JavaSegmentedSieveGenerator` class, which implements the Sieve of Eratosthenes algorithm optimized for segments, to find all prime numbers within its assigned range. This parallelizes the computationally intensive task of prime searching.
    *   The prime numbers found by the workers are then collected by the orchestrator and added to the `idQueue`, replenishing the supply available for consumption.

**Benefits of this Approach:**

*   **Decoupling**: The process of generating IDs is decoupled from the process of requesting/consuming IDs. Services needing IDs don't have to wait for the prime calculation itself in most cases.
*   **Reduced Latency**: For most ID requests, the latency is very low because the ID is simply retrieved from the front of the queue.
*   **Improved Throughput**: The system can handle a higher rate of entity creation requests because the ID generation bottleneck is significantly reduced. The background threads work proactively to ensure IDs are available.
*   **Scalability**: The use of a multi-threaded worker pool for prime searching allows the system to leverage multi-core processors to speed up ID generation when the demand is high.

**Other Thread Safety Considerations:**

Beyond the `SharedIDService`, other services like `EventService`, `CustomerService`, and `TicketService` also employ thread-safe mechanisms for managing their respective data. They primarily use `java.util.concurrent.ConcurrentHashMap` to store entities (events, customers, tickets). This allows multiple threads to safely access and modify this data concurrently, for example, when multiple users are trying to book tickets for different events simultaneously or when the `PerformanceClient` simulates concurrent operations.Okay, the `README.md` file has been created with the specified content. I've covered the project overview, how it works, build and run instructions, and a detailed explanation of the `SharedIDService`'s multithreaded architecture and its benefits. I also included a note on the general thread safety in other services due to `ConcurrentHashMap`.

I will now submit the subtask report.
