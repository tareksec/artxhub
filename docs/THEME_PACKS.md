# CyberHome packs (theme packs and backups)

A pack is a normal `.zip` file. Apply it in **Settings → CyberHome → Theme packs & backup →
Apply a theme pack**. A backup made on the same screen is the same format with more fields.

## Layout of the zip

```
my-pack.zip
├── cyberhome-pack.json      required
├── cards/blue.png           any image file names you like (PNG, JPG or WebP)
├── cards/pink.png
├── cards/teal.png
└── wallpaper.jpg
```

## cyberhome-pack.json

```json
{
  "format": 1,
  "name": "Spring pack",
  "cards": {
    "blue": { "image": "cards/blue.png", "title": "Phone", "number": "05", "caption": "Call a friend" },
    "pink": { "character": "sakura", "title": "Special day", "caption": "Our day" },
    "teal": { "image": "cards/teal.png" }
  },
  "board": { "decoration": true, "mirrored": false, "hidden": ["weather"] },
  "wallpaper": { "image": "wallpaper.jpg" }
}
```

Every part is optional. Rules:

| Field | Meaning |
|---|---|
| `format` | Pack format version. CyberHome refuses packs with a higher number than it knows (now 1). |
| `cards.blue / pink / teal` | The three board cards. `image`: path inside the zip. `character`: a built-in one (`frost`, `sakura`, `sleepy`, `lilac`) instead of an image. `title`, `number` (max 4 characters), `caption`: card texts. |
| `board.decoration` | Thin lines, "+" marks and card corners on/off. |
| `board.mirrored` | Clock column on the left. |
| `board.hidden` | Parts to hide: `blue_card`, `clock`, `shortcuts`, `pink_card`, `battery`, `calendar`, `weather`, `music`, `apps`, `drink`. |
| `wallpaper` | `image`: path inside the zip, or `builtin`: `auto`, `snow`, `lounge`, `petal`, `cafe`, `sketch`, `sunset`, `ocean`, `crimson`. |

Image tips: portrait, about 3:4, at most ~1080 px on the long side (bigger images are scaled
down). A PNG/WebP with a transparent background stands on the card like the built-in
characters; a photo fills the card.

## Backups

"Back up CyberHome settings" writes `"backup": true` plus a `settings` object (our preference
files: board, cards, counter, water, weather city, wallpaper choice) and the user's own card images
and wallpaper. Restoring replaces the current CyberHome settings. The home screen layout (apps,
folders) is not included; Lawnchair's own backup in the launcher settings covers that.

## Safety limits on import

At most 64 files, 12 MB per file and 48 MB in total; file names with `..` are ignored; only
CyberHome's own preference files can be written. Unknown fields are ignored.

## Licensing

Only put art in a pack that you made or are allowed to share. Do not use characters, logos or
fonts from other works (see `ASSETS.md`).
