# User Scenarios

> Step-by-step guides for Admin and Customer workflows

---

## 🔧 Admin Setup Guide

Before customers can start bidding, an Admin must set up the platform.

### 1️⃣ Initial Setup

```
Login → Create Roles → Assign Permissions → Create Categories
```

| Step | Action | Endpoint |
|------|--------|----------|
| 1 | Login with admin credentials | `POST /api/v1/auth/login` |
| 2 | Create roles (e.g., BIDDER, SELLER) | `POST /api/v1/roles` |
| 3 | Assign permissions to roles | `PUT /api/v1/roles/{id}` |
| 4 | Create item categories | `POST /api/v1/categories` |
| 5 | Upload category images | `POST /api/v1/categories/{id}/image` |

---

### 2️⃣ Add Auction Items

```
Create Item → Upload Images → Set Category → Item Ready
```

| Step | Action | Details |
|------|--------|---------|
| 1 | **Create auction item** | Add name, SKU, description |
| 2 | **Upload display image** | Main product image |
| 3 | **Add gallery images** | Additional product photos |
| 4 | **Assign category** | Link item to a category |

> 📌 Item status will be `ON_HOLD` until an auction is created

---

### 3️⃣ Create Auction

```
Select Item → Set Prices → Set Schedule → Publish
```

| Field | Description | Example |
|-------|-------------|---------|
| `startPrice` | Opening bid amount | 100.00 |
| `minimumIncrement` | Min bid increase | 5.00 |
| `startsAt` | Auction start time | 2026-04-14T10:00:00 |
| `endsAt` | Auction end time | 2026-04-15T18:00:00 |

> ⏰ Auction automatically goes **ACTIVE** when `startsAt` is reached

---

### 4️⃣ Monitor & Manage

| Action | Description |
|--------|-------------|
| View all auctions | Track active, pending, ended auctions |
| View bid history | See all bids on an auction |
| Cancel auction | Item returns to `ON_HOLD` status |
| Manage users | View, suspend, or delete users |

---

## 🛒 Customer Journey

How customers discover items, place bids, and win auctions.

### 1️⃣ Registration & Login

```
Register → Verify Email → Complete Profile → Start Bidding
```

| Step | Action |
|------|--------|
| 1 | Register with email & password |
| 2 | Check email for verification link |
| 3 | Click link to verify account |
| 4 | Login and complete profile |

---

### 2️⃣ Browse & Search

```
Browse Categories → View Items → Check Auction Details
```

| Action | What You See |
|--------|--------------|
| Browse categories | List of item categories with images |
| View category items | All auction items in that category |
| View auction details | Start price, current bid, time remaining |

---

### 3️⃣ Place a Bid

```
Select Auction → Enter Bid Amount → Confirm → Bid Placed!
```

**Bid Rules:**
- ✅ Bid must be higher than current highest bid
- ✅ Bid must meet minimum increment
- ❌ Cannot bid on your own auction
- ❌ Cannot bid on ended/cancelled auctions

| Scenario | Result |
|----------|--------|
| First bid | Must be ≥ start price |
| Outbid someone | Must be ≥ current bid + min increment |
| Get outbid | Receive email notification |

---

### 4️⃣ Notifications

```
📧 You've Been Outbid!
```

When someone places a higher bid, you receive an email with:

| Info | Description |
|------|-------------|
| Item name | What you were bidding on |
| Your previous bid | Your last bid amount |
| New highest bid | The amount that beat you |
| Minimum to win | What you need to bid next |
| Auction link | Quick link back to the auction |
| Time remaining | When the auction ends |

---

### 5️⃣ Winning

```
Auction Ends → Highest Bidder Wins → Item Marked SOLD
```

| Event | System Action |
|-------|---------------|
| Auction ends | Status changes to `ENDED` |
| Winner determined | Highest bidder at end time |
| Item sold | Item status changes to `SOLD` |

---

## 📊 Quick Reference

### Auction Status Flow

```
PENDING ──────► ACTIVE ──────► ENDED
    │                            │
    │                            ▼
    │                        Item: SOLD
    │
    └──────► CANCELLED
                  │
                  ▼
             Item: ON_HOLD
```

### Item Status Flow

```
ON_HOLD ──────► AUCTION ──────► SOLD
    ▲               │
    │               │ (if cancelled)
    └───────────────┘
```

---

## 🔗 Related Documentation

- [README](README.md) - Project overview & setup
- [Database Schema](SCHEMA.md) - Entity relationships
