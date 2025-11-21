# 🔄 Global Loading Manager - Usage Guide

## ✅ ONE-TIME IMPLEMENTATION FOR ALL SCREENS!

Instead of opening every screen, just copy-paste these code snippets wherever you need loading animations.

---

## 📱 Basic Usage

### **Show Loading:**
```java
// Basic loading
GlobalLoadingManager.show(this, "Loading...");

// Cloud operations
GlobalLoadingManager.showCloud(this, "Syncing data");

// Database operations  
GlobalLoadingManager.showDatabase(this, "Saving changes");

// Email operations
GlobalLoadingManager.showEmail(this);
```

### **Hide Loading:**
```java
GlobalLoadingManager.hide();
```

---

## 🚀 Common Scenarios

### **1. Login Process:**
```java
btnLogin.setOnClickListener(v -> {
    GlobalLoadingManager.show(this, "Signing in...");
    
    authViewModel.login(username, password);
    // Hide in success/error callbacks
});
```

### **2. Database Save:**
```java
btnSave.setOnClickListener(v -> {
    GlobalLoadingManager.showDatabase(this, "Saving report");
    
    // Your database operation
    database.reportDao().insertReport(report);
    
    GlobalLoadingManager.hide();
});
```

### **3. Cloud Sync:**
```java
btnSync.setOnClickListener(v -> {
    GlobalLoadingManager.showCloud(this, "Syncing with server");
    
    // Your Firebase/API call
    firebaseFunction.call()
        .addOnSuccessListener(result -> {
            GlobalLoadingManager.hide();
            // Success handling
        })
        .addOnFailureListener(error -> {
            GlobalLoadingManager.hide();
            // Error handling
        });
});
```

### **4. Email Sending:**
```java
btnSendEmail.setOnClickListener(v -> {
    GlobalLoadingManager.showEmail(this);
    
    EmailHelper.sendEmail(context, data);
    
    // Hide after delay or in callback
    new Handler().postDelayed(() -> {
        GlobalLoadingManager.hide();
    }, 2000);
});
```

---

## 🎯 Auto-Timeout (Optional)

```java
// Show loading for 3 seconds then auto-hide
GlobalLoadingManager.showWithTimeout(this, "Processing...", 3000);
```

---

## 💡 Best Practices

### **✅ DO:**
- Always call `hide()` in both success and error cases
- Use specific messages ("Saving report" vs "Loading...")
- Show loading before starting operations
- Use try-catch blocks to ensure loading gets hidden

### **❌ DON'T:**
- Forget to hide loading (causes stuck dialogs)
- Show loading for instant operations
- Use loading for UI-only changes

---

## 🔧 Implementation Examples

### **AuthViewModel (Login):**
```java
// In your login method
GlobalLoadingManager.show(context, "Authenticating...");

// In success callback
GlobalLoadingManager.hide();
Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show();

// In error callback  
GlobalLoadingManager.hide();
Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show();
```

### **Report Creation:**
```java
btnCreateReport.setOnClickListener(v -> {
    GlobalLoadingManager.showDatabase(this, "Creating report");
    
    Executors.newSingleThreadExecutor().execute(() -> {
        try {
            // Database operation
            long reportId = database.reportDao().insertReport(report);
            
            runOnUiThread(() -> {
                GlobalLoadingManager.hide();
                Toast.makeText(this, "Report created!", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                GlobalLoadingManager.hide();
                Toast.makeText(this, "Failed to create report", Toast.LENGTH_SHORT).show();
            });
        }
    });
});
```

---

## 🎨 Customization

The loading dialog uses your existing colors:
- Background: `@color/background_secondary`
- Progress bar: `@color/electric_blue`  
- Text: `@color/text_primary`

---

## 📋 Quick Copy-Paste Templates

### **Template 1: Simple Operation**
```java
GlobalLoadingManager.show(this, "Processing...");
// Your operation here
GlobalLoadingManager.hide();
```

### **Template 2: Async Operation**
```java
GlobalLoadingManager.show(this, "Loading data...");

asyncOperation()
    .onSuccess(() -> {
        GlobalLoadingManager.hide();
        // Success handling
    })
    .onError(() -> {
        GlobalLoadingManager.hide();
        // Error handling
    });
```

### **Template 3: Background Thread**
```java
GlobalLoadingManager.show(this, "Saving...");

Executors.newSingleThreadExecutor().execute(() -> {
    try {
        // Background work
        
        runOnUiThread(() -> {
            GlobalLoadingManager.hide();
            // UI updates
        });
    } catch (Exception e) {
        runOnUiThread(() -> {
            GlobalLoadingManager.hide();
            // Error handling
        });
    }
});
```

---

**🎉 That's it! One implementation, use everywhere! No need to open every screen!**
