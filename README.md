
# Tuple Space Client-Server Application

## Description
This project implements a client-server application based on the tuple space model, allowing multiple clients to interact with a shared key-value data space. Supported operations include:

- **PUT**: Add a key-value pair if the key does not already exist.
- **GET**: Retrieve and remove a key-value pair.
- **READ**: Retrieve a key-value pair without removing it.

The server handles concurrent clients using a fixed thread pool and reports statistics every 10 seconds.

---

## Files Included
- `Server.java`: Main server implementation.
- `Client.java`: Client that connects to the server and sends commands from a request file.
- `client_1.txt`: Sample client request file containing test commands.

---

## How to Run

### 1. Compile the Java Source Files
Use the Java compiler to compile both the server and client:
```bash
javac Server.java
javac Client.java
```

### 2. Start the Server
Start the server with a specified port (e.g., 51234):
```bash
java Server 51234
```
The server will begin listening for client connections and print statistical reports every 10 seconds.

### 3. Run the Client
Open a **new terminal** window (do not close the server) and run the client using the request file `client_1.txt`:
```bash
java Client localhost 51234 client_1.txt
```

The client reads commands from the file and sends them to the server. The server processes each command and returns a response.

---

## Request File Format

Each line in the client request file should start with one of the following commands:
- `P key value` — PUT operation
- `G key` — GET operation
- `R key` — READ operation


---

## Server Output

The server periodically prints runtime statistics:
```
========== Tuple Space Statistics ==========
Tuple count       : 1
Avg tuple size    : 8.00
Avg key size      : 4.00
Avg value size    : 4.00
Client count      : 1
Total operations  : 6
Total PUTs        : 2
Total GETs        : 2
Total READs       : 2
Total ERRs        : 1
============================================
```

---