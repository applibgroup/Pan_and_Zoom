package xyz.zpayh.hdimageview;

import ohos.agp.utils.Point;
import ohos.utils.Parcel;
import ohos.utils.Sequenceable;
import org.jetbrains.annotations.NotNull;

public class PointF implements Sequenceable {
    public float x;
    public float y;

    public PointF() {}

    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }


    public PointF(@NotNull Point p) {
        this.x = p.getPointX();
        this.y = p.getPointY();
    }

    /**
     * Set the point's x and y coordinates
     */
    public final void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Set the point's x and y coordinates to the coordinates of p
     */
    public final void set(@NotNull PointF p) {
        this.x = p.x;
        this.y = p.y;
    }

    public final void negate() {
        x = -x;
        y = -y;
    }

    public final void offset(float dx, float dy) {
        x += dx;
        y += dy;
    }

    /**
     * Returns true if the point's coordinates equal (x,y)
     */
    public final boolean equals(float x, float y) {
        return this.x == x && this.y == y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PointF pointF = (PointF) o;

        if (Float.compare(pointF.x, x) != 0) return false;
        if (Float.compare(pointF.y, y) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PointF(" + x + ", " + y + ")";
    }

    /**
     * Return the euclidian distance from (0,0) to the point
     */
    public final float length() {
        return length(x, y);
    }

    /**
     * Returns the euclidian distance from (0,0) to (x,y)
     */
    public static float length(float x, float y) {
        return (float) Math.hypot(x, y);
    }

    /**
     * Parcelable interface methods
     */
//    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Write this point to the specified parcel. To restore a point from
     * a parcel, use readFromParcel()
     * @param out The parcel to write the point's coordinates into
     */


    public static final @NotNull Sequenceable.Producer<PointF> CREATOR = new Sequenceable.Producer<PointF>() {
        /**
         * Return a new point from the data in the specified parcel.
         */
        @Override
        public PointF createFromParcel(Parcel in) {
            PointF r = new PointF();
            r.unmarshalling(in);
            return r;
        }

        /**
         * Return an array of rectangles of the specified size.
         */
//        @Override
        public PointF[] newArray(int size) {
            return new PointF[size];
        }
    };

    /**
     * Set the point's coordinates from the data stored in the specified
     * parcel. To write a point to a parcel, call writeToParcel().
     *
//     * @param out The parcel to read the point's coordinates from
     */
//    public void readFromParcel(@NotNull Parcel in) {
//        x = in.readFloat();
//        y = in.readFloat();
//    }

    @Override
    public boolean marshalling(Parcel out) {
        out.writeFloat(x);
        out.writeFloat(y);
        return true;
    }

    @Override
    public boolean unmarshalling(Parcel parcel) {
        this.x = parcel.readFloat();
        this.y = parcel.readFloat();
        return true;
    }
}