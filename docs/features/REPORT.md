# REPORT FEATURE


### Description
I wanna redo the dashboard feature, add more valuable report based on the current data.


### Detail requirements

#### Core Changes
- Rename the 'Dashboard' to 'Reports'/‘统计报表'
- Discard the current customer growth trends
- Add a new customer growth trends by month based on the certificateAt field
- Also, I wanna another growth trends that shows the trends for each certificate types, each trends should be shown in different colors
- In the 'Sales Team Leaderboard', the data should be summarized based on the business manager not the accounts of the system users
- Sales Team Leaderboard, I should be able to choose which month's data I wanna see

#### Additional Statistics Reports

**Certificate Issuer Performance**
- Show trends by certificate issuer over time
- Compare performance across different certificate issuers
- Display issuer rankings and market share
- Include issuer-specific conversion rates

**Certificate Type Popularity**
- Most requested certificate types
- Certificate type distribution and trends
- Popularity ranking with percentages
- Type-specific performance metrics

**Peak Certification Periods**
- Seasonal trends analysis
- Identify peak months/quarters for certifications
- Show historical patterns and cycles
- Predict future high-demand periods

---

## 🎨 Implementation Features

### **Data Persistence & Caching**

#### **SessionStorage Storage**
All dashboard data and filters are stored in `sessionStorage` for:
- Automatic cleanup when window closes (privacy-first)
- State persistence across page refreshes
- Efficient data caching to reduce API calls

**Stored Data Items:**
- `adminDashboardFilters` - Main filters (year, month, view options, certificate types)
- `admin_dashboard_overview` - Dashboard overview metrics cache
- `admin_dashboard_status_distribution` - Status chart data cache
- `admin_dashboard_trends` - Trends chart data cache
- `admin_dashboard_certificate_trends` - Certificate type trends cache
- `admin_dashboard_leaderboard` - Leaderboard data cache
- `admin_dashboard_certificate_type_selections` - Certificate selections state
- `admin_dashboard_state` - Additional dashboard state

**Caching Strategy:**
- Initial load: Use cached data if available
- Filter changes: Only refetch affected data (e.g., leaderboard on year/month change)
- Force refetch: When user explicitly changes dropdown selections
- Logout: Clear all dashboard data from sessionStorage

#### **Individual Loading States**
- Each chart has its own loading state
- No global loading spinner that blocks the entire page
- Charts render independently as their data arrives
- Better user experience with progressive loading

### **Filter Controls**

#### **Customer Trends Chart**
- **View Option Toggle**: Switch between "New Certifications" and "Total Customers"
- Updates chart data and y-axis label accordingly
- State persisted in sessionStorage

#### **Certificate Type Trends Chart**
- **Multi-Select Dropdown**: Choose which certificate types to display
- Color-coded certificate types with consistent color palette
- "All" option to show/hide all types at once
- Individual toggle for each certificate type
- State persisted in sessionStorage

#### **Sales Leaderboard**
- **Year Selector**: Choose which year to view
- **Month Selector**: Choose specific month or "All Months"
- Only triggers leaderboard API call (not all dashboard APIs)
- State persisted in sessionStorage

### **Dashboard Components**

#### **KPI Cards (Top Row)**
1. **Total Customers**: Overall customer count with period-over-period change
2. **New Customers (30 days)**: Recent certifications with change indicator
3. **Active Customers**: Customers with recent activity
4. **Conversion Rate**: Certification conversion percentage with trend

#### **Charts Section (Full Width)**

**Customer Trends Chart**
- Line chart showing customer trends over time
- Toggle between "New Certifications" and "Total Customers" views
- Monthly granularity with date-fns localization
- Latest values summary below chart

**Certificate Type Trends Chart**
- Multi-line chart showing trends by certificate type
- Interactive dropdown to filter which types to display
- Up to 10 different certificate types with distinct colors
- Localized certificate type names
- Tooltips show sorted values by count

**Two-Column Layout**

**Status Distribution Chart**
- Donut/pie chart showing customer status distribution
- Visual breakdown of all customer statuses
- Total customer count displayed

**Sales Leaderboard**
- Ranked list of sales performers
- Year and month filters
- Shows rank, name, customer count, conversions, and conversion rate
- Scrollable list with hover effects

---

## 🏗️ Technical Implementation

### **Files Modified**
- `src/app/dashboard/admin/page.tsx` - Main admin dashboard component
- `src/components/dashboard/charts/TrendLineChart.tsx` - Customer trends chart
- `src/components/dashboard/charts/CertificateTypeTrendsChart.tsx` - Certificate type trends chart
- `src/contexts/AuthContext.tsx` - Logout clears dashboard data

### **API Endpoints Used**
- `GET /api/analytics/dashboard/overview` - Dashboard overview metrics
- `GET /api/analytics/customers/status-distribution` - Status distribution data
- `GET /api/analytics/customers/trends?days=2000&granularity=monthly` - Customer trends
- `GET /api/analytics/customers/trends-by-certificate-type?days=2000` - Certificate type trends
- `GET /api/analytics/sales/leaderboard/monthly?year=&month=&metric=conversions` - Monthly leaderboard
- `GET /api/analytics/sales/leaderboard/yearly?year=&metric=conversions` - Yearly leaderboard

### **State Management**
```typescript
interface StoredFilters {
  selectedYear: number;
  selectedMonth: number | null;
  trendsViewOption: string; // 'newCertifications' or 'totalCustomers'
  certificateTypes: string[]; // Array of selected certificate types

  // Cached data
  overview?: DashboardOverview | null;
  statusDistribution?: StatusDistribution | null;
  trends?: TrendAnalysisResponse | null;
  certificateTrends?: CertificateTypeTrendsResponse | null;
  leaderboard?: LeaderboardResponse | null;
  lastFetchTime?: number;
}
```

---

## 🔄 User Workflow

### **Dashboard Viewing Flow**
1. User navigates to "Reports" page
2. System checks sessionStorage for cached data
3. If cache exists: Display immediately (fast loading)
4. If no cache: Fetch all data from APIs
5. Save data to sessionStorage for next visit

### **Filter Interaction Flow**
1. User changes year/month for leaderboard
2. Only leaderboard API is called (efficient)
3. Leaderboard updates with new data
4. New state saved to sessionStorage
5. Other charts remain unchanged

### **Logout Flow**
1. User clicks logout button
2. All sessionStorage data cleared (including dashboard)
3. Next login starts with fresh state
4. All filters reset to defaults

---

## 📱 Responsive Design

### **Layout Adaptations**
- **Desktop (1024px+)**: Two-column layout for status chart and leaderboard
- **Tablet (640px+)**: Stacked layout with full-width charts
- **Mobile (< 640px)**: Single column, all charts full-width

### **Chart Responsiveness**
- All charts use `maintainAspectRatio: false`
- Heights set explicitly (e.g., `350px`, `80px`)
- Scroll containers for mobile leaderboard
- Responsive tooltips and legends

---

## ✅ Implementation Status

**Status**: ✅ Fully Implemented

**Key Features:**
- ✅ SessionStorage-based caching and persistence
- ✅ Individual loading states per chart
- ✅ Selective API refetching on filter changes
- ✅ Comprehensive filter controls (year, month, view options, certificate types)
- ✅ Interactive and responsive charts
- ✅ Automatic cleanup on logout
- ✅ Multi-language support (English/Chinese)

**Commits:**
- "Fix: Clear admin dashboard state on logout"
- "Fix TypeScript type error in StoredFilters interface"

---

This reports dashboard provides a comprehensive view of customer and sales performance with efficient caching, responsive design, and user-friendly filter controls.