# Android App Utilising The OpenLibrary Api

### This App follows the Clean Architecture Pattern.

### The ViewModel follows a modified MVVM pattern that works with Compose.
- Multiple State Holders are maintained
- The view does not directly call the ViewModel, I modified the traditional MVVM pattern in order to maintain the Compose Screen Unit-Testable
  - With the help of Robolectric the Compose Screen can be unit tested, eliminating the need for an emulator or device.

#### It wasn't a requirement however as the want-to-read endpoint is paginated I decided to handle this case
