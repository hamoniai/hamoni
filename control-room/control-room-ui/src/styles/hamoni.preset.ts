import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/* Hamoni preset — PrimeNG styled mode (ADR-0038, §38.1).
 *
 * Maps the *Design System for Hamoni* onto PrimeNG's semantic tokens. Every
 * value is a CSS custom property from tokens.css (or a color-mix of one); this
 * file never writes a colour of its own, so the preset, the Tailwind theme and
 * the screens all read one source.
 *
 * Colour model (§38.5): purple (--accent) is *where you are* → `primary`;
 * teal (--info) is *where you go next* → the `info` severity. Focus is the
 * accent, uniformly. States are neutral; only selection takes the accent.
 *
 * Wiring (app.config.ts):
 *
 *   import { providePrimeNG } from 'primeng/config';
 *   import { HamoniPreset } from './styles/hamoni.preset';
 *
 *   providePrimeNG({
 *     theme: {
 *       preset: HamoniPreset,
 *       options: {
 *         darkModeSelector: '.app-dark',
 *         cssLayer: { name: 'primeng', order: 'theme, base, primeng, components, utilities' },
 *       },
 *     },
 *   });
 *
 * Light mode is provisional: the schemes below carry the same var() references
 * for light and dark — the theme itself is switched by tokens.css (.app-dark),
 * so a single definition serves both until light mode is designed.
 *
 * Imports come from '@primeuix/themes'. On older releases (PrimeNG v18–v20) the
 * same helpers ship as '@primeng/themes'.
 */

/* ── shared severity shapes ─────────────────────────────────────────────── */

const filledNeutral = {
  background: 'var(--surface)',
  hoverBackground: 'var(--fg-soft)',
  activeBackground: 'var(--fg-faint)',
  borderColor: 'var(--border)',
  hoverBorderColor: 'var(--muted)',
  activeBorderColor: 'var(--muted)',
  color: 'var(--fg)',
  hoverColor: 'var(--fg)',
  activeColor: 'var(--fg)',
  focusRing: { color: 'var(--accent)', shadow: 'none' },
};

const filledStrong = {
  background: 'var(--fg)',
  hoverBackground: 'var(--fg-hover)',
  activeBackground: 'color-mix(in oklch, var(--fg) 78%, var(--bg))',
  borderColor: 'var(--fg)',
  hoverBorderColor: 'var(--fg-hover)',
  activeBorderColor: 'color-mix(in oklch, var(--fg) 78%, var(--bg))',
  color: 'var(--bg)',
  hoverColor: 'var(--bg)',
  activeColor: 'var(--bg)',
  focusRing: { color: 'var(--fg)', shadow: 'none' },
};

const outlinedNeutral = {
  hoverBackground: 'var(--fg-soft)',
  activeBackground: 'var(--fg-faint)',
  borderColor: 'var(--border)',
  color: 'var(--fg)',
};

const textNeutral = {
  hoverBackground: 'var(--fg-soft)',
  activeBackground: 'var(--fg-faint)',
  color: 'var(--muted)',
};

/* ── the colour scheme (light and dark share it; tokens.css switches) ────── */

const scheme = {
  primary: {
    color: 'var(--accent)',
    contrast: { color: 'var(--bg)' },
    hover: { color: 'var(--accent-hover)' },
    active: { color: 'var(--accent-active)' },
  },
  highlight: {
    background: 'var(--accent-soft)',
    focusBackground: 'color-mix(in oklch, var(--accent) 22%, transparent)',
    color: 'var(--accent)',
    focusColor: 'var(--accent)',
  },
  mask: { background: 'var(--scrim)', color: 'var(--fg)' },
  surface: {
    0: 'var(--bg)',
    50: 'var(--inset)',
    100: 'var(--surface)',
    200: 'var(--raise)',
    300: 'color-mix(in oklch, var(--surface) 68%, var(--fg))',
    400: 'color-mix(in oklch, var(--surface) 52%, var(--fg))',
    500: 'var(--dim)',
    600: 'color-mix(in oklch, var(--dim) 70%, var(--fg))',
    700: 'var(--muted)',
    800: 'color-mix(in oklch, var(--muted) 80%, var(--fg))',
    900: 'var(--fg)',
    950: 'var(--fg)',
  },
  content: {
    background: 'var(--surface)',
    hoverBackground: 'var(--fg-faint)',
    borderColor: 'var(--border)',
    color: 'var(--fg)',
    hoverColor: 'var(--fg)',
  },
  text: {
    color: 'var(--fg)',
    hoverColor: 'var(--fg)',
    mutedColor: 'var(--muted)',
    hoverMutedColor: 'var(--fg)',
  },
  formField: {
    background: 'var(--bg)',
    disabledBackground: 'var(--inset)',
    filledBackground: 'var(--inset)',
    filledFocusBackground: 'var(--inset)',
    borderColor: 'var(--border)',
    hoverBorderColor: 'var(--muted)',
    focusBorderColor: 'var(--accent)',
    invalidBorderColor: 'var(--fg)',
    color: 'var(--fg)',
    disabledColor: 'var(--dim)',
    placeholderColor: 'var(--muted)',
    invalidPlaceholderColor: 'var(--muted)',
    floatLabelColor: 'var(--muted)',
    floatLabelFocusColor: 'var(--accent)',
    floatLabelInvalidColor: 'var(--fg)',
    iconColor: 'var(--muted)',
    shadow: 'none',
  },
  overlay: {
    select: {
      background: 'var(--surface)',
      borderColor: 'var(--border)',
      color: 'var(--fg)',
      option: {
        color: 'var(--muted)',
        focusColor: 'var(--fg)',
        selectedColor: 'var(--accent)',
        focusBackground: 'var(--fg-faint)',
        selectedBackground: 'var(--accent-soft)',
        selectedFocusBackground: 'color-mix(in oklch, var(--accent) 22%, transparent)',
      },
    },
    popover: { background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--fg)' },
    modal: { background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--fg)' },
  },
  list: {
    option: {
      color: 'var(--muted)',
      focusColor: 'var(--fg)',
      selectedColor: 'var(--accent)',
      focusBackground: 'var(--fg-faint)',
      selectedBackground: 'var(--accent-soft)',
      selectedFocusBackground: 'color-mix(in oklch, var(--accent) 22%, transparent)',
    },
    optionGroup: { color: 'var(--muted)' },
  },
  navigation: {
    item: {
      color: 'var(--muted)',
      focusColor: 'var(--fg)',
      activeColor: 'var(--accent)',
      focusBackground: 'var(--fg-soft)',
      activeBackground: 'transparent',
    },
    submenuLabel: { color: 'var(--muted)' },
  },
};

/* ── button severities (§38.2: primary · secondary · ghost · danger · next) ─ */

const buttonColorScheme = {
  root: {
    primary: {
      background: 'var(--accent)',
      hoverBackground: 'var(--accent-hover)',
      activeBackground: 'var(--accent-active)',
      borderColor: 'var(--accent)',
      hoverBorderColor: 'var(--accent-hover)',
      activeBorderColor: 'var(--accent-active)',
      color: 'var(--bg)',
      hoverColor: 'var(--bg)',
      activeColor: 'var(--bg)',
      focusRing: { color: 'var(--accent)', shadow: 'none' },
    },
    secondary: filledNeutral,
    success: filledNeutral,
    info: {
      background: 'var(--info)',
      hoverBackground: 'color-mix(in oklch, var(--info) 88%, var(--fg))',
      activeBackground: 'color-mix(in oklch, var(--info) 82%, var(--bg))',
      borderColor: 'var(--info)',
      hoverBorderColor: 'color-mix(in oklch, var(--info) 88%, var(--fg))',
      activeBorderColor: 'color-mix(in oklch, var(--info) 82%, var(--bg))',
      color: 'var(--bg)',
      hoverColor: 'var(--bg)',
      activeColor: 'var(--bg)',
      focusRing: { color: 'var(--info)', shadow: 'none' },
    },
    warn: filledNeutral,
    help: filledNeutral,
    danger: filledStrong,
    contrast: filledStrong,
  },
  outlined: {
    primary: {
      hoverBackground: 'var(--accent-soft)',
      activeBackground: 'color-mix(in oklch, var(--accent) 22%, transparent)',
      borderColor: 'var(--accent-line)',
      color: 'var(--accent)',
    },
    secondary: outlinedNeutral,
    success: outlinedNeutral,
    /* the forward action — "next" — teal border, neutral text (§38.5) */
    info: {
      hoverBackground: 'var(--info-soft)',
      activeBackground: 'color-mix(in oklch, var(--info) 22%, transparent)',
      borderColor: 'var(--info-line)',
      color: 'var(--fg)',
    },
    warn: outlinedNeutral,
    help: outlinedNeutral,
    danger: outlinedNeutral,
    contrast: outlinedNeutral,
    plain: outlinedNeutral,
  },
  text: {
    primary: {
      hoverBackground: 'var(--accent-soft)',
      activeBackground: 'color-mix(in oklch, var(--accent) 22%, transparent)',
      color: 'var(--accent)',
    },
    secondary: textNeutral,
    success: textNeutral,
    info: {
      hoverBackground: 'var(--info-soft)',
      activeBackground: 'color-mix(in oklch, var(--info) 22%, transparent)',
      color: 'var(--info)',
    },
    warn: textNeutral,
    help: textNeutral,
    danger: { hoverBackground: 'var(--fg-soft)', activeBackground: 'var(--fg-faint)', color: 'var(--fg)' },
    contrast: { hoverBackground: 'var(--fg-soft)', activeBackground: 'var(--fg-faint)', color: 'var(--fg)' },
    plain: textNeutral,
  },
  link: {
    color: 'var(--accent)',
    hoverColor: 'var(--accent-hover)',
    activeColor: 'var(--accent-active)',
  },
};

/* ── segmented scope control (p-selectbutton → togglebutton tokens) ──────── */

const togglebuttonColorScheme = {
  root: {
    background: 'var(--inset)',
    checkedBackground: 'var(--bg)',
    hoverBackground: 'var(--fg-soft)',
    borderColor: 'var(--border)',
    color: 'var(--muted)',
    hoverColor: 'var(--fg)',
    checkedColor: 'var(--fg)',
    checkedBorderColor: 'var(--border)',
  },
  content: { checkedBackground: 'var(--bg)' },
  icon: { color: 'var(--muted)', hoverColor: 'var(--fg)', checkedColor: 'var(--fg)' },
};

/* ── switch (p-toggleswitch) — on turns the track dark (§38.8) ─────────────
   The track's border tokens live on the component `root` (shared by both
   schemes); the track and handle fills are per scheme. */

const toggleswitchColorScheme = {
  root: {
    background: 'var(--inset)',
    disabledBackground: 'var(--inset)',
    hoverBackground: 'var(--fg-soft)',
    checkedBackground: 'var(--bg)',
    checkedHoverBackground: 'var(--bg)',
  },
  handle: {
    background: 'var(--muted)',
    disabledBackground: 'var(--dim)',
    hoverBackground: 'var(--fg)',
    checkedBackground: 'var(--fg)',
    checkedHoverBackground: 'var(--fg)',
    color: 'var(--bg)',
    checkedColor: 'var(--bg)',
  },
};

/* ── tag / badge — info is the AI voice; the rest are neutral (§38.5) ────── */

const tagColorScheme = {
  primary: { background: 'var(--accent-soft)', color: 'var(--accent)' },
  secondary: { background: 'transparent', color: 'var(--muted)' },
  success: { background: 'transparent', color: 'var(--muted)' },
  info: { background: 'var(--info-soft)', color: 'var(--info)' },
  warn: { background: 'transparent', color: 'var(--muted)' },
  danger: { background: 'transparent', color: 'var(--muted)' },
  contrast: { background: 'var(--fg-soft)', color: 'var(--fg)' },
};

/* ── messages / toasts — only info is teal; states are neutral (§38.5) ───── */

const messageColorScheme = {
  info: { background: 'var(--info-soft)', borderColor: 'var(--info-line)', color: 'var(--fg)' },
  success: { background: 'var(--bg)', borderColor: 'var(--border)', color: 'var(--fg)' },
  warn: { background: 'var(--bg)', borderColor: 'var(--border)', color: 'var(--fg)' },
  error: { background: 'var(--bg)', borderColor: 'var(--border)', color: 'var(--fg)' },
  secondary: { background: 'var(--bg)', borderColor: 'var(--border)', color: 'var(--fg)' },
  contrast: { background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--fg)' },
};

const toastColorScheme = {
  info: { background: 'var(--surface)', borderColor: 'var(--info-line)', color: 'var(--fg)', detailColor: 'var(--muted)', shadow: 'none' },
  success: { background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--fg)', detailColor: 'var(--muted)', shadow: 'none' },
  warn: { background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--fg)', detailColor: 'var(--muted)', shadow: 'none' },
  error: { background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--fg)', detailColor: 'var(--muted)', shadow: 'none' },
  secondary: { background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--fg)', detailColor: 'var(--muted)', shadow: 'none' },
  contrast: { background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--fg)', detailColor: 'var(--muted)', shadow: 'none' },
};

export const HamoniPreset = definePreset(Aura, {
  semantic: {
    /* purple scale — the brand accent, mixes of --accent with --bg/--fg */
    primary: {
      50: 'color-mix(in oklch, var(--accent) 12%, var(--bg))',
      100: 'color-mix(in oklch, var(--accent) 20%, var(--bg))',
      200: 'color-mix(in oklch, var(--accent) 32%, var(--bg))',
      300: 'color-mix(in oklch, var(--accent) 46%, var(--bg))',
      400: 'color-mix(in oklch, var(--accent) 62%, var(--bg))',
      500: 'var(--accent)',
      600: 'color-mix(in oklch, var(--accent) 88%, var(--fg))',
      700: 'color-mix(in oklch, var(--accent) 76%, var(--fg))',
      800: 'color-mix(in oklch, var(--accent) 64%, var(--fg))',
      900: 'color-mix(in oklch, var(--accent) 52%, var(--fg))',
      950: 'color-mix(in oklch, var(--accent) 40%, var(--fg))',
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: 'var(--accent)',
      offset: '2px',
      shadow: 'none',
    },
    disabledOpacity: '0.42',
    iconSize: '14px',
    anchorGutter: '2px',
    formField: {
      paddingX: '12px',
      paddingY: '6px',
      borderRadius: 'var(--radius)',
      transitionDuration: 'var(--fast)',
      focusRing: {
        width: '3px',
        style: 'solid',
        color: 'var(--accent-soft)',
        offset: '0px',
        shadow: 'none',
      },
      sm: { fontSize: 'var(--fs-cap)', paddingX: '8px', paddingY: '4px' },
      lg: { fontSize: 'var(--fs-body)', paddingX: '14px', paddingY: '8px' },
    },
    colorScheme: { light: scheme, dark: scheme },
  },

  components: {
    button: {
      root: {
        fontSize: 'var(--fs-sm)',
        label: { fontWeight: '600' },
      },
      colorScheme: { light: buttonColorScheme, dark: buttonColorScheme },
    },

    /* form controls use the set's control text — 13px (--fs-sm), not the app's
       14px body */
    inputtext: { root: { fontSize: 'var(--fs-sm)' } },
    textarea: { root: { fontSize: 'var(--fs-sm)' } },
    select: { root: { fontSize: 'var(--fs-sm)' } },

    /* the number spinner is an icon affordance on the field, not a second
       control: transparent, tight, and never taller than the field */
    inputnumber: {
      button: {
        width: '1.5rem',
        verticalPadding: '0',
        borderRadius: 'var(--radius-sm)',
        background: 'transparent',
        hoverBackground: 'var(--fg-soft)',
        activeBackground: 'var(--fg-faint)',
        borderColor: 'transparent',
        hoverBorderColor: 'transparent',
        activeBorderColor: 'transparent',
        color: 'var(--muted)',
        hoverColor: 'var(--fg)',
        activeColor: 'var(--fg)',
      },
    },

    /* the date field's icon trigger is an icon on the field, not a raised
       control — transparent, muted, `--fg` on hover */
    datepicker: {
      dropdown: {
        width: '2rem',
        background: 'transparent',
        hoverBackground: 'var(--fg-soft)',
        activeBackground: 'var(--fg-faint)',
        borderColor: 'transparent',
        hoverBorderColor: 'transparent',
        activeBorderColor: 'transparent',
        color: 'var(--muted)',
        hoverColor: 'var(--fg)',
        activeColor: 'var(--fg)',
      },
      inputIcon: { color: 'var(--muted)' },
    },

    togglebutton: { colorScheme: { light: togglebuttonColorScheme, dark: togglebuttonColorScheme } },

    /* checkbox / radio — the quiet inset box, the same mark the chip wraps
       (§38.8): transparent off; --bg with a --muted border on; a small --fg
       tick / dot. Never a light fill. */
    checkbox: {
      root: {
        width: '14px',
        height: '14px',
        borderRadius: 'var(--radius-xs)',
        background: 'transparent',
        checkedBackground: 'var(--bg)',
        checkedHoverBackground: 'var(--bg)',
        borderColor: 'var(--border)',
        hoverBorderColor: 'var(--muted)',
        focusBorderColor: 'var(--accent)',
        checkedBorderColor: 'var(--muted)',
        checkedHoverBorderColor: 'var(--muted)',
        checkedFocusBorderColor: 'var(--muted)',
      },
      icon: { size: '10px', checkedColor: 'var(--fg)', checkedHoverColor: 'var(--fg)' },
    },

    radiobutton: {
      root: {
        width: '14px',
        height: '14px',
        background: 'transparent',
        checkedBackground: 'var(--bg)',
        checkedHoverBackground: 'var(--bg)',
        borderColor: 'var(--border)',
        hoverBorderColor: 'var(--muted)',
        focusBorderColor: 'var(--accent)',
        checkedBorderColor: 'var(--muted)',
        checkedHoverBorderColor: 'var(--muted)',
        checkedFocusBorderColor: 'var(--muted)',
      },
      icon: { size: '8px', checkedColor: 'var(--fg)', checkedHoverColor: 'var(--fg)' },
    },

    toggleswitch: {
      root: {
        width: '34px',
        height: '18px',
        borderRadius: '999px',
        borderColor: 'var(--border)',
        hoverBorderColor: 'var(--muted)',
        checkedBorderColor: 'var(--muted)',
        checkedHoverBorderColor: 'var(--muted)',
      },
      handle: { size: '12px' },
      colorScheme: { light: toggleswitchColorScheme, dark: toggleswitchColorScheme },
    },

    tag: {
      root: {
        fontSize: 'var(--fs-cap)',
        fontWeight: '600',
        padding: '3px 9px',
        gap: '5px',
        borderRadius: 'var(--radius-xs)',
      },
      icon: { size: '11px' },
      colorScheme: { light: tagColorScheme, dark: tagColorScheme },
    },

    message: {
      root: { borderRadius: 'var(--radius)', borderWidth: '1px' },
      colorScheme: { light: messageColorScheme, dark: messageColorScheme },
    },

    toast: { colorScheme: { light: toastColorScheme, dark: toastColorScheme } },

    dialog: {
      root: {
        background: 'var(--surface)',
        borderColor: 'var(--border)',
        color: 'var(--fg)',
        borderRadius: 'var(--radius-lg)',
      },
    },

    datatable: {
      headerCell: {
        background: 'transparent',
        color: 'var(--muted)',
        borderColor: 'var(--border)',
        padding: '10px 14px',
      },
      bodyCell: { borderColor: 'var(--border)', padding: '13px 14px' },
      rowToggleButton: { color: 'var(--muted)', hoverColor: 'var(--fg)' },
      sortIcon: { color: 'var(--muted)', hoverColor: 'var(--fg)' },
    },
  },
});
