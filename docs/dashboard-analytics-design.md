# Dashboard & Analytics Feature Design Document

## Executive Summary

This document outlines the comprehensive design for adding Dashboard & Analytics capabilities to the existing CRM system. The design leverages the current architecture while introducing advanced data visualization, real-time insights, and role-based analytics tailored for Admin and Sales personas.

## Current System Architecture Analysis

The existing system provides a solid foundation:
- **Backend**: Spring Boot 3.3.2 with Java 17, PostgreSQL database
- **Frontend**: Next.js 14 with TypeScript, Tailwind CSS + Material Design
- **Security**: JWT-based authentication with Admin/Sales roles
- **Data Model**: customers, sales, status_history tables with comprehensive audit trails
- **Status Workflow**: 7-stage customer lifecycle tracking

## User Personas & Analytics Requirements

### Admin Persona
- **Primary Goals**: Strategic oversight, performance monitoring, resource allocation
- **Key Metrics**: Overall conversion rates, sales team performance, business trends
- **Access Level**: System-wide data across all customers and sales users
- **Dashboard Features**:
  - Company-wide performance metrics
  - Sales team leaderboards and comparisons
  - Revenue and conversion trend analysis
  - Customer acquisition and retention insights

### Sales Persona
- **Primary Goals**: Personal performance tracking, customer pipeline management, quota achievement
- **Key Metrics**: Personal conversion rates, customer status distribution, activity metrics
- **Access Level**: Own customers and related metrics only
- **Dashboard Features**:
  - Personal performance metrics and goals
  - Customer pipeline visualization
  - Activity timeline and task management
  - Individual target tracking and progress

## Database Schema Extensions

### New Analytics Tables

```sql
-- Analytics aggregations table for performance optimization
CREATE TABLE analytics_snapshots (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  snapshot_date DATE NOT NULL,
  sales_phone TEXT, -- NULL for system-wide snapshots
  metric_type VARCHAR(50) NOT NULL, -- 'daily', 'weekly', 'monthly'
  
  -- Customer metrics
  total_customers INTEGER DEFAULT 0,
  new_customers INTEGER DEFAULT 0,
  active_customers INTEGER DEFAULT 0,
  converted_customers INTEGER DEFAULT 0,
  lost_customers INTEGER DEFAULT 0,
  
  -- Status distribution
  customers_called INTEGER DEFAULT 0,
  customers_replied INTEGER DEFAULT 0,
  orders_placed INTEGER DEFAULT 0,
  orders_cancelled INTEGER DEFAULT 0,
  products_delivered INTEGER DEFAULT 0,
  business_done INTEGER DEFAULT 0,
  
  -- Performance metrics
  conversion_rate DECIMAL(5,2),
  avg_cycle_time_days DECIMAL(8,2),
  avg_response_time_hours DECIMAL(8,2),
  
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(snapshot_date, sales_phone, metric_type)
);

-- Customer interaction logs for activity tracking
CREATE TABLE customer_interactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
  sales_phone TEXT NOT NULL,
  interaction_type VARCHAR(50) NOT NULL, -- 'call', 'email', 'meeting', 'note'
  interaction_date TIMESTAMPTZ NOT NULL,
  duration_minutes INTEGER, -- for calls/meetings
  notes TEXT,
  outcome VARCHAR(100), -- 'interested', 'not_interested', 'follow_up_needed'
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Sales goals and targets
CREATE TABLE sales_targets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sales_phone TEXT NOT NULL,
  target_period VARCHAR(20) NOT NULL, -- 'monthly', 'quarterly', 'yearly'
  period_start_date DATE NOT NULL,
  period_end_date DATE NOT NULL,
  
  -- Target metrics
  target_new_customers INTEGER DEFAULT 0,
  target_conversions INTEGER DEFAULT 0,
  target_revenue DECIMAL(12,2) DEFAULT 0,
  
  -- Actual achievements (updated by triggers/batch jobs)
  actual_new_customers INTEGER DEFAULT 0,
  actual_conversions INTEGER DEFAULT 0,
  actual_revenue DECIMAL(12,2) DEFAULT 0,
  
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(sales_phone, target_period, period_start_date)
);

-- Indexes for analytics performance
CREATE INDEX idx_analytics_snapshots_date_sales ON analytics_snapshots(snapshot_date, sales_phone);
CREATE INDEX idx_analytics_snapshots_type ON analytics_snapshots(metric_type);
CREATE INDEX idx_customer_interactions_date ON customer_interactions(interaction_date DESC);
CREATE INDEX idx_customer_interactions_sales ON customer_interactions(sales_phone, interaction_date DESC);
CREATE INDEX idx_sales_targets_period ON sales_targets(sales_phone, target_period, period_start_date);
```

## Backend API Design

### Analytics Controller Structure

```java
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics & Dashboard", description = "APIs for dashboard metrics and analytics")
public class AnalyticsController {
    
    // Dashboard overview endpoints
    @GetMapping("/dashboard/overview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
    public ResponseEntity<DashboardOverviewResponse> getDashboardOverview(
        @RequestParam(defaultValue = "30") int days,
        Authentication authentication
    );
    
    @GetMapping("/dashboard/charts/{chartType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES')")
    public ResponseEntity<ChartDataResponse> getChartData(
        @PathVariable String chartType,
        @RequestParam(defaultValue = "30") int days,
        @RequestParam(required = false) String granularity,
        Authentication authentication
    );
    
    // Customer analytics
    @GetMapping("/customers/conversion-funnel")
    public ResponseEntity<ConversionFunnelResponse> getConversionFunnel(
        @RequestParam(defaultValue = "30") int days,
        Authentication authentication
    );
    
    @GetMapping("/customers/status-distribution")
    public ResponseEntity<StatusDistributionResponse> getStatusDistribution(
        @RequestParam(defaultValue = "current") String period,
        Authentication authentication
    );
    
    @GetMapping("/customers/trends")
    public ResponseEntity<TrendAnalysisResponse> getCustomerTrends(
        @RequestParam(defaultValue = "90") int days,
        @RequestParam(defaultValue = "daily") String granularity,
        Authentication authentication
    );
    
    // Sales performance analytics
    @GetMapping("/sales/performance")
    public ResponseEntity<SalesPerformanceResponse> getSalesPerformance(
        @RequestParam(defaultValue = "30") int days,
        Authentication authentication
    );
    
    @GetMapping("/sales/leaderboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaderboardResponse> getSalesLeaderboard(
        @RequestParam(defaultValue = "30") int days,
        @RequestParam(defaultValue = "conversions") String metric
    );
    
    // Real-time metrics
    @GetMapping("/realtime/metrics")
    public ResponseEntity<RealtimeMetricsResponse> getRealtimeMetrics(
        Authentication authentication
    );
    
    // Export functionality
    @GetMapping("/export/{reportType}")
    public ResponseEntity<byte[]> exportReport(
        @PathVariable String reportType,
        @RequestParam String format, // 'csv', 'pdf', 'excel'
        @RequestParam Map<String, String> filters,
        Authentication authentication
    );
    
    // Goal management
    @GetMapping("/goals")
    public ResponseEntity<List<SalesTargetResponse>> getGoals(
        Authentication authentication
    );
    
    @PostMapping("/goals")
    public ResponseEntity<SalesTargetResponse> createGoal(
        @Valid @RequestBody CreateSalesTargetRequest request,
        Authentication authentication
    );
    
    @PutMapping("/goals/{id}")
    public ResponseEntity<SalesTargetResponse> updateGoal(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateSalesTargetRequest request,
        Authentication authentication
    );
}
```

### Service Layer Design

```java
@Service
public class AnalyticsService {
    
    // Dashboard data aggregation
    public DashboardOverviewResponse getDashboardOverview(String salesPhone, int days);
    public ChartDataResponse getChartData(String chartType, String salesPhone, int days, String granularity);
    
    // Customer analytics
    public ConversionFunnelResponse getConversionFunnel(String salesPhone, int days);
    public StatusDistributionResponse getStatusDistribution(String salesPhone, String period);
    public TrendAnalysisResponse getCustomerTrends(String salesPhone, int days, String granularity);
    
    // Sales performance
    public SalesPerformanceResponse getSalesPerformance(String salesPhone, int days);
    public LeaderboardResponse getSalesLeaderboard(int days, String metric);
    
    // Real-time metrics
    public RealtimeMetricsResponse getRealtimeMetrics(String salesPhone);
    
    // Data aggregation jobs
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void generateDailySnapshots();
    
    @Scheduled(cron = "0 0 3 * * MON") // Weekly on Monday at 3 AM
    public void generateWeeklySnapshots();
    
    @Scheduled(cron = "0 0 4 1 * *") // Monthly on 1st at 4 AM
    public void generateMonthlySnapshots();
}
```

## Frontend Dashboard Components

### Component Architecture

```
src/
├── app/
│   ├── dashboard/
│   │   ├── page.tsx                    # Main dashboard router
│   │   ├── admin/
│   │   │   └── page.tsx               # Admin dashboard
│   │   └── sales/
│   │       └── page.tsx               # Sales dashboard
├── components/
│   ├── dashboard/
│   │   ├── layout/
│   │   │   ├── DashboardLayout.tsx    # Common dashboard layout
│   │   │   ├── DashboardHeader.tsx    # Header with filters
│   │   │   └── DashboardSidebar.tsx   # Navigation sidebar
│   │   ├── widgets/
│   │   │   ├── MetricCard.tsx         # KPI metric cards
│   │   │   ├── TrendWidget.tsx        # Trend visualization
│   │   │   ├── ProgressWidget.tsx     # Progress tracking
│   │   │   ├── TableWidget.tsx        # Data tables
│   │   │   ├── ActivityFeed.tsx       # Activity timeline
│   │   │   └── GoalTracker.tsx        # Goal progress
│   │   ├── charts/
│   │   │   ├── LineChart.tsx          # Time series charts
│   │   │   ├── BarChart.tsx           # Bar/column charts
│   │   │   ├── PieChart.tsx           # Pie/donut charts
│   │   │   ├── FunnelChart.tsx        # Conversion funnel
│   │   │   ├── HeatmapChart.tsx       # Heat map visualization
│   │   │   └── GaugeChart.tsx         # Gauge/meter charts
│   │   └── controls/
│   │       ├── DateRangePicker.tsx    # Date range selection
│   │       ├── FilterPanel.tsx        # Advanced filters
│   │       ├── ExportButton.tsx       # Export functionality
│   │       └── RefreshButton.tsx      # Manual refresh
├── hooks/
│   ├── useAnalytics.ts                # Analytics data fetching
│   ├── useDashboard.ts                # Dashboard state management
│   └── useRealtime.ts                 # Real-time updates
└── types/
    ├── analytics.ts                   # Analytics type definitions
    └── dashboard.ts                   # Dashboard type definitions
```

### Dashboard Layout Wireframes

#### Admin Dashboard
```
┌─────────────────────────────────────────────────────────────┐
│ 🏠 Dashboard  📊 Analytics  👥 Sales Team  ⚙️ Settings      │
├─────────────────────────────────────────────────────────────┤
│ 📅 Last 30 days ▼  🔄 Auto-refresh: ON  📤 Export          │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐             │
│ │Total    │ │New This │ │Conversion│ │Revenue  │             │
│ │Customers│ │Month    │ │Rate     │ │This     │             │
│ │ 1,247   │ │  +89    │ │ 23.4%   │ │ Month   │             │
│ │  +12%   │ │  +15%   │ │  +2.1%  │ │$127,890 │             │
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘             │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────┐ ┌─────────────────────────────────┐ │
│ │ 📈 Customer Trends  │ │ 🎯 Conversion Funnel           │ │
│ │                     │ │                                 │ │
│ │ [Line Chart showing │ │ Customer Called      1,000     │ │
│ │  customer growth    │ │ Replied              750  ██   │ │
│ │  over time]         │ │ Order Placed         450  ██   │ │
│ │                     │ │ Product Delivered    380  ██   │ │
│ │                     │ │ Business Done        234  ██   │ │
│ └─────────────────────┘ └─────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🏆 Sales Team Performance                              │ │
│ │ ┌─────────────┬────────────┬──────────┬──────────────┐ │ │
│ │ │ Salesperson │ Customers  │ Conv.Rate│ This Month   │ │ │
│ │ ├─────────────┼────────────┼──────────┼──────────────┤ │ │
│ │ │ John Smith  │ 45 (+8)    │ 28.9%    │ $23,450      │ │ │
│ │ │ Jane Doe    │ 38 (+12)   │ 31.6%    │ $19,890      │ │ │
│ │ │ Bob Wilson  │ 52 (+5)    │ 19.2%    │ $15,670      │ │ │
│ │ └─────────────┴────────────┴──────────┴──────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### Sales Dashboard
```
┌─────────────────────────────────────────────────────────────┐
│ 🎯 My Dashboard  📊 My Analytics  📞 Activities  🎯 Goals    │
├─────────────────────────────────────────────────────────────┤
│ 📅 This Month  🔄 Last Update: 2min ago  📊 View Report     │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐             │
│ │My       │ │New This │ │My Conv. │ │Goal     │             │
│ │Customers│ │Week     │ │Rate     │ │Progress │             │
│ │   89    │ │  +7     │ │ 26.9%   │ │   73%   │             │
│ │         │ │         │ │  +3.1%  │ │ ████▒▒▒ │             │
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘             │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────┐ ┌─────────────────────────────────┐ │
│ │ 📊 My Pipeline      │ │ 📈 My Performance Trend        │ │
│ │                     │ │                                 │ │
│ │ Called     23       │ │ [Line chart showing weekly     │ │
│ │ Replied    18  ███  │ │  conversion rate and customer  │ │
│ │ Ordered    12  ███  │ │  acquisition trend]            │ │
│ │ Delivered   8  ███  │ │                                 │ │
│ │ Done        7  ███  │ │                                 │ │
│ │                     │ │                                 │ │
│ └─────────────────────┘ └─────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 📋 Recent Activities                                    │ │
│ │ • Called John Anderson - Interested in premium plan    │ │
│ │ • Followed up with Sarah Johnson - Order confirmed     │ │
│ │ • Meeting scheduled with Tech Corp - Tomorrow 2PM      │ │
│ │ • Status updated: Mike Wilson → Product Delivered      │ │
│ │ • New lead: Lisa Chen from Marketing Inc               │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Technical Implementation Roadmap

### Phase 1: Backend Analytics Infrastructure (Weeks 1-3)

**Week 1: Database Schema & Migrations**
- Create analytics_snapshots table
- Create customer_interactions table  
- Create sales_targets table
- Add performance indexes
- Create Flyway migration scripts

**Week 2: Analytics Service Layer**
- AnalyticsService implementation
- MetricsService for real-time calculations
- ReportService for data export
- Data aggregation batch jobs

**Week 3: Analytics Controller & APIs**
- Dashboard endpoints implementation
- Chart data endpoints
- Export functionality
- Role-based access control integration

### Phase 2: Frontend Dashboard Framework (Weeks 3-5)

**Week 3-4: Component Library Setup**
- Chart.js or D3.js integration
- Dashboard layout components
- Responsive grid system
- Theme consistency with existing Material Design

**Week 4-5: Dashboard Pages & Widgets**
- Admin dashboard page implementation
- Sales dashboard page implementation  
- Metric cards and widget components
- Filter controls and date pickers

### Phase 3: Advanced Analytics Features (Weeks 5-7)

**Week 5-6: Chart Components & Visualizations**
- Line charts for trends
- Bar charts for comparisons
- Pie charts for distributions
- Funnel charts for conversions
- Real-time data integration

**Week 6-7: Export & Advanced Features**
- PDF report generation
- Excel/CSV export functionality
- Goal setting and tracking
- Activity timeline components

### Phase 4: Optimization & Polish (Weeks 7-8)

**Week 7: Performance & Mobile**
- Mobile responsiveness optimization
- Data caching implementation
- API performance optimization
- Progressive loading features

**Week 8: Testing & Documentation**
- Unit and integration testing
- User acceptance testing
- API documentation updates
- User guide creation

## Data Flow Architecture

### Analytics Data Processing Flow

1. **Raw Data Collection**
   - Customer CRUD operations → Real-time metrics updates
   - Status transitions → Audit trail and analytics events
   - User interactions → Activity logging

2. **Data Aggregation**
   - Daily batch jobs → Generate snapshots
   - Real-time calculations → Live metrics
   - Historical analysis → Trend computations

3. **API Layer**
   - Role-based data filtering
   - Caching layer integration
   - Response optimization

4. **Frontend Rendering**
   - Chart library integration
   - Progressive data loading
   - Real-time updates via WebSocket

### Performance Optimization Strategy

**Database Level:**
- Pre-aggregated snapshots for historical data
- Materialized views for complex queries
- Proper indexing strategy
- Query optimization

**Application Level:**
- Redis caching for frequently accessed data
- Lazy loading for dashboard components
- API response pagination
- Background job processing

**Frontend Level:**
- Chart data caching
- Progressive component loading
- Mobile-optimized rendering
- Browser storage utilization

## Security & Access Control

### Role-Based Analytics Access

**Admin Access:**
- Full system analytics and metrics
- Sales team performance data
- Customer acquisition and retention insights
- Revenue and business intelligence reports

**Sales Access:**
- Personal performance metrics only
- Own customer pipeline data
- Individual goal tracking
- Activity and interaction history

### Data Security Measures

- **Authentication**: Existing JWT token validation
- **Authorization**: Controller-level role checking
- **Data Filtering**: Service-layer data access control
- **Audit Logging**: Dashboard access tracking
- **Rate Limiting**: API endpoint protection

## Testing Strategy

### Backend Testing
- **Unit Tests**: Service layer logic and calculations
- **Integration Tests**: API endpoints and database queries
- **Performance Tests**: Large dataset handling
- **Security Tests**: Access control validation

### Frontend Testing
- **Component Tests**: Individual widget functionality
- **Integration Tests**: Dashboard page workflows
- **Visual Tests**: Chart rendering and responsiveness
- **E2E Tests**: Complete user journey testing

## Deployment Considerations

### Infrastructure Requirements
- **Database**: Additional storage for analytics tables
- **Memory**: Increased RAM for caching layer
- **Processing**: CPU resources for batch jobs
- **Storage**: Historical data retention

### Monitoring & Alerting
- Dashboard performance metrics
- API response time monitoring
- Data processing job status
- User engagement analytics

### Rollback Strategy
- Feature toggle implementation
- Database migration rollback procedures
- Component-level feature disabling
- Gradual rollout capability

## Success Metrics

### Technical Metrics
- Dashboard load time < 3 seconds
- API response time < 500ms
- 99.9% uptime for analytics services
- Mobile responsiveness score > 95

### Business Metrics
- User adoption rate > 80%
- Daily active dashboard users
- Export feature utilization
- Goal completion tracking accuracy

### User Experience Metrics
- Time to insight < 30 seconds
- User satisfaction score > 4.5/5
- Feature usage analytics
- Support ticket reduction for reporting needs

---

This design document provides the comprehensive blueprint for implementing the Dashboard & Analytics feature while maintaining consistency with the existing CRM architecture and ensuring scalability for future enhancements.