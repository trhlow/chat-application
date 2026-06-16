# Demo Data

Start MongoDB, Redis and the backend, then run:

```powershell
npm run seed:demo
```

Default API URL: `http://localhost:8080/api`.

Override it when needed:

```powershell
$env:SEED_API_URL="http://localhost:8080/api"
$env:SEED_PASSWORD="Test@123456"
npm run seed:demo
```

## Accounts

All accounts use password `Test@123456`.

| Username | Scenario |
| --- | --- |
| `demo.tui` | Main account used to test the application |
| `demo.an` | Friend, direct-message peer, group admin |
| `demo.binh` | Friend and group member |
| `demo.chi` | Incoming friend request and group member |
| `demo.dung` | Receives a pending friend request from the main account |
| `demo.linh` | Has a pending group invite/join request |

The script is safe to run repeatedly. Existing accounts, friendships, rooms and seeded messages are reused.
When protected REST APIs return `401`, it automatically falls back to the local Docker MongoDB container.

If the backend authentication filter rejects newly issued tokens, the account creation step still succeeds.
Complete the remaining demo data directly in MongoDB:

```powershell
Get-Content -Raw scripts/mongodb/seed-demo-data.js |
  docker exec -i chat-application-mongodb-1 mongosh --quiet `
    "mongodb://root:root@localhost:27017/hlow_chat?authSource=admin"
```
