package flat.math.shapes;

import flat.math.Affine;
import flat.math.Mathf;
import flat.math.Vector2;
import flat.math.util.FlatteningPathIterator;
import flat.math.util.Platform;

import java.io.Serializable;
import java.util.NoSuchElementException;

/**
 * Represents a rectangle with rounded corners, defined by an arc width and height.
 */
public final class RoundRectangle implements Shape, Serializable {

    private static final long serialVersionUID = 5850741513376725608L;

    /** The x-coordinate of the framing rectangle. */
    public float x;

    /** The y-coordinate of the framing rectangle. */
    public float y;

    /** The width of the framing rectangle. */
    public float width;

    /** The height of the framing rectangle. */
    public float height;

    /** The top/left arc that defines the rounded corners. */
    public float arcTop;

    /** The right/top arc that defines the rounded corners. */
    public float arcRight;

    /** The bottom/right arc that defines the rounded corners. */
    public float arcBottom;

    /** The left/bottom arc that defines the rounded corners. */
    public float arcLeft;

    /**
     * Creates a rounded rectangle with frame (0x0+0+0) and corners of size (0x0x0x0).
     */
    public RoundRectangle () {
    }

    /**
     * Creates a rounded rectangle with the specified frame and corner dimensions.
     */
    public RoundRectangle (float x, float y, float width, float height, float arc) {
        set(x, y, width, height, arc, arc, arc, arc);
    }

    /**
     * Creates a rounded rectangle with the specified frame and corner dimensions.
     */
    public RoundRectangle (float x, float y, float width, float height, float arcTop, float arcRight, float arcBottom, float arcLeft) {
        set(x, y, width, height, arcTop, arcRight, arcBottom, arcLeft);
    }

    /**
     * Creates a rounded rectangle with the specified rounded rectangle.
     */
    public RoundRectangle (RoundRectangle roundRect) {
        set(roundRect);
    }

    @Override
    public RoundRectangle clone () {
        return new RoundRectangle(this);
    }

    /**
     * Sets the frame and corner dimensions of this rectangle to the specified values.
     */
    public void set(float x, float y, float width, float height, float arcTop, float arcRight, float arcBottom, float arcLeft) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.arcTop = arcTop;
        this.arcRight = arcRight;
        this.arcBottom = arcBottom;
        this.arcLeft = arcLeft;
    }

    /**
     * Sets the frame and corner dimensions of this rectangle to be equal to those of the supplied
     * rectangle.
     */
    public void set(RoundRectangle rr) {
        set(rr.x, rr.y, rr.width, rr.height, rr.arcTop, rr.arcRight, rr.arcBottom, rr.arcLeft);
    }

    /**
     * Sets the frame dimensions of this rectangle
     */
    public void setSize(float x, float y, float width, float height) {
        set(x, y, width, height, arcTop, arcRight, arcBottom, arcLeft);
    }

    @Override
    public boolean isOptimized() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    @Override
    public Rectangle bounds() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public boolean contains (float px, float py) {
        if (isEmpty()) return false;

        float rx1 = x, ry1 = y;
        float rx2 = rx1 + width, ry2 = ry1 + height;
        if (px < rx1 || px >= rx2 || py < ry1 || py >= ry2) {
            return false;
        }

        float arc;
        if (px < (rx1 + rx2) / 2f) {
            arc = py < (ry1 + ry2) / 2f ? arcTop : arcLeft;
        } else {
            arc = py < (ry1 + ry2) / 2f ? arcRight : arcBottom;
        }
        float aw = Math.max(0, Math.min(width / 2f, arc));

        float cx, cy;
        if (px < rx1 + aw) {
            cx = rx1 + aw;
        } else if (px > rx2 - aw) {
            cx = rx2 - aw;
        } else {
            return true;
        }

        float ah = Math.max(0, Math.min(height / 2f, arc));
        if (py < ry1 + ah) {
            cy = ry1 + ah;
        } else if (py > ry2 - ah) {
            cy = ry2 - ah;
        } else {
            return true;
        }

        px = (px - cx) / aw;
        py = (py - cy) / ah;
        return px * px + py * py <= 1f;
    }

    @Override
    public boolean contains (Vector2 point) {
        return contains(point.x, point.y);
    }

    @Override
    public boolean contains (float rx, float ry, float rw, float rh) {
        if (isEmpty() || rw <= 0f || rh <= 0f) return false;
        float rx1 = rx, ry1 = ry, rx2 = rx + rw, ry2 = ry + rh;
        return contains(rx1, ry1) && contains(rx2, ry1) && contains(rx2, ry2) && contains(rx1, ry2);
    }

    @Override
    public boolean contains (Rectangle rectangle) {
        return contains(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    @Override
    public boolean intersects (float rx, float ry, float rw, float rh) {
        if (isEmpty() || rw <= 0f || rh <= 0f) return false;

        float x1 = x, y1 = y, x2 = x1 + width, y2 = y1 + height;
        float rx1 = rx, ry1 = ry, rx2 = rx + rw, ry2 = ry + rh;
        if (rx2 < x1 || x2 < rx1 || ry2 < y1 || y2 < ry1) {
            return false;
        }

        float cx = (x1 + x2) / 2f, cy = (y1 + y2) / 2f;
        float nx = cx < rx1 ? rx1 : (cx > rx2 ? rx2 : cx);
        float ny = cy < ry1 ? ry1 : (cy > ry2 ? ry2 : cy);
        return contains(nx, ny);
    }

    @Override
    public boolean intersects (Rectangle rectangle) {
        return intersects(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    @Override
    public PathIterator pathIterator (Affine at) {
        return new Iterator(this, at);
    }

    @Override
    public PathIterator pathIterator(Affine at, float flatness) {
        return new FlatteningPathIterator(pathIterator(at), flatness);
    }

    @Override
    public boolean equals (Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj.getClass() == getClass()) {
            RoundRectangle r = (RoundRectangle)obj;
            return r.x == x && r.y == y && r.width == width && r.height == height;
        }
        return false;
    }

    @Override
    public int hashCode () {
        return Platform.hashCode(x) ^ Platform.hashCode(y) ^ Platform.hashCode(width) ^ Platform.hashCode(height);
    }

    @Override
    public String toString () {
        return "RoundRectangle[x: " + x + ", y:" + y + ", width:" + width + ", height:" + height + "]";
    }

    /** Provides an iterator over an {@link RoundRectangle}. */
    protected static class Iterator implements PathIterator {
        private final float x, y, width, height, aTopW, aTopH, aRightW, aRightH, aBottomW, aBottomH, aLeftW, aLeftH;
        private final Affine t;
        private int index;

        Iterator (RoundRectangle rr, Affine at) {
            this.x = rr.x;
            this.y = rr.y;
            this.width = rr.width;
            this.height = rr.height;

            aRightW = Math.min(width / 2f, rr.arcRight) * 2;
            aRightH = Math.min(height / 2f, rr.arcRight) * 2;
            aBottomW = Math.min(width / 2f, rr.arcBottom) * 2;
            aBottomH = Math.min(height / 2f, rr.arcBottom) * 2;
            aLeftW = Math.min(width / 2f, rr.arcLeft) * 2;
            aLeftH = Math.min(height / 2f, rr.arcLeft) * 2;
            aTopW = Math.min(width / 2f, rr.arcTop) * 2;
            aTopH = Math.min(height / 2f, rr.arcTop) * 2;

            this.t = at;
            if (width < 0f || height < 0f) {
                index = POINTS.length;
            }
        }

        @Override public int windingRule () {
            return WIND_NON_ZERO;
        }

        @Override public boolean isDone () {
            return index > POINTS.length;
        }

        @Override public void next () {
            index++;
        }

        @Override public int currentSegment (float[] coords) {
            if (isDone()) {
                throw new NoSuchElementException("Iterator out of bounds");
            }
            if (index == POINTS.length) {
                return SEG_CLOSE;
            }
            int j = 0;
            float[] p = POINTS[index];
            float aw;
            float ah;
            if (index == 1 || index == 2) {
                aw = aRightW;
                ah = aRightH;
            } else if (index == 3 || index == 4) {
                aw = aBottomW;
                ah = aBottomH;
            } else if (index == 5 || index == 6) {
                aw = aLeftW;
                ah = aLeftH;
            } else {
                aw = aTopW;
                ah = aTopH;
            }
            for (int i = 0; i < p.length; i += 4) {
                coords[j++] = x + p[i + 0] * width + p[i + 1] * aw;
                coords[j++] = y + p[i + 2] * height + p[i + 3] * ah;
            }
            if (t != null) {
                t.transform(coords, 0, coords, 0, j / 2);
            }
            return TYPES[index];
        }
    }

    // the path for round corners is generated the same way as for Ellipse

    /** The segment types correspond to points array. */
    protected static final int[] TYPES = {
            PathIterator.SEG_MOVETO, PathIterator.SEG_CUBICTO, PathIterator.SEG_LINETO,
            PathIterator.SEG_CUBICTO, PathIterator.SEG_LINETO, PathIterator.SEG_CUBICTO,
            PathIterator.SEG_LINETO, PathIterator.SEG_CUBICTO, PathIterator.SEG_LINETO
    };

    /** The coefficient to calculate control points of Bezier curves. */
    protected static final float U = 0.5f - 2f / 3f * (Mathf.sqrt(2f) - 1f);

    /** The points coordinates calculation table. */
    protected static final float[][] POINTS = {
            {0f, 0.5f, 0f, 0f},// MOVETO
            {0f, U, 0f, 0f, 0f, 0f, 0f, U, 0f, 0f, 0f, 0.5f}, // CUBICTO
            {0f, 0f, 1f, -0.5f}, // LINETO
            {0f, 0f, 1f, -U, 0f, U, 1f, 0f, 0f, 0.5f, 1f, 0f}, // CUBICTO
            {1f, -0.5f, 1f, 0f}, // LINETO
            {1f, -U, 1f, 0f, 1f, 0f, 1f, -U, 1f, 0f, 1f, -0.5f}, // CUBICTO
            {1f, 0f, 0f, 0.5f}, // LINETO
            {1f, 0f, 0f, U, 1f, -U, 0f, 0f, 1f, -0.5f, 0f, 0f}, // CUBICTO
            {0f, 0.5f, 0f, 0f} // LINETO

    };
}
