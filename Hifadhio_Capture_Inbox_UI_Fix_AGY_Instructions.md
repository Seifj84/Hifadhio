# Hifadhio — Capture Inbox UI Fix Instructions for AGY

## Purpose

Fix the **Capture Inbox** screen shown in the supplied screenshot.  
This task is not a redesign of the whole application. Preserve the current Hifadhio identity and navigation structure, but correct the visual, usability, spacing, menu, card, and state-management problems listed below.

The result must look intentional, polished, readable, and production-ready on Android.

---

# 1. Critical bug: overflow menu is unreadable

## Current problem

The three-dot item menu opens as a very dark rectangle, while the menu text is also extremely dark.

Visible options include:

- View Details
- Open in Browser
- Share Link
- Move to Collection
- Edit Details
- Delete

The contrast is so poor that the menu is effectively unusable.

The menu also appears oversized and visually detached from the card.

## Required fix

1. Use the application's actual light-theme surface color for the popup menu.
2. Menu text must use the normal high-contrast foreground color.
3. `Delete` must use the destructive/error color.
4. Add normal Material-style elevation/shadow.
5. Use a sensible corner radius consistent with the rest of Hifadhio.
6. Keep the menu width only as wide as necessary for its labels.
7. Anchor it correctly to the three-dot icon.
8. Keep the popup entirely inside the device viewport.
9. Do not allow the popup to cover more of the item than necessary.
10. Respect both light and dark themes if theme support exists.

### Expected appearance

A light popup on the current light screen, for example:

- white/surface background
- dark readable labels
- subtle shadow
- rounded corners
- red/destructive `Delete`
- proper item spacing

Do **not** solve this by hardcoding random colors that break dark mode. Use theme tokens.

---

# 2. Fix popup-menu interaction behavior

The menu should behave like a native Android context menu.

## Requirements

- Tapping outside closes it.
- Selecting an action closes it.
- Android Back closes it before leaving the screen.
- Only one menu can be open at a time.
- The menu must not shift the underlying card layout.
- It must not leave a permanent dark overlay after dismissal.
- The three-dot button must have an adequate touch target, at least about 48×48 dp.
- Add semantic/accessibility labels to the button and menu items.

---

# 3. Improve the saved-link card hierarchy

The card currently contains too many competing lines without a clear information hierarchy.

Current visible structure roughly contains:

- Facebook chip
- Inbox
- `0 min. ago`
- overflow menu
- `facebook.com/share...`
- raw URL
- Open Link
- another partially hidden action

## Required card structure

Use this hierarchy:

### Row 1 — source/status
- Platform chip: `Facebook`
- Collection/status: `Inbox`
- Relative save time
- Three-dot menu

### Row 2 — content title
Show the best available human-readable title.

If metadata has not been fetched yet, use a clean fallback such as:

`Facebook link`

Do not use a long raw URL as the primary title unless there is absolutely no better value.

### Row 3 — domain / URL preview
Show a secondary muted line such as:

`facebook.com`

or a safely truncated URL.

The full raw URL should not dominate the card.

### Row 4 — actions
Primary lightweight action:

`Open`

Optional secondary action only if genuinely useful.

Do not duplicate actions that already exist inside the overflow menu unless there is a clear usability reason.

---

# 4. Fix the relative time text

`0 min. ago` is awkward and robotic.

Use human-friendly relative time.

Examples:

- under ~1 minute → `Just now`
- 1 minute → `1 min ago`
- 5 minutes → `5 min ago`
- 1 hour → `1 hr ago`
- older dates → use an appropriate short date if needed

Keep this logic reusable across the app.

---

# 5. Improve URL and title truncation

The screenshot shows a long Facebook share URL spilling visually across the card.

## Requirements

- Never allow long URLs to overflow horizontally.
- Never make the card wider than its parent.
- Title should normally be max 2 lines.
- URL/domain preview should normally be max 1 line.
- Use ellipsis.
- Preserve the full URL internally.
- Allow the full URL to be seen in Details/Edit where appropriate.
- Ensure unusual URLs cannot break the layout.

Test with:
- very long Facebook share links
- TikTok links
- YouTube Shorts links
- normal websites
- URLs with query parameters
- URLs with no metadata

---

# 6. Clarify the Capture Inbox explanation

Current copy:

`1 item to triage`

and

`Tap collection or menu to organize`

The first label is useful, but the second instruction is vague.

## Change the helper copy

Use a clearer short instruction, for example:

`Organize saved links into collections.`

If moving by tapping the collection label is supported, the interface should visually indicate that the collection label is interactive.

Do not depend on instructional text to explain an otherwise hidden interaction.

---

# 7. Make Inbox status useful and understandable

A captured item is currently labeled `Inbox`.

That is acceptable only if Inbox has a clear product meaning:

> newly captured items that have not yet been organized.

Implement this consistently.

An item should leave Inbox when the user intentionally files it into a collection, unless the product specification says otherwise.

The count shown in the Inbox badge and `items to triage` must come from the same source of truth.

Do not maintain separate counters that can drift apart.

---

# 8. Fix the oversized `Save Link` button on this screen

The floating `Save Link` button is visually too dominant on the Inbox screen.

The main task on this screen is reviewing/organizing captured links, not manually adding another link.

## Required change

Keep manual Save Link available, but reduce its visual dominance.

Preferred options:

- use a standard Material extended FAB with correct dimensions; or
- collapse it to a normal `+` FAB where appropriate.

Do not place an oversized custom rectangle floating over the interface.

Ensure it:
- respects bottom navigation
- respects safe-area/navigation insets
- never covers content
- uses consistent elevation
- has a standard touch target

---

# 9. Correct bottom navigation proportions

The bottom navigation occupies too much vertical space.

## Required fix

Use a standard Android/Material navigation-bar height and spacing.

Maintain these five destinations for now:

- Home
- Inbox
- Library
- Ask
- Profile

Requirements:

- selected state must be obvious but restrained
- icon and label must align consistently
- all icons should come from one coherent icon family
- inactive labels/icons must retain sufficient contrast
- badge positioning must not collide with the Inbox icon
- bottom system gesture area must remain unobstructed

The `1` badge should look attached to the Inbox icon, not like a floating decorative circle.

---

# 10. Improve overall vertical spacing

The current screen has excessive unused space below the single Inbox card.

Do not artificially stretch the card, but make the page composition feel deliberate.

## Requirements

- Keep consistent left/right margins.
- Use a consistent spacing scale.
- Keep the page title, triage status, and cards visually connected.
- The first card should not feel detached from the heading.
- Avoid arbitrary large gaps.
- Allow a scrolling list naturally when multiple items exist.

Suggested spacing system:

- 4 dp: micro spacing
- 8 dp: closely related elements
- 12–16 dp: internal component spacing
- 16–24 dp: section spacing
- 24–32 dp: major section spacing

Use the project's existing design tokens if available rather than introducing duplicate constants.

---

# 11. Refine the item card styling

Current card styling is acceptable as a starting point, but refine it.

## Requirements

- use the same corner-radius system as the rest of Hifadhio
- reduce unnecessary card height
- maintain subtle elevation/border
- avoid heavy shadows
- keep internal padding consistent
- align the three-dot menu vertically with the top metadata row
- make platform chips compact rather than oversized

The card should feel information-dense enough to browse many saved links quickly.

---

# 12. Facebook metadata fallback

The screenshot suggests that Hifadhio saved a Facebook share URL without obtaining a meaningful title.

Do not fabricate metadata.

Use this fallback priority:

1. fetched Open Graph/page title, when legitimately available
2. user-edited title
3. recognized platform + content type, if determinable
4. platform fallback such as `Facebook link`
5. domain fallback

Examples:

- `Facebook Reel`
- `Facebook post`
- `Facebook link`

Only claim `Reel`, `post`, etc. when the URL or fetched metadata reliably indicates that type.

---

# 13. Platform chip consistency

The `Facebook` chip looks reasonable but platform presentation must be systematic.

Use one reusable platform-chip component for:

- Facebook
- Instagram
- TikTok
- YouTube
- Web / Other

Do not invent a completely different style for each card.

Platform-specific color may be used subtly if already part of the design system, but readability and Hifadhio branding take priority.

---

# 14. Fix possible action duplication

Audit the card and menu actions.

The current UI appears to expose `Open Link` directly while also exposing `Open in Browser` in the overflow menu.

That may be redundant.

Use this rule:

### Direct card actions
Only actions frequently needed while browsing the Inbox.

For example:
- Open
- Move/Organize

### Overflow actions
Less frequent actions:
- View Details
- Share
- Edit Details
- Delete

Do not expose the same command twice under different names unless there is a documented UX reason.

---

# 15. Destructive delete behavior

`Delete` must not immediately destroy saved content through an accidental tap.

Implement one of these:

### Preferred
Delete → remove item → show Snackbar:

`Item deleted`   `UNDO`

or use a confirmation dialog if deletion has irreversible consequences.

Do not use both confirmation and Undo unless there is a specific reason.

---

# 16. Accessibility

Verify:

- sufficient text/background contrast
- minimum touch target sizes
- screen-reader labels
- overflow button semantic label such as `More options`
- platform chips are readable
- destructive actions are announced correctly
- dynamic font scaling does not break the layout
- long titles do not overlap actions
- 200% font scale remains usable where practical

---

# 17. Responsive-layout checks

Test this screen at minimum on:

- narrow Android phone
- normal Android phone
- large Android phone
- landscape orientation if supported

Test with:
- 0 Inbox items
- 1 Inbox item
- 2–5 items
- 20+ items
- very long title
- very long URL
- no metadata
- dark theme if supported

Nothing should overflow, clip, or become unreadable.

---

# 18. Empty Inbox state

When Inbox count reaches zero, do not leave the page looking broken.

Display a compact empty state.

Example:

**Inbox cleared**

`New links you share to Hifadhio will appear here before you organize them.`

Do not show `0 items to triage` as the primary experience if a better empty state can be shown.

---

# 19. Preserve the Hifadhio visual identity

Keep the existing core identity:

- dark navy header
- Hifadhio name
- teal/mint primary accent
- tagline where appropriate

But keep the design restrained.

Do not introduce additional unrelated accent colors.

Define/reuse semantic tokens such as:

- background
- surface
- surfaceVariant
- primary
- onPrimary
- textPrimary
- textSecondary
- outline
- error
- onError

Do not scatter hardcoded hex values across individual components.

---

# 20. Do not break functionality

Before changing components, inspect the existing implementation and identify:

- screen/component files
- reusable saved-item/card component
- popup/menu implementation
- navigation implementation
- theme/design tokens
- Inbox repository/state
- collection movement logic
- delete logic
- URL metadata model
- badge-count source

Refactor only where needed.

Do not rewrite the whole app merely to solve this screen.

---

# 21. Implementation sequence

AGY must perform the work in this order.

## Step 1 — Inspect

Report:

- files responsible for this screen
- files responsible for popup menu
- saved-item card component
- bottom navigation component
- theme/color definitions
- Inbox count source
- suspected reason for dark-on-dark popup text

Do not edit until the root cause is identified.

## Step 2 — Fix the popup first

Correct:

- background
- text color
- Delete color
- sizing
- positioning
- dismissal
- viewport safety

Then compile/test.

## Step 3 — Refactor the Inbox card

Correct:

- title hierarchy
- URL preview
- time formatting
- direct actions
- overflow actions
- truncation
- padding

Then compile/test.

## Step 4 — Fix layout

Correct:

- heading spacing
- helper text
- FAB
- bottom navigation proportions
- badges
- safe areas

Then compile/test.

## Step 5 — Implement empty state

Test both:

- Inbox with items
- Inbox with no items

## Step 6 — Responsive and accessibility verification

Test long URLs, long titles, multiple items and increased text size.

## Step 7 — Regression test

Verify:

- Home
- Inbox
- Library
- Ask
- Profile
- share-to-Hifadhio capture flow
- manual Save Link
- Open Link
- View Details
- Move to Collection
- Edit
- Share
- Delete/Undo
- Inbox badge count

---

# 22. Required final visual behavior

The finished Capture Inbox should approximately behave like this:

```text
Hifadhio
Save once. Find it when it matters.

Capture Inbox
[1 item to triage]
Organize saved links into collections.

┌───────────────────────────────────────┐
│ [Facebook]  Inbox        Just now  ⋮ │
│                                       │
│ Facebook link                         │
│ facebook.com                          │
│                                       │
│ Open                 Organize         │
└───────────────────────────────────────┘

                              [+ Save]

Home   Inbox¹   Library   Ask   Profile
```

The illustration above represents hierarchy only.  
Follow the project's proper Material/design system rather than reproducing ASCII dimensions literally.

---

# 23. Acceptance criteria

The task is complete only if all of the following pass:

- [ ] popup text is clearly readable
- [ ] popup stays inside screen bounds
- [ ] Delete is visually destructive
- [ ] popup opens/closes correctly
- [ ] no dark-on-dark text remains
- [ ] long URLs do not overflow
- [ ] primary content title is more useful than a raw URL
- [ ] `0 min. ago` is replaced with human-friendly relative time
- [ ] Inbox count and badge remain synchronized
- [ ] card action duplication is reduced
- [ ] manual Save Link remains available without dominating the page
- [ ] bottom navigation height and spacing are improved
- [ ] Inbox badge is correctly positioned
- [ ] empty Inbox has a proper empty state
- [ ] accessibility/touch targets are acceptable
- [ ] no existing navigation or capture functionality is broken
- [ ] project builds successfully
- [ ] APK compiles successfully if this project is currently configured for Android APK builds

---

# 24. AGY reporting requirements

At completion, report exactly:

1. Root cause(s) found.
2. Files changed.
3. UI changes made.
4. Functional changes made.
5. Any refactoring performed.
6. Tests/checks run.
7. Build result.
8. APK output path, if APK compilation is part of the current project workflow.
9. Remaining known issues, if any.
10. Before/after screenshots if the environment supports emulator/device screenshots.

Do not simply say "fixed".

Explain each material change and its verification result.

---

# Important constraint

Do **not** treat this only as a color bug.

The unreadable popup is the most obvious defect, but the task also includes the Inbox card hierarchy, URL handling, relative-time formatting, action duplication, FAB prominence, bottom navigation proportions, Inbox state consistency, and responsive behavior.

Fix the underlying components so the solution applies consistently to future saved items, not only the single Facebook item visible in the screenshot.
