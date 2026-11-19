# Changelog

All notable changes to this project will be documented in this file.

## 1.0.0 (2026-08-16)

Released to Maven Central from the Code repository; the pack has been maintained in the Library repository since 1.1.0.


- Initial release: `sedr-lens`, a book-branded lens styled after the cover and interior of _Software Engineering Done Right_. Metric cards use a whole-frame percentage fill, a riveted brass nameplate header, a boiler-plate tracker panel with corner rivets, a faint tiled cog-ring watermark, and a locomotive favicon.
- Bundled a companion `sedr-lens` Mustache template (`templateId = 'sedr-lens'`), replacing the plugin's default line chart with a stacked bar (one dataset per category, converging toward "done") beside a pie that snaps to whichever date the reader hovers on the bar. Both read the tracker's own per-date breakdown, so they can never disagree with each other.
- Each tracker gets its own forecast toggle - a switch (not a bare checkbox) in the tracker's upper-right corner, aligned with its title, off by default. Turning it on reveals the forecast box, which carries its own working-week choice (5-day/7-day) scoped to that one tracker.
- For `API_CONTRACT` trackers, recomputes the classic Shadow/Mirage/Doppelganger/Compliant partition from the plugin's own `declared`/`implemented`/`verified` counts at every date in the tracker's history (per `DASHBOARD-THEMING.adoc`'s documented formula), not just "now". `Stubbed` is deliberately kept out of that partition - it is not derivable from the other four, and the plugin's own documentation confirms it is not meant to be - so it gets its own synthesized "stub coverage" tracker section instead, scoped to declared endpoints only, with its own metric cards, chart, and forecast.
