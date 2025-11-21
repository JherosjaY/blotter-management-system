# 📧 Firebase Cloud Email Setup Guide

This guide explains how to enable **automatic email sending** for Officer Credentials using Firebase Cloud Functions + SendGrid.

---

## ✅ What's Already Implemented

1. **Email Template** - Beautiful HTML email format
2. **Email Helper Class** - `EmailHelper.java` with cloud-ready code
3. **Firebase Dependencies** - Already added to `build.gradle`
4. **Local Email Intent** - Currently working (opens email app)

---

## 🚀 How to Enable Cloud-Based Email

### Step 1: Setup Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing
3. Add your Android app:
   - Package name: `com.example.blottermanagementsystem`
   - Download `google-services.json`
   - Place in `app/` folder

### Step 2: Setup SendGrid Account

1. Go to [SendGrid](https://sendgrid.com/)
2. Create free account (100 emails/day free)
3. Verify your sender email
4. Create API Key:
   - Settings → API Keys → Create API Key
   - Copy the API key (you'll need it later)

### Step 3: Install Firebase CLI

```bash
npm install -g firebase-tools
firebase login
firebase init functions
```

Select:
- JavaScript or TypeScript
- Install dependencies: Yes

### Step 4: Install SendGrid in Functions

```bash
cd functions
npm install @sendgrid/mail
```

### Step 5: Create Cloud Function

Create `functions/index.js`:

```javascript
const functions = require('firebase-functions');
const sgMail = require('@sendgrid/mail');

// Set SendGrid API Key
sgMail.setApiKey('YOUR_SENDGRID_API_KEY_HERE');

exports.sendEmail = functions.https.onCall(async (data, context) => {
  const { officerName, email, username, password, emailType } = data;
  
  if (emailType === 'officer_credentials') {
    const msg = {
      to: email,
      from: 'noreply@yourbarangay.com', // Must be verified in SendGrid
      subject: 'Your Officer Account Credentials - Blotter Management System',
      html: getOfficerCredentialsHTML(officerName, username, password)
    };
    
    try {
      await sgMail.send(msg);
      return { success: true, message: 'Email sent successfully' };
    } catch (error) {
      console.error('Error sending email:', error);
      throw new functions.https.HttpsError('internal', 'Failed to send email');
    }
  }
});

function getOfficerCredentialsHTML(officerName, username, password) {
  return `
    <!DOCTYPE html>
    <html>
    <head>
      <style>
        body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
        .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; text-align: center; }
        .credentials { background-color: #f8f9fa; border-left: 4px solid #667eea; padding: 20px; margin: 20px 0; }
        .credential-item { margin: 10px 0; }
        .credential-label { font-weight: bold; color: #667eea; }
        .credential-value { font-family: monospace; background-color: #e9ecef; padding: 5px 10px; border-radius: 4px; display: inline-block; }
        .security-tips { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
        .footer { text-align: center; color: #6c757d; font-size: 12px; margin-top: 30px; }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <h1>🔐 Officer Account Created</h1>
          <p>Blotter Management System</p>
        </div>
        <p>Dear Officer <strong>${officerName}</strong>,</p>
        <p>Welcome to the Blotter Management System! Your officer account has been successfully created.</p>
        <div class="credentials">
          <h3>📋 Login Credentials</h3>
          <div class="credential-item">
            <span class="credential-label">Username:</span>
            <span class="credential-value">${username}</span>
          </div>
          <div class="credential-item">
            <span class="credential-label">Password:</span>
            <span class="credential-value">${password}</span>
          </div>
        </div>
        <div class="security-tips">
          <h4>🔐 Security Reminder</h4>
          <ul>
            <li>Please change your password after first login</li>
            <li>Keep your credentials confidential</li>
            <li>Do not share your account with others</li>
            <li>Report any suspicious activity immediately</li>
          </ul>
        </div>
        <h4>📱 How to Login:</h4>
        <ol>
          <li>Open the Blotter Management System app</li>
          <li>Select 'Officer Login'</li>
          <li>Enter your username and password</li>
          <li>Click 'Login'</li>
        </ol>
        <p>If you have any questions or need assistance, please contact your administrator.</p>
        <p>Best regards,<br><strong>Blotter Management System</strong><br>Barangay Administration</p>
        <div class="footer">
          <p>This is an automated message. Please do not reply.</p>
        </div>
      </div>
    </body>
    </html>
  `;
}
```

### Step 6: Deploy Cloud Function

```bash
firebase deploy --only functions
```

### Step 7: Enable Cloud Function in Android App

In `EmailHelper.java`, uncomment the cloud method:

```java
public static void sendOfficerCredentialsEmail(Context context, String officerName, 
                                               String email, String username, String password) {
    // Uncomment this line:
    sendOfficerCredentialsViaCloud(officerName, email, username, password);
    
    // Comment out this line:
    // sendOfficerCredentialsViaIntent(context, officerName, email, username, password);
}
```

Then uncomment the `sendOfficerCredentialsViaCloud()` method (lines 100-122).

### Step 8: Test

1. Create a new officer
2. Click "Send Email"
3. Check the officer's email inbox
4. Email should arrive within seconds!

---

## 📊 Current vs Cloud Comparison

| Feature | Current (Local) | Cloud (Firebase + SendGrid) |
|---------|----------------|----------------------------|
| **Delivery** | Opens email app | Automatic delivery |
| **Speed** | Manual | Instant (< 5 seconds) |
| **Reliability** | Depends on user | 99.9% uptime |
| **Design** | Plain text | Beautiful HTML |
| **Tracking** | None | Open/click tracking |
| **Multi-device** | No | Yes |
| **Offline** | Requires email app | Works always |

---

## 💰 Cost

- **SendGrid Free Tier**: 100 emails/day (forever free)
- **Firebase Functions**: Free tier includes 2M invocations/month
- **Total Cost**: $0 for small barangays!

---

## 🔐 Security Best Practices

1. **Never commit API keys** to Git
2. Use Firebase environment variables:
   ```bash
   firebase functions:config:set sendgrid.key="YOUR_API_KEY"
   ```
3. Enable **email verification** in SendGrid
4. Use **custom domain** for professional emails

---

## 🎯 Next Steps

1. Setup Firebase project
2. Get SendGrid API key
3. Deploy cloud function
4. Test with real email
5. Monitor usage in Firebase Console

---

## 📞 Support

If you need help setting this up, contact your Firebase administrator or check:
- [Firebase Documentation](https://firebase.google.com/docs/functions)
- [SendGrid Documentation](https://docs.sendgrid.com/)

---

**Ready to go cloud? Follow the steps above!** 🚀📧
