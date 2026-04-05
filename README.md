# CodeStorm TravelOptimizer 🚀

A smart travel optimization application that finds the optimal route between any two locations based on multiple criteria (Time, Cost, or Minimum Hops).

## 📋 Table of Contents
- [Features](#features)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [API Endpoints](#api-endpoints)
- [Test Cases](#test-cases)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Contributing](#contributing)

---

## ✨ Features

- **Multi-Criteria Optimization**: Find routes optimized for:
  - ⏱️ **Time** - Minimize total travel time
  - 💰 **Cost** - Minimize total cost
  - 🔄 **Hops** - Minimize number of transfers
  
- **Intelligent Pathfinding**: Uses modified Dijkstra's algorithm with graph traversal
- **Real-time AI Summaries**: Generates natural language trip summaries using Hugging Face API
- **Comprehensive Testing**: 6 test cases with varying complexity
- **REST API**: Spring Boot REST endpoints for easy integration
- **JSON Support**: Full JSON serialization support using Gson

---

## 📁 Project Structure

```
CodeStorm-TravelOptimizer/
├── src/
│   ├── main/
│   │   ├── java/com/nice/avishkar/
│   │   │   ├── Application.java              # Spring Boot Entry Point
│   │   │   ├── TravelOptimizerImpl.java       # Core Algorithm (483 lines)
│   │   │   ├── TravelOptimizerController.java # REST API Controller
│   │   │   ├── ITravelOptimizer.java         # Interface
│   │   │   ├── OptimalTravelSchedule.java    # Data Model
│   │   │   ├── Route.java                    # Route Model
│   │   │   ├── ResourceInfo.java             # Resource Configuration
│   │   │   ├── TestRunner.java               # Test Executor
│   │   │   └── TestRunnerSingle.java         # Single Test Runner
│   │   └── resources/
│   │       ├── application.properties        # App Configuration
│   │       └── TestCase-1 to TestCase-6/    # Test Data (CSV)
│   │
│   └── test/
│       └── java/com/nice/avishkar/
│           └── TravelOptimizerTest.java      # Unit Tests
│
├── pom.xml                                    # Maven Configuration
├── .gitignore                                 # Git Ignore Rules
└── README.md                                  # This File
```

---

## 🛠 Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 8+ (tested on Java 25) |
| **Framework** | Spring Boot | 2.7.14 |
| **Build Tool** | Maven | 3.11.0 |
| **JSON Processing** | Gson | 2.10.1 |
| **Testing** | JUnit 4 + Mockito | 4.12 + 1.10.19 |
| **AI Integration** | Hugging Face API | facebook/bart-large-cnn |

---

## 📦 Prerequisites

- **Java 8 or higher** (tested with Java 25 LTS)
- **Maven 3.6+**
- **Git**
- **Internet Connection** (for Hugging Face API - optional)

### Environment Variables (Optional)
```bash
HF_TOKEN=your_hugging_face_api_token  # For AI summaries
```

---

## 🚀 Installation

### 1. Clone the Repository
```bash
git clone https://github.com/AnushkaTandon26/CodeStorm-TravelOptimizer.git
cd CodeStorm-TravelOptimizer
```

### 2. Build the Project
```bash
mvn clean install
```

### 3. Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

---

## 💻 Usage

### Starting the Server
```bash
mvn spring-boot:run
```

### Running Tests
```bash
# Run all tests
mvn test

# Run with AI summary generation
mvn test -DgenerateSummary=true
```

---

## 📡 API Endpoints

### 1. Health Check
```http
GET /api/travel/health
```
**Response:**
```json
{
  "status": "UP",
  "service": "Travel Optimizer API",
  "version": "1.0.0"
}
```

### 2. Optimize Travel (Without Summary)
```http
POST /api/travel/optimize?schedulesPath=path/to/schedules.csv&requestsPath=path/to/requests.csv
```

**Response:**
```json
{
  "request-id": {
    "routes": [
      {
        "source": "A",
        "destination": "B",
        "mode": "Flight",
        "departureTime": "09:00",
        "arrivalTime": "12:00",
        "cost": 150
      }
    ],
    "criteria": "Time",
    "value": 180,
    "summary": "Not generated"
  }
}
```

### 3. Optimize Travel (With AI Summary)
```http
POST /api/travel/optimize-with-summary?schedulesPath=path/to/schedules.csv&requestsPath=path/to/requests.csv
```

**Response:** Same as above, but `summary` contains AI-generated text (requires HF_TOKEN)

### 4. Run Test Case
```http
GET /api/travel/test-case/{caseNumber}
```
**Parameters:**
- `caseNumber`: 1-6

**Example:**
```http
GET /api/travel/test-case/1
```

---

## 🧪 Test Cases

The project includes **6 comprehensive test cases** with varying complexity:

| Test Case | Description | Routes | Complexity |
|-----------|-------------|--------|-----------|
| **TestCase-1** | Simple routes (A→B→C) | 10 | Low |
| **TestCase-2** | Medium complexity graph | 25 | Medium |
| **TestCase-3** | Complex network | 40+ | High |
| **TestCase-4** | Large dataset | 100+ | Very High |
| **TestCase-5** | Multiple transfer options | 50+ | Very High |
| **TestCase-6** | Dense graph with cycles | 100+ | Very High |

Each test case includes:
- `Schedules.csv` - Transport schedules with routes, times, and costs
- `CustomerRequests.csv` - Travel requests with source, destination, and optimization criteria

### Run All Tests
```bash
mvn test
```

### Run Single Test
```bash
mvn test -Dtest=TravelOptimizerTest#test1
```

---

## ⚙️ Configuration

### application.properties
```properties
# Spring Boot Configuration
spring.application.name=TravelOptimizer
server.port=8080

# Logging
logging.level.root=INFO
logging.level.com.nice.avishkar=DEBUG
```

### Environment Variables
```bash
# For AI Summary Generation (Optional)
export HF_TOKEN=hf_xxxxxxxxxxxxxxxxxxxx

# For API Key (if needed)
export API_KEY=your_api_key
```

---

## 🏗 Architecture

### Algorithm: Modified Dijkstra's Shortest Path
```
Input: Source, Destination, Optimization Criteria
Process:
  1. Build graph from transport schedules
  2. Use priority queue ordered by optimization criteria
  3. Track best states (time, cost, hops)
  4. Avoid revisiting worse states
  5. Return optimal path when destination reached
Output: List of routes with optimized metrics
```

### Key Components

1. **TravelOptimizerImpl** (Core Engine)
   - Graph building
   - Pathfinding algorithm
   - State management
   - Summary generation

2. **TravelOptimizerController** (REST API)
   - Request handling
   - Response formatting
   - AI integration

3. **Data Models**
   - `Route` - Transport connection
   - `Request` - Customer request
   - `State` - Internal state for algorithm
   - `OptimalTravelSchedule` - Result model

---

## 🔐 Security Notes

⚠️ **Important:**
- **Never** commit API keys or secrets to the repository
- Use environment variables for sensitive data
- The HF_TOKEN is read from environment, not hardcoded

---

## 📊 Performance

- **Time Complexity**: O(E log V) where E = edges, V = vertices
- **Space Complexity**: O(V + E) for graph storage
- **Max Hops**: 20 (configurable)
- **Max Iterations**: 50,000 (configurable)

---

## 🧑‍💻 Development

### Build and Run Locally
```bash
# Clean build
mvn clean install

# Run with specific port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"

# Debug mode
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

### Code Quality
```bash
# Run tests with coverage
mvn test jacoco:report

# SonarQube analysis (if configured)
mvn clean verify sonar:sonar
```

---

## 📝 Commit History

```
f5a8ef2 - Initial commit: Travel Optimizer Application - Removed hardcoded API key
```

---

## 🐛 Known Issues

- AI summary generation requires valid Hugging Face API token
- Large datasets (>10,000 routes) may take longer to process
- Some edge cases with midnight crossing times require special handling

---

## 🚀 Future Enhancements

- [ ] Add support for real-time traffic data
- [ ] Implement user authentication
- [ ] Add route preferences (prefer flights, avoid transfers, etc.)
- [ ] Support multi-stop journeys
- [ ] Add mobile application
- [ ] Implement caching for frequently requested routes
- [ ] Add WebSocket support for real-time updates

---

## 📞 Support & Contact

**Author:** Anushka Tandon  
**GitHub:** [@AnushkaTandon26](https://github.com/AnushkaTandon26)  
**Repository:** [CodeStorm-TravelOptimizer](https://github.com/AnushkaTandon26/CodeStorm-TravelOptimizer)

---

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 🙏 Acknowledgments

- Spring Boot Documentation
- Gson Library
- Hugging Face API for NLP capabilities
- Java 8+ Language Features

---

## 📈 Stats

- **Total Lines of Code**: 1000+
- **Java Files**: 9
- **Test Cases**: 6
- **Test Data Points**: 500+
- **Java Version**: 8-25 (Compatible)

---

**Last Updated:** April 5, 2026  
**Status:** ✅ Production Ready
