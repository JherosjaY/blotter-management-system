# CardView + RecyclerView Global Style Guide

## ✅ Global Styles Created

### 1. **CardViewSecondaryBackground**
Use this for empty state backgrounds (the dark card behind content)

### 2. **ReportCardStyle**
Use this for report/item cards in RecyclerViews (lighter color to stand out)

```xml
<androidx.cardview.widget.CardView
    android:id="@+id/emptyStateCard"
    style="@style/CardViewSecondaryBackground"
    android:layout_width="match_parent"
    android:layout_height="450dp"
    android:layout_marginStart="8dp"
    android:layout_marginEnd="8dp"
    android:layout_marginTop="8dp"
    android:layout_marginBottom="16dp"
    android:visibility="visible">
    
    <!-- Empty state content here -->
    
</androidx.cardview.widget.CardView>
```

### 2. **RecyclerViewElevated**
Use this for RecyclerViews that need to appear on top of CardViews

```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerReports"
    style="@style/RecyclerViewElevated"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="@dimen/spacing_medium" />
```

## 📋 How to Apply to New Screens

### Step 1: Wrap in FrameLayout
```xml
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    
    <!-- CardView first (background) -->
    <!-- RecyclerView second (on top) -->
    
</FrameLayout>
```

### Step 2: Add CardView Background
```xml
<androidx.cardview.widget.CardView
    android:id="@+id/emptyStateCard"
    style="@style/CardViewSecondaryBackground"
    android:layout_width="match_parent"
    android:layout_height="450dp"
    android:visibility="visible">
    
    <LinearLayout
        android:id="@+id/emptyState"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:orientation="vertical"
        android:visibility="gone">
        
        <!-- Empty state icon, text, etc. -->
        
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

### Step 3: Add RecyclerView
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerReports"
    style="@style/RecyclerViewElevated"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="@dimen/spacing_medium" />
```

### Step 4: Java Logic
```java
// CardView always visible as background
if (emptyStateCard != null) {
    emptyStateCard.setVisibility(View.VISIBLE);
}

if (dataList.isEmpty()) {
    // Empty state - show empty message, hide RecyclerView
    if (emptyState != null) {
        emptyState.setVisibility(View.VISIBLE);
    }
    if (recyclerView != null) {
        recyclerView.setVisibility(View.GONE);
    }
} else {
    // Has data - hide empty message, show RecyclerView on top of CardView
    if (emptyState != null) {
        emptyState.setVisibility(View.GONE);
    }
    if (recyclerView != null) {
        recyclerView.setVisibility(View.VISIBLE);
    }
}
```

## 🎨 Color Resources

All colors are defined in `colors.xml`:
- `@color/card_background` - Primary dark (#0f172a) for CardView
- `@color/body_background` - Secondary (#1e293b) for screen background
- `@color/card_stroke` - Border color (#475569)

## ✅ Already Applied To:
1. User Dashboard
2. View Reports
3. View Hearings
4. Notifications

## 📝 To Apply to New Screens:
Just use `style="@style/CardViewSecondaryBackground"` and `style="@style/RecyclerViewElevated"`!

No more manual fixing! 🎯
