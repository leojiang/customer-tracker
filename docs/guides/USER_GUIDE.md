# User Guide

> **Complete guide for using the Customer Tracker CRM system effectively**

## 🚀 **Getting Started**

### **🔐 Logging In**
1. **Open the application** at http://localhost:3000
2. **Click "Login"** or navigate to the login page
3. **Enter your credentials**:
   - **Phone Number**: Your registered phone number
   - **Password**: Your secure password
4. **Click "Sign In"** to access the system

**Default Admin Account:**
- **Phone**: `18980994001`
- **Password**: `123456`

### **📝 Creating an Account**
1. **Click "Register"** on the login page
2. **Enter your information**:
   - **Phone Number**: Must be unique in the system
   - **Password**: Create a secure password
   - **Confirm Password**: Re-enter for verification
3. **Click "Create Account"** to register
4. **You'll be logged in automatically** after successful registration

## 👥 **Customer Management**

### **📋 Viewing Customers**

#### **Customer List**
- **Main Dashboard** shows all your customers (or all customers for Admin)
- **Search Bar** allows you to find customers quickly
- **Status Filters** help you focus on specific customer stages
- **Pagination** helps you navigate through large customer lists

#### **Customer Cards**
Each customer displays:
- **👤 Customer Name** and contact information
- **🏢 Company** name (if provided)
- **📞 Phone Number** for contact
- **🏷️ Current Status** with color-coded badge
- **📅 Created Date** for reference

### **🔍 Searching Customers**

#### **Quick Search**
- **Type in the search box** to search across:
  - Customer names
  - Phone numbers  
  - Company names
  - Business requirements
- **Results update instantly** as you type

#### **Advanced Filtering**
- **Status Filter** - View customers in specific stages
- **Company Filter** - Find all customers from specific companies
- **Date Filters** - Find customers created in specific time periods

### **➕ Adding New Customers**

1. **Click "Add Customer"** button
2. **Fill in customer information**:
   - **Name** (Required) - Customer's full name
   - **Phone** (Required) - Must be unique in system
   - **Company** (Optional) - Customer's organization
   - **Business Requirements** (Optional) - What they need
   - **Demographics** (Optional) - Age, education, location, etc.
3. **Click "Save Customer"** to create
4. **Customer starts in "Customer Called" status**

### **✏️ Editing Customers**

1. **Click on any customer** in the list
2. **Customer detail view opens** with full information
3. **Click "Edit Customer"** button
4. **Modify any fields** as needed
5. **Click "Save Changes"** to update
6. **Changes are saved immediately**

## 🔄 **Status Management**

### **📊 Understanding Customer Statuses**

| Status | Meaning | What To Do |
|--------|---------|------------|
| 🔵 **Customer Called** | Initial contact made | Wait for response or follow up |
| 💬 **Replied to Customer** | Customer responded | Work on closing the deal |
| 📦 **Order Placed** | Customer made purchase | Process and fulfill order |
| ❌ **Order Cancelled** | Customer cancelled order | Try to re-engage or mark lost |
| 🚚 **Product Delivered** | Order fulfilled successfully | Confirm completion |
| ✅ **Business Done** | Deal completed | No further action needed |
| 📉 **Lost** | Customer not interested | Can restart process later |

### **🔄 Changing Customer Status**

1. **Open customer details** by clicking on customer
2. **Find the status section** with current status badge
3. **Click "Change Status"** button
4. **Select new status** from dropdown (only valid options shown)
5. **Add reason** (optional but recommended)
6. **Click "Update Status"** to save
7. **Status change is logged** in customer history

#### **⚖️ Status Rules**
- **Forward Movement** - Generally progress through the sales pipeline
- **Recovery Paths** - Can recover from cancellations and lost customers
- **Terminal States** - "Business Done" is final
- **Restart Capability** - Lost customers can restart from beginning

### **📋 Viewing Status History**

1. **Open customer details**
2. **Scroll to "Status History" section**
3. **View complete timeline** of all status changes
4. **See details** including:
   - Previous and new status
   - Date and time of change
   - Reason for change (if provided)
   - Who made the change

## 📊 **Dashboard & Analytics**

### **🏠 Accessing Dashboards**

#### **For Sales Users**
- **Click "Dashboard"** in navigation
- **Personal metrics** and customer pipeline
- **Individual performance** tracking

#### **For Admin Users**  
- **System-wide analytics** and team performance
- **Sales team leaderboard** and comparisons
- **Complete business intelligence** overview

### **📈 Understanding Metrics**

#### **Key Performance Indicators (KPIs)**
- **Total Customers** - All customers you manage
- **New Customers** - Recently added customers (30 days)
- **Active Customers** - Customers with recent activity
- **Conversion Rate** - Percentage reaching "Business Done"

#### **📊 Charts and Visualizations**
- **Status Distribution** - Donut chart showing customer pipeline
- **Growth Trends** - Line chart showing customer acquisition over time
- **Performance Comparison** - Your performance vs. team averages (Sales)
- **Team Rankings** - Leaderboard of sales team performance (Admin)

### **🔄 Refreshing Data**

- **Click "Refresh"** button to update all dashboard data
- **Auto-refresh** keeps data current (configurable)
- **Real-time updates** for critical metrics
- **Last updated** timestamp shows data freshness

## 🎯 **User Roles & Permissions**

### **👨‍💼 Admin Users**

#### **What You Can Do:**
- **👀 View All Customers** - System-wide customer access
- **✏️ Modify All Customers** - Update any customer record
- **📊 System Analytics** - Complete dashboard with team metrics
- **🏆 Team Management** - View sales team performance and rankings  
- **⚙️ System Administration** - Manage users and system settings

#### **📊 Admin Dashboard Features:**
- **System-wide KPIs** - Total customers, conversion rates, growth metrics
- **Sales Team Leaderboard** - Performance rankings by multiple criteria
- **Customer Distribution** - Status breakdown across entire system
- **Trend Analysis** - Historical growth and performance trends

### **💼 Sales Users**

#### **What You Can Do:**
- **👀 View Own Customers** - Access only to your assigned customers
- **✏️ Modify Own Customers** - Update your customer records
- **📊 Personal Analytics** - Individual performance dashboard
- **🎯 Track Goals** - Monitor your progress and achievements
- **📞 Manage Pipeline** - Focus on your sales process

#### **📊 Sales Dashboard Features:**
- **Personal KPIs** - Your customer count, conversion rate, new acquisitions
- **Pipeline Status** - Your customers' status distribution
- **Performance Tracking** - Progress against goals and targets
- **Quick Actions** - Shortcuts to common customer management tasks

### **🔒 What Each Role CANNOT Do**

#### **Sales User Limitations:**
- ❌ **View Other Sales Data** - Cannot see other users' customers or performance
- ❌ **System Administration** - No access to system settings or user management
- ❌ **Team Leaderboard** - Cannot view team performance comparisons
- ❌ **Modify Assignments** - Cannot reassign customers to other sales users

## 💡 **Pro Tips & Best Practices**

### **🎯 Customer Management Tips**

1. **📝 Complete Customer Profiles**
   - Fill in all available customer information
   - Use business requirements field for detailed notes
   - Keep contact information up to date

2. **🔄 Regular Status Updates**
   - Update customer status promptly after interactions
   - Add meaningful reasons for status changes
   - Review pipeline regularly for follow-up opportunities

3. **🔍 Effective Searching**
   - Use partial matches for broad searches
   - Combine filters for precise results
   - Save common searches as bookmarks

### **📊 Dashboard Best Practices**

1. **📈 Monitor Trends**
   - Check dashboard daily for performance insights
   - Track conversion rate trends over time
   - Identify patterns in customer behavior

2. **🎯 Goal Setting**
   - Set realistic monthly targets
   - Track progress regularly
   - Adjust strategies based on data

3. **📋 Pipeline Management**
   - Keep pipeline balanced across statuses
   - Focus on moving customers forward
   - Don't let customers stagnate in early stages

## 🆘 **Troubleshooting**

### **🔐 Login Issues**
- **❌ Invalid Credentials**: Double-check phone number and password
- **❌ Account Locked**: Contact administrator for account reset
- **❌ Page Not Loading**: Check internet connection and try refreshing

### **👥 Customer Management Issues**
- **❌ Cannot Find Customer**: Try broader search terms or check spelling
- **❌ Cannot Update Status**: Ensure transition is valid according to business rules
- **❌ Phone Already Exists**: Each phone number can only be used once

### **📊 Dashboard Issues**
- **❌ No Data Showing**: Click refresh button or check date range filters
- **❌ Charts Not Loading**: Refresh page or check browser compatibility
- **❌ Slow Performance**: Clear browser cache or contact support

### **🛠️ Technical Support**
- **📧 Report Issues**: Use GitHub Issues for bug reports
- **💬 Get Help**: Ask questions in GitHub Discussions
- **📖 Documentation**: Check docs folder for detailed guides

## 🎓 **Advanced Features**

### **📊 Custom Analytics**
- **Date Range Selection** - Analyze specific time periods
- **Metric Comparisons** - Compare different performance indicators
- **Export Capabilities** - Download data for external analysis
- **Historical Analysis** - Review long-term trends and patterns

### **⚙️ Workflow Optimization**
- **Bulk Operations** - Update multiple customers simultaneously (planned)
- **Automated Follow-ups** - System reminders for customer contacts (planned)
- **Integration Capabilities** - Connect with external CRM systems (planned)
- **Mobile Access** - Full functionality on mobile devices

## 📞 **Getting Help**

### **📚 Resources**
- **📖 User Guide** - This document for usage help
- **🔧 Development Guide** - Technical documentation for developers
- **📡 API Reference** - Complete API documentation
- **🎯 Feature Guide** - Detailed feature explanations

### **🆘 Support Channels**
- **GitHub Issues** - Report bugs and feature requests
- **GitHub Discussions** - Ask questions and get community help
- **Documentation** - Comprehensive guides in docs/ folder
- **Admin Contact** - Contact your system administrator for account issues

---

**🎉 You're now ready to effectively use the Customer Tracker CRM to manage your customer relationships and boost your sales performance!**