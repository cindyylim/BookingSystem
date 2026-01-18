# Booking System

A full-stack booking system with:
- Appointment scheduling
- Time slot management
- Email reminders with cancellation links
- User authentication (JWT)
- Admin time slot management
- User dashboard (upcoming/history bookings, profile)

## Features
- Book appointments 
- View available time slots in a calendar (local time zone)
- Email confirmation with cancellation link
- User dashboard: upcoming bookings, history bookings, profile edit, cancel bookings
- Admin: create/delete time slots, admin login/logout

## Tech Stack
- **Backend:** Java 17+, Spring Boot, Spring Security, JPA, PostgreSQL, JWT, JavaMail
- **Frontend:** React, react-big-calendar, date-fns

---

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL

### Backend Setup
1. **Clone the repo and cd into it:**
   ```sh
   git clone <repo-url>
   cd booking-system
   ```
2. **Configure PostgreSQL:**
   - Create `src/main/resources/application.properties`:
```
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/<db-name>
spring.datasource.username=<username>
spring.datasource.password=<password>

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Email (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<your_gmail_address@gmail.com>
spring.mail.password=<your_gmail_app_password>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```
   - Create a database and user (see `src/main/resources/application.properties` for defaults)
   - Example:
     ```sql
     CREATE DATABASE bookingdb;
     CREATE USER bookinguser WITH PASSWORD 'bookingpass';
     GRANT ALL PRIVILEGES ON DATABASE bookingdb TO bookinguser;
     GRANT CREATE ON SCHEMA public TO bookinguser;
     ```
3. **Configure email (Gmail SMTP):**
   - Edit `src/main/resources/application.properties` with your Gmail and app password.
4. **Run database migration for time zone support:**
   ```sql
   ALTER TABLE time_slot
     ALTER COLUMN start_time TYPE timestamptz USING start_time AT TIME ZONE 'UTC',
     ALTER COLUMN end_time TYPE timestamptz USING end_time AT TIME ZONE 'UTC';
   ```
5. **Build and run the backend:**
   ```sh
   mvn clean package
   mvn spring-boot:run
   ```
   The backend runs on [http://localhost:8080](http://localhost:8080)

### Frontend Setup
1. **Install dependencies:**
   ```sh
   cd frontend
   npm install
   ```
2. **Start the frontend:**
   ```sh
   npm start
   ```
   The frontend runs on [http://localhost:3000](http://localhost:3000)

---

## Usage

- **Register/Login:** Create an account or log in to access your dashboard.
- **User Dashboard:** View/cancel/reschedule bookings, edit profile.
- **Admin:** Click "Admin Login" (top right), log in, and manage time slots.
- **Email Reminders:** When booking, you receive an email with a cancellation link.

---

## API Endpoints (Backend)
- `/api/auth/register` — Register user
- `/api/auth/login` — Login (returns JWT)
- `/api/user/profile` — Get/update user profile (JWT required)
- `/api/user/appointments` — Get user bookings (JWT required)
- `/api/appointments` — Book appointment 
- `/api/appointments/cancel/{token}` — Cancel by link
- `/api/timeslots` — Admin time slot management (basic auth)

---

## Notes
- All times are stored and compared in UTC, but displayed in the user's local time zone.
- Admin and user sessions are separate.
- For production, use secure secrets and HTTPS.

