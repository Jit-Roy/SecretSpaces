# Implementation Notes: Crop Frame Boundary Fix for Rotated Images

## Problem Statement
The crop frame (white bounding box) was extending beyond the actual rotated image area, capturing black background in the cropped result when rotation was applied. This occurred because:
1. The app lacked rotation/flip functionality
2. There was no boundary checking to constrain the crop frame within rotated image bounds

## Solution Overview
Added complete rotation and flip support with proper boundary checking using polygon containment algorithms.

## Key Components

### 1. State Management
Added two new state variables to `ImageCropScreen`:
- `rotationAngle: Float` - Current rotation angle (0°, 90°, 180°, 270°)
- `isFlippedHorizontally: Boolean` - Horizontal flip state

These are preserved using `rememberSaveable` to survive configuration changes.

### 2. UI Controls
Added three buttons for image transformation:
- **Rotate Left (↶)**: Rotates -90° (counterclockwise)
- **Flip (⇄)**: Toggles horizontal flip
- **Rotate Right (↷)**: Rotates +90° (clockwise)

When rotation or flip is triggered, the crop frame is reset to center (offsets set to 0,0) to ensure a valid initial position.

### 3. Boundary Checking Functions

#### `computeDisplayDims()`
Calculates the display dimensions of the image on canvas based on:
- Canvas dimensions
- Image dimensions
- Scale factor
- Maintains aspect ratio

#### `rectCorners()`
Helper function that returns the four corners of a rectangle as a list of `Offset` points.

#### `isPointInPolygon()`
Checks if a point is inside a convex polygon using the cross-product method.

**Key Fix:** Made robust to handle both clockwise and counterclockwise polygon winding orders. This was critical because horizontal flip reverses the winding order of the polygon.

**Algorithm:**
- Calculate cross product for each edge and the test point
- Count positive and negative cross products
- Point is inside if all cross products have the same sign
- Uses small tolerance (0.01f) for floating-point errors

#### `isRectInsideRotatedImage()`
Main boundary checking function that:
1. Calculates display dimensions of the image
2. Computes the four corners of the image in local space
3. Applies transformations (flip, then rotation) to get corners in canvas space
4. Checks if all four corners of the crop rectangle are inside the rotated image polygon
5. Uses a margin (4px) to avoid floating-point edge leaks

**Transformation Order (matches Canvas rendering):**
1. Apply horizontal flip (if enabled): `x = -x`
2. Apply rotation around center using rotation matrix:
   - `rotX = x * cos(θ) - y * sin(θ) + centerX`
   - `rotY = x * sin(θ) + y * cos(θ) + centerY`

### 4. Canvas Rendering
Updated `CropView` to render the rotated/flipped image using:
```kotlin
rotate(degrees = rotationAngle, pivot = Offset(centerX, centerY)) {
    scale(scaleX = if (isFlippedHorizontally) -1f else 1f, scaleY = 1f, pivot = Offset(centerX, centerY)) {
        drawImage(...)
    }
}
```

The transformation order in Canvas:
1. Inner scope: scale (flip) is applied first
2. Outer scope: rotation is applied second

This matches our boundary checking transformation order.

### 5. Crop Frame Constraint Logic

#### During Drag (Move)
- Calculate new position based on touch delta
- Check if new position would place crop frame outside rotated image
- Only apply movement if the new position is valid (all corners inside)
- For non-rotated images, uses simple bounding box constraints (optimization)

#### During Resize (Edge/Corner Drag)
- Calculate new size and position based on anchor point
- Validate the new crop frame position
- Only apply resize if the new frame is within bounds
- Handles all 8 drag points (4 corners + 4 edges)

### 6. Bitmap Cropping (Save Operation)
Updated `cropBitmapToLandscape()` to handle rotation/flip:

**Approach:**
1. Calculate crop coordinates in **original bitmap space** (before rotation)
2. Crop the **original bitmap** using these coordinates
3. Apply rotation/flip transformations to the **cropped portion**
4. Scale to target dimensions (1920x1080)

**Why this order?**
- Avoids coordinate system mismatch issues
- More efficient (transform smaller cropped image, not full image)
- Cleaner logic - crop coordinates stay in original space

**Transformation application:**
```kotlin
Matrix().apply {
    if (isFlippedHorizontally) postScale(-1f, 1f, centerX, centerY)
    if (rotationAngle != 0f) postRotate(rotationAngle, centerX, centerY)
}
```

## Edge Cases Handled

1. **Flipped images** - Polygon winding order reverses, handled by robust point-in-polygon check
2. **90°/270° rotations** - Dimensions swap, handled by transforming after cropping
3. **No rotation** - Optimization: uses simple bounding box instead of polygon containment
4. **Rotation + Flip** - Both transformations applied in correct order
5. **Small margins** - 4px margin prevents floating-point errors at edges

## Remaining Considerations

1. **Canvas dimensions in cropBitmapToLandscape**: Currently hardcoded to 1080x1920. Ideally should match actual device dimensions.
2. **Initial crop frame scale**: Set to 0.9 (90% of canvas width). Should fit most rotated images but may need adjustment for extreme aspect ratios.
3. **45° rotations**: Not implemented. Would require similar polygon containment approach but with arbitrary angles.

## Testing Requirements

See `TESTING_ROTATION_CROP.md` for detailed manual testing steps.

Key scenarios to verify:
- Crop frame cannot be dragged beyond rotated image bounds
- Crop frame cannot be resized beyond rotated image bounds
- Saved cropped images have no black background
- All rotation angles (0°, 90°, 180°, 270°) work correctly
- Flip works correctly (with and without rotation)
- Zoom functionality still works with rotation

## Files Modified

- `app/src/main/java/com/secretspaces32/android/ui/screens/ImageCropScreen.kt`
  - Added state variables for rotation and flip
  - Added UI controls (3 buttons)
  - Added 4 helper functions for boundary checking
  - Updated CropView rendering to support rotation/flip
  - Updated drag/resize logic to use boundary checking
  - Updated cropBitmapToLandscape to handle rotation/flip

## Dependencies
No new dependencies added. Uses existing Compose and Android graphics libraries.
