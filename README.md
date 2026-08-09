# Lapis-AuthmeReloadedFix

> Companion plugin for **AuthMeReloaded 6.x** that restores the useful fixes AuthMe dropped in its
> 6.0 rewrite, and adds first-class support for **FakePlayerPlugin (FPP) bots**.

Official AuthMeReloaded project: <https://github.com/AuthMe/AuthMeReloaded>  
This plugin is **not** an AuthMe fork - it runs **alongside** AuthMe and only uses its public API.

If you are using this, do **NOT** ask AuthMe for support about behaviour provided by this plugin.

## Why use Lapis-AuthmeReloadedFix?

AuthMeReloaded 6.0 was a big architecture rewrite (multi-module, PlatformAdapter, PacketEvents). Several
behaviours that server owners relied on disappeared in the process:

* the **anti ghost-player / double-login** listener,
* **Bedrock (Floodgate) auto-login**,
* the bundled **PlaceholderAPI expansion** (`%authme_*%`),
* and any useful way to stop **FakePlayerPlugin bots** from being kicked by AuthMe's login/registration
  timeout.

AuthMe 6 also made `forceLogin` a **no-op for players who don't have a database account**, which means fake-players
never had a way to authenticate. This plugin fixes exactly that.

It provides:

- **Anti ghost-player fix** - if a player with the same name is already online, the stale session is kicked
  (FPP bots are never kicked).
- **Floodgate auto-login** - Bedrock players are automatically logged in on join.
- **PlaceholderAPI integration** - `%authme_isloggedin%`, `%authme_isregistered%`, `%authme_email%`,
  `%authme_lastlogin%`, `%authme_lastip%`, `%authme_regdate%`, `%authme_realname%` and more.
- **FPP bot compatibility** - bots are registered with a short-lived account and auto-logged-in so AuthMe
  never times them out. The account is removed when the bot leaves and stale accounts survive crash/restart
  cleanup.

### Requirements

* **AuthMeReloaded 6.0 or newer.** This plugin does nothing without AuthMe. It works against both the `AuthMeApi`
  (5.x) and `AuthmeApi` (6.x) API classes via a reflection bridge.
* **A Paper/Folia-based server.** Must be compatible with `Paper 1.21+`; Folia is supported and detected at runtime.
* **Java 21 or higher.**

### Soft dependencies (optional, feature-gated at runtime):

| Plugin            | Enables                              |
|:------------------|:-------------------------------------|
| `floodgate`       | Bedrock auto-login                    |
| `FakePlayerPlugin`| FPP bot auto-login / cleanup         |
| `PlaceholderAPI`  | `%authme_*%` placeholders              |

If a soft dependency is missing its corresponding module is simply skipped.

---

## Building

You need **JDK 21** to build this plugin.

Clone this repository, then run:

* On Linux / macOS: `./gradlew build`
* On Windows: `gradlew build`

You can then find the plugin jar in the `build/libs/` directory.

---

## Configuration

All features can be toggled in `plugins/Lapis-AuthmeReloadedFix/config.yml`:

```yaml
ghost-player-fix:
  enabled: true
  kick-message: "&cA player with the same name is already online. Please try again."

bedrock-auto-login:
  enabled: true
  only-registered: true

fpp-bot-auto-login:
  enabled: true
  cleanup-on-startup: true

placeholder-api:
  enabled: true
```

Use `/lapisauthmefix reload` (permission `lapisauthmefix.reload`, default op) to reload the config.

### How the FPP module works

1. A bot joins and AuthMe schedules a "login/registration timeout" kick.
2. This plugin detects the bot (`FppApi#isBot`), then:
   - if the name is **new**: registers a temp account with a random password (auto-login) and saves the name
     to `plugins/Lapis-AuthmeReloadedFix/bot-accounts.yml`;
   - if the name is **already registered by us**: just `forceLogin`;
   - if the name is **registered by someone else (real player)**: it is **skipped** - we never log into or
     hijack real player accounts.
3. When the bot is removed, `PlayerQuitEvent` unregisters the temp account.
4. On crash/shutdown, any leftover temp accounts are removed automatically during the next startup
   (`cleanup-on-startup`, default `true`). Only names recorded by this plugin can ever be removed.

---

## Support

Found a bug or want a feature? Open an issue on the project's GitHub repository.

---

## Contributing

PRs welcome. Keep the plugin dependency-free (reflection bridges only, no AuthMe/Floodgate/FPP jars at compile
time) so it stays compatible across AuthMe 5.x/6.x.

## License

This plugin is released under the [MIT License](LICENSE). AuthMeReloaded itself remains GPL-3.0.