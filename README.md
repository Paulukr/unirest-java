# Unirest 3 — community fork

[![Verify](https://github.com/Paulukr/unirest-java/actions/workflows/maven.yml/badge.svg?branch=u3a5)](https://github.com/Paulukr/unirest-java/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

This is an independently maintained prerelease fork of [Kong Unirest](https://github.com/Kong/unirest-java), based on Unirest 3.14.5. It is not an official Kong release.

- `u3a4` retains the Apache HttpClient 4 transport and uses the `3.14.x` version line.
- `u3a5` uses Apache HttpClient 5 and the `3.15.x` version line. Code using Apache-specific clients, interceptors, or configuration types must be adapted and recompiled.

Both branches target Java 8. The workflow verifies Java 8, 11, 17, 21, and 25 and checks Java 25-built classes on Java 8. The badge shows the result for `u3a5`.

## Download a build

Open [Verify workflows](https://github.com/Paulukr/unirest-java/actions/workflows/maven.yml), select a successful run for the branch you want, and download **unirest-maven-repository** from its artifacts. GitHub requires sign-in to download workflow artifacts. They expire after 90 days or according to the repository retention policy.

The archive contains ordinary JARs, their Maven POMs, the parent POM, and the project license. Extract its `repository` directory into a local Maven repository or configure Maven to use it as a repository. Dependencies such as Apache HttpClient and Gson are not bundled in these JARs and must be resolved separately.

These prereleases currently use the upstream `com.konghq` Maven group and Unirest 3 package names. Do not place this fork and upstream Unirest 3 on the same classpath. They have not been published to Maven Central.

## Compatibility and maintenance

The optional Jackson mapper retains Jackson 2.14.x for its existing parse-error diagnostics. That Jackson branch is no longer community-maintained and has a [published deep-nesting issue](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-h46c-h94j-95f3). Apache HttpCore 4 and HttpAsyncClient 4 are end-of-life; the `u3a4` branch retains them for compatibility.

The upstream project's [Unirest 3 documentation](docs/index.md) and [upgrade guide](UPGRADE_GUIDE.md) remain available, but may not describe every fork-specific change.

## License

[MIT License](LICENSE). The Maven repository bundle also includes the license in each project JAR.
