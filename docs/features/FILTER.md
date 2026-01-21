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