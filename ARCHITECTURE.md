# Architecture Overview

## Clean Architecture + MVVM

Echo-Location follows **Clean Architecture** principles combined with **MVVM** (Model-View-ViewModel) pattern for maintainability, testability, and scalability.

## Project Structure

```
app/src/main/kotlin/com/echolocation/app/
├── data/                           # Data Layer
│   └── model/
│       └── Models.kt               # Data classes (Location, NavigationPoint, IndoorMap)
├── domain/                         # Domain Layer (Business Logic)
│   ├── repository/
│   │   └── Repositories.kt         # Repository interfaces
│   └── usecase/
│       └── LocationUseCase.kt      # Use cases
├── presentation/                   # Presentation Layer
│   ├── navigation/
│   │   └── Navigation.kt           # Screen routing
│   ├── screens/
│   │   └── Screens.kt              # Composable screens
│   ├── viewmodel/
│   │   └── LocationViewModel.kt    # State management
│   ├── theme/
│   │   └── Theme files             # Material Design 3
│   └── MainActivity.kt
└── resources/
    └── values/
        ├── strings.xml
        └── themes.xml
```

## Architecture Layers

### 1. Data Layer (`data/`)
**Responsibility**: Handle all data operations

- Models and DTOs
- Repository implementations
- API clients and database access
- Local storage management
- Remote data synchronization

### 2. Domain Layer (`domain/`)
**Responsibility**: Business logic and rules

- Pure Kotlin code (framework-independent)
- Repository interfaces (contracts)
- Use cases (business operations)
- Domain models
- No Android dependencies

### 3. Presentation Layer (`presentation/`)
**Responsibility**: UI and user interaction

- Jetpack Compose UI components
- ViewModels for state management
- Navigation logic
- Theme and styling
- Event handling

## Data Flow

```
┌─────────────────────────────────────────────────┐
│              User Interaction (UI)              │
│           (Button clicks, Input, etc.)          │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│        ViewModel (State Management)              │
│   - Handles UI state                            │
│   - Calls use cases                             │
│   - Emits StateFlow events                      │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│        UseCase (Business Logic)                  │
│   - Orchestrates domain operations              │
│   - Applies business rules                      │
│   - Calls repositories                          │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│      Repository (Data Abstraction)               │
│   - Implements repository interfaces            │
│   - Fetches from DB or API                      │
│   - Data transformation                         │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│         Data Source (DB / API)                   │
│   - Room database                               │
│   - REST API calls                              │
│   - Local storage                               │
└─────────────────────────────────────────────────┘
```

## Key Technologies

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Presentation | Jetpack Compose | Modern UI toolkit |
| Presentation | Navigation Compose | App routing |
| State | ViewModel | Lifecycle-aware state |
| State | StateFlow | Reactive state updates |
| Domain | Kotlin Coroutines | Async operations |
| Data | Room | Local database |
| Data | Retrofit | HTTP client (future) |
| DI | Hilt | Dependency injection (future) |
| Testing | JUnit | Unit tests |
| Testing | Mockito | Mocking framework |
| Testing | Espresso | UI testing |

## Dependency Injection (Future - Hilt)

```kotlin
@HiltViewModel
class LocationViewModel @Inject constructor(
    private val getLocationUseCase: GetCurrentLocationUseCase,
    private val getHistoryUseCase: GetLocationHistoryUseCase
) : ViewModel() {
    // ViewModel code
}
```

## State Management

Using **StateFlow** for reactive state:

```kotlin
private val _currentLocation = MutableStateFlow<Location?>(null)
val currentLocation: StateFlow<Location?> = _currentLocation

// Emit updates
_currentLocation.value = newLocation
```

## Testing Strategy

### Unit Tests
- Use cases
- ViewModels
- Data transformations

### Integration Tests
- Repository layer
- Database operations

### UI Tests
- Compose components
- Navigation flows
- User interactions

## Future Architecture Enhancements

1. **Feature Modules**: Separate features into independent modules
2. **Multi-Module Structure**: `app`, `core`, `feature-location`, `feature-map`, etc.
3. **Advanced DI**: Scoped providers for feature-specific dependencies
4. **Analytics**: Analytics module for event tracking
5. **Networking**: Centralized HTTP client with interceptors
6. **Caching**: Smart caching strategies for maps and data

## Design Principles

- **Single Responsibility**: Each class has one reason to change
- **Open/Closed**: Open for extension, closed for modification
- **Dependency Inversion**: Depend on abstractions, not concrete classes
- **Interface Segregation**: Use focused interfaces
- **Don't Repeat Yourself**: Extract reusable logic
