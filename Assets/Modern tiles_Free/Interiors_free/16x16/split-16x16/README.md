Each PNG is a non-transparent 16x16 crop from its source atlas. Filenames use zero-based column and row coordinates (`c##_r##.png`); `manifest.csv` maps them to source pixels. The restaurant border crops `c00` and `c01`, rows `r07` and `r09`, start 2 pixels below their nominal atlas rows; row `r08` samples at atlas y=128. Empty transparent cells are omitted, and the original atlases remain unchanged.

The restaurant also uses `c03_r08` above the lower-right border tile and `c03_r09` in place of the tile beside the corner. Four copies of `c03_r05` continue the bottom border immediately to the right of `c03_r09` (the kitchen computer tile).

The dining area uses `c06_r13` as a single-tile round table. Each left-side chair stacks `c06_r21` above `c06_r22`; the lower chair tile aligns beside the table to form an L. The `Interiors_free` atlas is used because its palette matches the restaurant border.
