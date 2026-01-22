# Customer Status Refacor Requirement



### Description
I'd like to refactor the whole customer status feature. 



### Requirement details
- Drop all the current status defainition and status transition logic.
- New status will be : 1. New(新用户） 2. Notified(已提醒复审) 3. Aborted(客户放弃) 4. Submitted(已提交资料) 5.Certified(已发证)
- Newly created user has 'New' status, and it can be changed to any status aftwards.
- Notified, Aborted, Submitted, Certified can not be changed back to 'New' status, but they can switch to any other status.
- There other requirements like UI, status transition history in the customer detail page, keep the same as of now.
- In the search componnet in the customer list, add a new search criteria for status.