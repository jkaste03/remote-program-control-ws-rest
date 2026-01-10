# Keycloak Configuration

This directory contains the realm configuration for the Remote Program Control application.

## Setup Instructions

1.  **Download Keycloak**: Ensure you have Keycloak 26.4.7 (or a compatible version).
2.  **Import Realm**:
    -   Start Keycloak.
    -   Go to the Admin Console.
    -   Create a new realm (or select "Import Realm").
    -   Select `DAT152-realm.json` from this directory.

## Files

-   `DAT152-realm.json`: The main realm export containing clients, roles, and configuration.
-   `DAT152-users-0.json`: Contains the initial user setup (if exported with users).
