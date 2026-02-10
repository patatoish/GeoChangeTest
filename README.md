# Privacy Test Utility

A minimal Android application for testing privacy methods including VPN logic, DNS resolution changes, and Mock Location detection.

## Features
- **WireGuard VPN**: Integration for using WireGuard tunnels. Supports Mock mode for logic testing without a server.
- **DNS Changer**: Configure DNS settings (simulated or via VPN).
- **Mock Location**: Helper to push mock GPS data and visualize Real vs Mock signals.
- **Self-Test**: Built-in diagnostic tool to generate a JSON report of current privacy states.

## Setup & Usage

### 1. VPN & IP
- **Toggle VPN**: Use the switch on the main screen to enable the VPN service.
- **Import Config**: Click "Import Config" to load a valid WireGuard `.conf` file.
  - *Note*: If no config is imported, the app uses **Mock Mode** (Connected state simulated, no real traffic routing).
- **Status**: View the current internal IP and Country (Mock or Real).

### 2. DNS
- **Select Provider**: Choose from Cloudflare, Google, NextDNS, or Custom.
- **Apply via VPN**: Toggle if the DNS should be applied to the VPN tunnel.

### 3. Mock Location
- **Enable Developer Options**: Go to Settings > About Phone > Tap "Build Number" 7 times.
- **Select Mock App**: In Developer Options, find "Select mock location app" and choose "Privacy Test Utility".
- **Usage**: Toggle "Mock Location Helper" in the app.
  - The app will attempt to push a mock location (Paris, France).
  - The status table will show "Real" (if detectable) and "Mock" coordinates side-by-side.

### 4. Self-Test
- Click **"Run Self-Test"** to generate a JSON report.
- The report verifies:
  - VPN Connection State
  - Location Provider Status
  - DNS Simulation results

## Development
- **Build**: `./gradlew assembleDebug`
- **Architecture**:
  - `MyVpnService`: Handles VpnService lifecycle and Mock logic.
  - `LocationHelper`: Manages FusedLocationProvider and Mock injections.
  - `AppRepository`: Centralizes state for UI.

## Limitations
- **System Timezone**: Cannot be changed without root.
- **SIM Country**: Read-only without root/Magisk.
- **Real GPS**: Might be unavailable if the system fully replaces the provider when Mock is active.
