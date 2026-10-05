# Social preview image

`og-image.html` is the source for `public/og-image.png`, the link-preview image
referenced by the Open Graph and Twitter tags in `index.html`. After editing it,
re-render at 1200x630 (opaque PNG, keep it under 300 KB for WhatsApp):

```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new \
  --hide-scrollbars --force-device-scale-factor=1 --window-size=1200,630 \
  --screenshot="$PWD/public/og-image.png" "file://$PWD/social/og-image.html"
```

Then bump the `?v=` query on the image URLs in `index.html` so WhatsApp and X
fetch the new file instead of their cached copy.
