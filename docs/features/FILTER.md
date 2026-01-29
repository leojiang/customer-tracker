# Filter enhancement feature

### Description
I'd like to add more filters to the search component so that I can search customer by certified date range, created date range. These filters can work together with the current search criteria feature.


### Requirement detail
- For the current search, make a change so that it can only search by name or phone number.
- Below the current earch input field, add a new section named 'Cerfitifed time range', give it two time picker for the user to pick a start date and an end date.
- If the time range and the search text are both specified, then the search results should match both.
- If time range is not specified, then only search user with the text inputed.
- If search text is not specified, then only search with the time range and return all the cusomters amtch the time range.
- The result should be sorted by the certified date DES no matter the time range for search is speficifed or not.

### Additional Features

#### **Responsive Design**
- Search bar is fully responsive across all device sizes
- **Mobile (< 640px)**: Full-width stacked elements
- **Tablet (640px+)**: 2-3 column grid layout
- **Desktop (1024px+)**: Full 12-column grid layout
- Buttons expand to full width on mobile for easier tapping
- Responsive padding: 16px mobile → 24px tablet → 32px desktop

#### **Filter Persistence**
- All filter selections are stored in `sessionStorage`
- Filters persist across page refreshes
- Filters are automatically cleared when browser window/tab is closed
- Filter state includes:
  - Search term (name or phone)
  - Status filter
  - Certificate type filter
  - Certificate issuer filter
  - Customer agent filter (Admin/Officer only)
  - Certified date range (start and end dates)
  - Page number and page size

#### **Smart Phone Number Detection**
- Automatically detects if search input is a phone number
- Routes phone searches to `phone` parameter API
- Routes text searches to `q` parameter API
- Uses regex pattern: `^[\d+s\-()]+$` with minimum 3 digits

#### **Available Filters**
1. **Search Input**: Name or phone number search
2. **Status**: Customer status dropdown (All, Certified, Submitted, Notified, Aborted)
3. **Certificate Type**: Certificate type dropdown (All certificate types available)
4. **Certificate Issuer**: Issuer organization dropdown (All issuers available)
5. **Customer Agent**: Agent name search (Admin and Officer only)
6. **Certified Date Range**: Start date and end date pickers with clear buttons

### Implementation Details

**Files Modified:**
- `src/components/customers/CustomerList.tsx`

**Storage Key:**
- `customerListFilters` in sessionStorage

**Filter State Structure:**
```typescript
interface StoredFilters {
  searchTerm?: string;
  certifiedStartDate?: string;
  certifiedEndDate?: string;
  selectedCertificateType?: string;
  selectedStatus?: string;
  certificateIssuer?: string;
  customerAgent?: string;
  page?: number;
  pageSize?: number;
}
```

### User Experience

**Filter Behavior:**
- Real-time filter application on search button click
- Clear button resets all filters to default state
- Individual filters can be cleared using X buttons (for dates)
- Page state maintains across filter changes
- Smooth transitions with loading states

**Responsive Behavior:**
- Mobile: Filters stack vertically for easy scrolling
- Desktop: Filters organized in efficient grid layout
- Buttons full-width on mobile for better touch targets
- Search and clear buttons always easily accessible

**Data Persistence:**
- Filters saved automatically on every change
- Restored when returning to customer list
- Cleared on logout or window close
- No persistent storage for privacy