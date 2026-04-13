# Auction

An online auction platform where users can list items for bidding and participate in auctions.

## Description

Auction is a web-based platform that enables users to buy and sell items through a competitive bidding process. The system manages the complete auction lifecycle from item listing to final sale.

## How It Works

### For Sellers
1. **List Items** - Sellers add items to the warehouse with details like name, description, images, and category
2. **Create Auctions** - Set a starting price, minimum bid increment, start time, and end time for the auction
3. **Monitor Progress** - Track bids and see the current highest bid in real-time

### For Bidders
1. **Browse Auctions** - View active auctions and item details
2. **Place Bids** - Submit bids that must be higher than the current highest bid plus the minimum increment
3. **Get Notified** - Receive email notifications when outbid by another user
4. **Win Items** - The highest bidder when the auction ends wins the item

### Auction Lifecycle
- **Pending** - Auction is scheduled but hasn't started yet
- **Active** - Auction is live and accepting bids
- **Ended** - Auction has closed; the highest bidder wins
- **Cancelled** - Auction was cancelled; item returns to inventory

### User Roles & Permissions
The system supports role-based access control with customizable permissions for different user types (e.g., Admin, Bidder, Seller).

## Getting Started

### Prerequisites
- Java 17+
- Maven
- PostgreSQL database
- SMTP email service (Gmail recommended)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Auction
   ```

2. **Configure environment variables**
   
   Copy the example environment file and fill in your values:
   ```bash
   cp .env.example .env
   ```
   
   Required variables:
   | Variable | Description |
   |----------|-------------|
   | `DB_URL` | PostgreSQL connection URL (e.g., `jdbc:postgresql://localhost:5432/auction`) |
   | `DB_USERNAME` | Database username |
   | `DB_PASSWORD` | Database password |
   | `JWT_SECRET` | Secret key for JWT token signing |
   | `MAILER_USERNAME` | Email address for sending notifications |
   | `MAILER_PASSWORD` | Email app password |

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```
   
   Or on Windows:
   ```cmd
   mvnw.cmd spring-boot:run
   ```

4. **Access the API**
   
   The server runs on `http://localhost:8080` by default.

### API Testing

A Postman collection is included in the `/postman` folder for testing all API endpoints.

## Tech Stack

- **Backend**: Spring Boot 4.0
- **Database**: PostgreSQL
- **Security**: JWT Authentication
- **Email**: Spring Mail with Thymeleaf templates
