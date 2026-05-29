---
name: feedback-fowl-jungle-editor
description: Corrections and confirmed approaches from working on fowl-jungle-editor
metadata: 
  node_type: memory
  type: feedback
  originSessionId: ffaf33a1-d2f7-40ea-96ee-7c6f749dacdc
---

**"ingress" means entry FROM adjacent tiles, not toward them.**
**Why:** The field defines which directions a tile can be entered from. "Ingress from NORTH" = a player standing north of this tile can enter it. I initially described it as "toward existing tiles" which was backwards.
**How to apply:** All tooltip/label copy involving ingress should say "from," not "toward."

---

**Tooltip timeout cannot be set in Material3; use `isPersistent = true` only.**
**Why:** `rememberTooltipState` has no timeout parameter in the Material3 API available in this project.
**How to apply:** Don't look for a timeout API — `isPersistent = true` is the only control.

---

**Prefer filled `Button` everywhere for consistency; confirmed no OutlinedButton in menu bar or dialogs.**
**Why:** User confirmed "filled everywhere is fine, we want consistency, we have confirmation dialogs."
**How to apply:** Default to `Button` not `OutlinedButton` for all actions in this app.

---

**`Arrangement.spacedBy` accepts negative values to collapse touch-target padding on Checkboxes.**
**Why:** Material3 Checkbox has a 48dp touch target but a ~20dp visual. Negative spacing (e.g. `-12.dp`) pulls items visually closer without losing the touch target.
**How to apply:** Use `verticalArrangement = Arrangement.spacedBy(-12.dp)` on compass direction toggle columns, and `horizontalArrangement = Arrangement.spacedBy(-8.dp, Alignment.CenterHorizontally)` on the W/E row.

---

**`LaunchedEffect(Unit)` fires once on first composition and never again — do not rely on it for re-triggering.**
**Why:** `Unit` never changes, so the block is permanently done after startup. `initialize()` must call `restoreSample()` and `project.open()` directly rather than expecting the LaunchedEffect to re-run.
**How to apply:** Any post-action re-initialization must be done imperatively, not by expecting LaunchedEffect to re-fire.
