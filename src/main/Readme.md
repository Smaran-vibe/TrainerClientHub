## Trainer Client Hub 

Designed for gym administrators and trainers to manage clients, track workouts, schedule sessions, handle memberships, process payments, and generate analytical reports all from a single cohesive interface.

---

## Features

* Client & Membership Management
* Workout Tracking & Session Scheduling
* Payment Handling & Reports
* Role-based access (Admin / Trainer)
* Runtime theme switching (Dark / Light)

---

## Tech Stack

* Java 21 + JavaFX
* MySQL 8 (JDBC)
* Maven
* jBCrypt

---

## Architecture

Follows clean layered design:

**MVC + Service Layer + DAO**

* Controllers → UI logic
* Services → Business logic
* DAO → Database operations

---

## 📂 Project Structure

```
src/main/java/com/trainerclienthub/
├── controller/
├── service/
├── dao/
├── model/
└── util/
```

---

## Setup

```bash
git clone https://github.com/YOUR_USERNAME/TrainerClientHub.git
cd TrainerClientHub
```

## Dependencies
xml
<!-- pom.xml -->
<dependency>org.openjfx : javafx-controls : 21.0.2</dependency>
<dependency>org.openjfx : javafx-fxml     : 21.0.2</dependency>
<dependency>org.openjfx : javafx-graphics : 21.0.2</dependency>
<dependency>com.mysql   : mysql-connector-j : 8.3.0</dependency>
<dependency>org.mindrot : jbcrypt          : 0.4   </dependency>

## 1. Create database

```sql
CREATE DATABASE trainer_client_hub;
```

```bash
mysql -u root -p trainer_client_hub < database/schema.sql
```

### 2. Configure DB

Edit:

```
src/main/resources/db.properties
```

# 3. Run project

```bash
mvn clean javafx:run
```

---

## Notes

* `.gitignore` excludes credentials and build files

---

## Author

Smaran Aryal 

