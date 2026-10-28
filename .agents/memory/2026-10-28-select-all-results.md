# Select all in Results selection mode

- Added a **Select all** action to the Results top bar while selection mode is active.
- Added `ResultsViewModel.onSelectAll()` to select every result currently visible after applying the active search, filter, and sort controls.
- Kept selections that are currently hidden by a filter or search when selecting all visible results.
- Added a unit test covering selection of all visible completed results.
- Added the English `action_select_all` string resource.
