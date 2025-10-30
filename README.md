The MVVM and MVI patterns are very similar, however MVI has clear advantages.

MVVM calls methods in the ViewModel directly, this means that Compose methods will have a reference of the ViewModel making them hard to test and not possible to test within Robolectric.
MVVM exposes multiple steams of State to the UI, this means that the screen can be updated from a multitude of places making it harder to predict the state of the screen.

MVI calls methods in the ViewModel indirectly via lambda functions, this means that Compose methods do not have a reference to the ViewModel, therefore they can be tested like unit tests.
MVI has a single state object shared with the screen, this reduces complexity.
