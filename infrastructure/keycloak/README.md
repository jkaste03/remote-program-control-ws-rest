# Keycloak setup

This repo ships a minimal, conventional realm for local/dev use.

- Client `remote-control-api` is a public PKCE client with redirects to `http://localhost:8090/login/oauth2/code/keycloak` (Spring Boot UI) and `http://localhost:8888/callback` (the agent).
- Client roles: `USER`, `ADMIN` (mapped to the `resource_access.remote-control-api.roles` claim).
- Optional scope `offline_access` is enabled so the agent can request refresh tokens.
- Default users: `admin` / `admin` (ADMIN+USER) and `user` / `user` (USER).
- Import files: `remote-control-realm.json` and `remote-control-users-0.json` (copied by `kc-dev.ps1`).
- Social login: Google IdP is pre-configured; set your Google OAuth credentials in Keycloak after import (`clientId`/`clientSecret` for alias `google`). Default scope: `openid profile email`.

To run Keycloak for dev on Windows:

```powershell
./kc-dev.ps1 -KeycloakHome "C:\keycloak" -HttpPort 8080
```

For production, point `SPRING_SECURITY_OAUTH2_*` issuer URLs in `prod.env` to your real Keycloak base.

Remember to do the following change in Keycloak console: Clients -> remote-control-api -> Client scopes -> remote control api dedicated -> Mappers -> Add mapper -> By configuration -> Sub -> Add to access token (On)
