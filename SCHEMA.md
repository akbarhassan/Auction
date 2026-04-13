# Database Schema

Entity relationships for the Auction platform.

## Entity Relationship Diagram

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│  Permission  │◄─────►│     Role     │◄──────│     User     │
└──────────────┘  M:N  └──────────────┘  1:N  └──────────────┘
                                                     │
                       ┌─────────────────────────────┼─────────────────────┐
                       │                             │                     │
                       ▼ 1:1                         ▼ 1:N                 ▼ 1:N
              ┌──────────────┐              ┌──────────────┐       ┌──────────────┐
              │ UserProfile  │              │ AuctionItem  │       │     BID      │
              └──────────────┘              └──────────────┘       └──────────────┘
                                                   │                      │
                                                   │ N:1                  │ N:1
                                                   ▼                      │
                                            ┌──────────────┐              │
                                            │   Category   │              │
                                            └──────────────┘              │
                                                   ▲                      │
                                                   │                      │
                                            ┌──────────────┐              │
                                            │   Auction    │◄─────────────┘
                                            └──────────────┘
                                                   │
                                                   │ 1:1
                                                   ▼
                                            ┌──────────────┐
                                            │ AuctionItem  │
                                            └──────────────┘
```

## Entities

### User
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| email | String | Unique email address |
| password | String | Hashed password |
| emailVerified | Boolean | Email verification status |
| status | UserStatus | ACTIVE, INACTIVE, SUSPENDED |
| deleted | Boolean | Soft delete flag |

**Relationships:**
- `Role` → Many-to-One (each user has one role)
- `UserProfile` → One-to-One (each user has one profile)
- `BID` → One-to-Many (user can place many bids)
- `PasswordHistory` → One-to-Many (tracks password changes)

---

### Role
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| name | String | Unique role name (e.g., ADMIN, BIDDER) |
| description | String | Role description |

**Relationships:**
- `User` → One-to-Many (role has many users)
- `Permission` → Many-to-Many (role has many permissions)

---

### Permission
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| action | String | Permission action (e.g., `auction:create`, `bid:read`) |

**Relationships:**
- `Role` → Many-to-Many (permission belongs to many roles)

---

### UserProfile
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| fullName | String | User's full name |
| mobileNumber | String | Phone number |
| profilePicture | String | Profile image path |
| country | String | Country |
| city | String | City |
| street | String | Street address |
| building | String | Building name/number |
| postalCode | String | Postal code |

**Relationships:**
- `User` → One-to-One (profile belongs to one user)

---

### Category
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| name | String | Unique category name |
| description | String | Category description |
| categoryImage | String | Category image path |

**Relationships:**
- `AuctionItem` → One-to-Many (category has many items)

---

### AuctionItem
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| name | String | Item name |
| sku | String | Unique stock keeping unit |
| description | String | Item description |
| displayImage | String | Main image path |
| galleryImages | List | Additional image URLs |
| status | WareHouseItemsCondition | ON_HOLD, AUCTION, SOLD |

**Relationships:**
- `User` → Many-to-One (item created by user)
- `Category` → Many-to-One (item belongs to category)
- `Auction` → One-to-One (item linked to auction)

---

### Auction
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| startPrice | Float | Starting bid price |
| startsAt | LocalDateTime | Auction start time |
| endsAt | LocalDateTime | Auction end time |
| status | AuctionStatus | PENDING, ACTIVE, ENDED, CANCELLED |
| minimumIncrement | Float | Minimum bid increment |
| currentHighestBid | Float | Current highest bid amount |

**Relationships:**
- `AuctionItem` → One-to-One (auction for one item)
- `User` → Many-to-One (auction created by user)
- `BID` → One-to-Many (auction has many bids)

---

### BID
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| amount | Float | Bid amount |

**Relationships:**
- `User` → Many-to-One (bid placed by user)
- `Auction` → Many-to-One (bid belongs to auction)

## Enums

### UserStatus
- `ACTIVE` - User can access the system
- `INACTIVE` - User account is inactive
- `SUSPENDED` - User is temporarily banned

### AuctionStatus
- `PENDING` - Auction scheduled, not yet started
- `ACTIVE` - Auction is live
- `ENDED` - Auction has finished
- `CANCELLED` - Auction was cancelled

### WareHouseItemsCondition
- `ON_HOLD` - Item available for auction
- `AUCTION` - Item currently in active auction
- `SOLD` - Item has been sold
