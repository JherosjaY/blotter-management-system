# 🚀 Complete Setup Guide: Render.com + Neon PostgreSQL

## PART 1: Create New PostgreSQL Database on Neon

### Step 1: Sign Up / Login to Neon
1. Go to **https://neon.tech**
2. Click **Sign Up** (or login if you have account)
3. Use GitHub/Google or email
4. Verify email

### Step 2: Create New Project
1. Click **New Project**
2. Enter project name: `blotter-management-db`
3. Select **PostgreSQL 15** (latest)
4. Select region closest to you (Singapore recommended for PH)
5. Click **Create Project**

### Step 3: Get Connection String
1. After project created, click on project
2. Go to **Connection string** tab
3. Copy the **Connection String** (looks like):
```
postgresql://user:password@ep-xyz.us-east-1.neon.tech/dbname?sslmode=require
```
4. **Save this somewhere safe!** You'll need it for Render

### Step 4: Create Tables (Optional - Backend will auto-create)
If you want to pre-create tables:
1. Click **SQL Editor**
2. Run your schema SQL (or let backend create via migrations)

---

## PART 2: Create New Web Service on Render.com

### Step 1: Sign Up / Login to Render
1. Go to **https://render.com**
2. Click **Sign Up** (or login)
3. Use GitHub/Google or email
4. Verify email

### Step 2: Create New Web Service
1. Click **New +** button (top right)
2. Select **Web Service**
3. Choose **Deploy an existing repository** OR **Create new repo**

### Step 3: Connect GitHub Repository
1. Click **Connect GitHub account**
2. Authorize Render to access your repos
3. Select your **blotter-backend** repository
4. Click **Connect**

### Step 4: Configure Web Service
1. **Name**: `blotter-backend` (or your choice)
2. **Environment**: `Node` (if Node.js) or `Docker` (if Java/Spring)
3. **Build Command**: 
   - For Node: `npm install`
   - For Java: `mvn clean package`
4. **Start Command**:
   - For Node: `npm start`
   - For Java: `java -jar target/app.jar`
5. **Plan**: Select **Free** or **Starter** ($7/month)

### Step 5: Add Environment Variables
1. Scroll to **Environment** section
2. Click **Add Environment Variable**
3. Add these variables:

```
DATABASE_URL = postgresql://user:password@ep-xyz.us-east-1.neon.tech/dbname?sslmode=require
NODE_ENV = production
PORT = 10000
```

Replace `DATABASE_URL` with your Neon connection string from Step 3 above!

### Step 6: Deploy
1. Click **Create Web Service**
2. Render will automatically deploy from your GitHub repo
3. Wait for deployment to complete (2-5 minutes)
4. You'll get a URL like: `https://blotter-backend.onrender.com`

### Step 7: Monitor Deployment
1. Go to **Logs** tab to see deployment progress
2. Look for "Build successful" message
3. Check **Live** tab to test your API

---

## PART 3: Update Your Android App

### Update ApiClient.kt
```kotlin
object ApiClient {
    // Use your new Render URL here!
    private const val BASE_URL = "https://blotter-backend.onrender.com/"
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun getApiService(): ApiService = retrofit.create(ApiService::class.java)
    
    fun getAllReports(callback: ApiCallback<List<BlotterReport>>) {
        getApiService().getAllReports().enqueue(object : Callback<List<BlotterReport>> {
            override fun onResponse(call: Call<List<BlotterReport>>, response: Response<List<BlotterReport>>) {
                if (response.isSuccessful && response.body() != null) {
                    callback.onSuccess(response.body()!!)
                } else {
                    callback.onError("Failed to fetch reports")
                }
            }

            override fun onFailure(call: Call<List<BlotterReport>>, t: Throwable) {
                callback.onError(t.message ?: "Unknown error")
            }
        })
    }

    interface ApiCallback<T> {
        fun onSuccess(data: T)
        fun onError(errorMessage: String)
    }
}
```

---

## PART 4: Backend Setup (Node.js Example)

If you're using Node.js/Express:

### package.json
```json
{
  "name": "blotter-backend",
  "version": "1.0.0",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "pg": "^8.10.0",
    "cors": "^2.8.5",
    "dotenv": "^16.3.1"
  }
}
```

### server.js
```javascript
const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
require('dotenv').config();

const app = express();
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
});

app.use(cors());
app.use(express.json());

// Test connection
pool.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error('Database connection error:', err);
  } else {
    console.log('✅ Connected to Neon PostgreSQL');
  }
});

// API Routes
app.get('/api/reports', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM blotter_reports');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/reports', async (req, res) => {
  try {
    const { caseNumber, complainant, status } = req.body;
    const result = await pool.query(
      'INSERT INTO blotter_reports (case_number, complainant, status, created_at) VALUES ($1, $2, $3, NOW()) RETURNING *',
      [caseNumber, complainant, status]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/reports/:id', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM blotter_reports WHERE id = $1', [req.params.id]);
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/reports/:id', async (req, res) => {
  try {
    const { caseNumber, complainant, status } = req.body;
    const result = await pool.query(
      'UPDATE blotter_reports SET case_number = $1, complainant = $2, status = $3, updated_at = NOW() WHERE id = $4 RETURNING *',
      [caseNumber, complainant, status, req.params.id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/reports/:id', async (req, res) => {
  try {
    await pool.query('DELETE FROM blotter_reports WHERE id = $1', [req.params.id]);
    res.json({ message: 'Report deleted' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Witnesses
app.get('/api/witnesses/:reportId', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM witnesses WHERE blotter_report_id = $1', [req.params.reportId]);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/witnesses', async (req, res) => {
  try {
    const { blotterReportId, name, address, contactNumber, statement } = req.body;
    const result = await pool.query(
      'INSERT INTO witnesses (blotter_report_id, name, address, contact_number, statement, created_at) VALUES ($1, $2, $3, $4, $5, NOW()) RETURNING *',
      [blotterReportId, name, address, contactNumber, statement]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Suspects
app.get('/api/suspects/:reportId', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM suspects WHERE blotter_report_id = $1', [req.params.reportId]);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/suspects', async (req, res) => {
  try {
    const { blotterReportId, name, alias, address, description } = req.body;
    const result = await pool.query(
      'INSERT INTO suspects (blotter_report_id, name, alias, address, description, created_at) VALUES ($1, $2, $3, $4, $5, NOW()) RETURNING *',
      [blotterReportId, name, alias, address, description]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Evidence
app.get('/api/evidence/:reportId', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM evidence WHERE blotter_report_id = $1', [req.params.reportId]);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/evidence', async (req, res) => {
  try {
    const { blotterReportId, evidenceType, description, collectedBy } = req.body;
    const result = await pool.query(
      'INSERT INTO evidence (blotter_report_id, evidence_type, description, collected_by, collected_date) VALUES ($1, $2, $3, $4, NOW()) RETURNING *',
      [blotterReportId, evidenceType, description, collectedBy]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Hearings
app.get('/api/hearings/:reportId', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM hearings WHERE blotter_report_id = $1', [req.params.reportId]);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/hearings', async (req, res) => {
  try {
    const { blotterReportId, hearingDate, hearingTime, location, purpose } = req.body;
    const result = await pool.query(
      'INSERT INTO hearings (blotter_report_id, hearing_date, hearing_time, location, purpose, created_at) VALUES ($1, $2, $3, $4, $5, NOW()) RETURNING *',
      [blotterReportId, hearingDate, hearingTime, location, purpose]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Resolutions
app.get('/api/resolutions/:reportId', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM resolutions WHERE blotter_report_id = $1', [req.params.reportId]);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/resolutions', async (req, res) => {
  try {
    const { blotterReportId, type, details } = req.body;
    const result = await pool.query(
      'INSERT INTO resolutions (blotter_report_id, type, details, resolved_date, created_at) VALUES ($1, $2, $3, NOW(), NOW()) RETURNING *',
      [blotterReportId, type, details]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`✅ Server running on port ${PORT}`);
  console.log(`📍 API: https://blotter-backend.onrender.com`);
});
```

### .env
```
DATABASE_URL=postgresql://user:password@ep-xyz.us-east-1.neon.tech/dbname?sslmode=require
NODE_ENV=production
PORT=10000
```

---

## PART 5: Database Schema (SQL)

Run this in Neon SQL Editor to create tables:

```sql
-- Blotter Reports
CREATE TABLE blotter_reports (
  id SERIAL PRIMARY KEY,
  case_number VARCHAR(50) UNIQUE NOT NULL,
  complainant VARCHAR(255) NOT NULL,
  status VARCHAR(50) DEFAULT 'PENDING',
  assigned_officer_id INTEGER,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Witnesses
CREATE TABLE witnesses (
  id SERIAL PRIMARY KEY,
  blotter_report_id INTEGER NOT NULL REFERENCES blotter_reports(id),
  name VARCHAR(255) NOT NULL,
  address TEXT,
  contact_number VARCHAR(20),
  statement TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Suspects
CREATE TABLE suspects (
  id SERIAL PRIMARY KEY,
  blotter_report_id INTEGER NOT NULL REFERENCES blotter_reports(id),
  name VARCHAR(255) NOT NULL,
  alias VARCHAR(255),
  address TEXT,
  description TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Evidence
CREATE TABLE evidence (
  id SERIAL PRIMARY KEY,
  blotter_report_id INTEGER NOT NULL REFERENCES blotter_reports(id),
  evidence_type VARCHAR(50),
  description TEXT,
  collected_by VARCHAR(255),
  collected_date TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Hearings
CREATE TABLE hearings (
  id SERIAL PRIMARY KEY,
  blotter_report_id INTEGER NOT NULL REFERENCES blotter_reports(id),
  hearing_date VARCHAR(50),
  hearing_time VARCHAR(50),
  location VARCHAR(255),
  purpose TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Resolutions
CREATE TABLE resolutions (
  id SERIAL PRIMARY KEY,
  blotter_report_id INTEGER NOT NULL REFERENCES blotter_reports(id),
  type VARCHAR(100),
  details TEXT,
  resolved_date TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for faster queries
CREATE INDEX idx_reports_status ON blotter_reports(status);
CREATE INDEX idx_witnesses_report ON witnesses(blotter_report_id);
CREATE INDEX idx_suspects_report ON suspects(blotter_report_id);
CREATE INDEX idx_evidence_report ON evidence(blotter_report_id);
CREATE INDEX idx_hearings_report ON hearings(blotter_report_id);
CREATE INDEX idx_resolutions_report ON resolutions(blotter_report_id);
```

---

## PART 6: Deploy & Test

### Deploy Steps:
1. Push your backend code to GitHub
2. Render auto-deploys on push
3. Check deployment status in Render dashboard
4. Get your live URL: `https://blotter-backend.onrender.com`

### Test Your API:
```bash
# Test GET
curl https://blotter-backend.onrender.com/api/reports

# Test POST
curl -X POST https://blotter-backend.onrender.com/api/reports \
  -H "Content-Type: application/json" \
  -d '{"caseNumber":"2024-001","complainant":"John Doe","status":"PENDING"}'
```

### Update Android App:
```kotlin
private const val BASE_URL = "https://blotter-backend.onrender.com/"
```

---

## ✅ Checklist

- [ ] Create Neon PostgreSQL database
- [ ] Get Neon connection string
- [ ] Create Render web service
- [ ] Add DATABASE_URL environment variable
- [ ] Deploy backend to Render
- [ ] Create database tables in Neon
- [ ] Test API endpoints
- [ ] Update ApiClient.kt with new BASE_URL
- [ ] Deploy Kotlin app to Google Play / Test APK

---

## 🎉 You're Done!

Your Blotter Management System is now:
✅ **Fully Cloudbase** with Render.com + Neon PostgreSQL
✅ **Offline-First** with local Room database
✅ **API-Integrated** with automatic sync
✅ **Production-Ready** and scalable

**Live URL**: `https://blotter-backend.onrender.com`
**Database**: Neon PostgreSQL (Singapore region)
