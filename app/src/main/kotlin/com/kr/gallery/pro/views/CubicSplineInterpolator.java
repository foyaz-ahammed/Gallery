package com.kr.gallery.pro.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CubicSplineInterpolator {

    private final List<Float> xs;
    private final List<Float> ys;
    private final List<Float> ks;

    private CubicSplineInterpolator(List<Float> x, List<Float> y, List<Float> m) {
        xs = x;
        ys = y;
        ks = m;
    }

    /**
     * Creates a monotone cubic spline from a given set of control points.
     *
     * The spline is guaranteed to pass through each control point exactly. Moreover, assuming the control points are
     * monotonic (Y is non-decreasing or non-increasing) then the interpolated values will also be monotonic.
     *
     * This function uses the Fritsch-Carlson method for computing the spline parameters.
     * http://en.wikipedia.org/wiki/Monotone_cubic_interpolation
     *
     * @param xs
     *            The X component of the control points, strictly increasing.
     * @param ys
     *            The Y component of the control points
     * @return
     *
     * @throws IllegalArgumentException
     *             if the X or Y arrays are null, have different lengths or have fewer than 2 values.
     */
    public static CubicSplineInterpolator createMonotoneCubicSpline(List<Float> xs, List<Float> ys) {
//        if (xs == null || ys == null || xs.size() != ys.size() || xs.size() < 2) {
//            throw new IllegalArgumentException("There must be at least two control "
//                    + "points and the arrays must be of equal length.");
//        }

        int n = xs.size()-1;
        float[][] A = zerosMat(n+1, n+2);

        for(int i=1; i<n; i++)	// rows
        {
            A[i][i-1] = 1/(xs.get(i) - xs.get(i-1));

            A[i][i  ] = 2 * (1/(xs.get(i) - xs.get(i-1)) + 1/(xs.get(i+1) - xs.get(i))) ;

            A[i][i+1] = 1/(xs.get(i+1) - xs.get(i));

            A[i][n+1] = 3*( (ys.get(i)-ys.get(i-1))/((xs.get(i) - xs.get(i-1))*(xs.get(i) - xs.get(i-1)))  +  (ys.get(i+1)-ys.get(i))/ ((xs.get(i+1) - xs.get(i))*(xs.get(i+1) - xs.get(i))) );
        }

        A[0][0  ] = 2/(xs.get(1) - xs.get(0));
        A[0][1  ] = 1/(xs.get(1) - xs.get(0));
        A[0][n+1] = 3 * (ys.get(1) - ys.get(0)) / ((xs.get(1)-xs.get(0))*(xs.get(1)-xs.get(0)));

        A[n][n-1] = 1/(xs.get(n) - xs.get(n-1));
        A[n][n  ] = 2/(xs.get(n) - xs.get(n-1));
        A[n][n+1] = 3 * (ys.get(n) - ys.get(n-1)) / ((xs.get(n)-xs.get(n-1))*(xs.get(n)-xs.get(n-1)));


        List<Float> ks = new ArrayList<>(Collections.nCopies(xs.size(), 0f));
        solve(A, ks);

        return new CubicSplineInterpolator(xs, ys, ks);
    }

    private static List<Float> solve(float[][] A, List<Float> x) {
        int m = A.length;
        for(int k=0; k<m; k++)	// column
        {
            // pivot for column
            int i_max = 0;
            float vali = Float.MIN_VALUE;

            for(int i=k; i<m; i++) {
                if(Math.abs(A[i][k])>vali) {
                    i_max = i;
                    vali = Math.abs(A[i][k]);
                }
            }


            swapRows(A, k, i_max);

            //if(A[k][k] == 0) console.log("matrix is singular!");

            // for all rows below pivot
            for(int i=k+1; i<m; i++)
            {
                float cf = (A[i][k] / A[k][k]);
                for(int j=k; j<m+1; j++)  A[i][j] -= A[k][j] * cf;
            }
        }

        for(int i=m-1; i>=0; i--)	// rows = columns
        {
            float v = A[i][m] / A[i][i];
            x.set(i, v);
            for(int j=i-1; j>=0; j--)	// rows
            {
                A[j][m] -= A[j][i] * v;
                A[j][i] = 0;
            }
        }
        return x;
    }

    private static void swapRows(float[][] m, int k, int l) {
        float[] p = m[k];
        m[k] = m[l];
        m[l] = p;
    }

    private static float[][] zerosMat(int r, int c) {
        float[][] A = new float[r][c];

        for(int i=0; i<r; i++) {
            A[i] = new float[c];
            for(int j=0; j<c; j++) A[i][j] = 0;
        }
        return A;
    }

    /**
     * Interpolates the value of Y = f(X) for given X. Clamps X to the domain of the spline.
     *
     * @param x
     *            The X value.
     * @return The interpolated Y = f(X) value.
     */
    public float interpolate(float x) {
        
        int i = 1;
        while(xs.get(i)<x) i++;

        float t = (x - xs.get(i-1)) / (xs.get(i) - xs.get(i-1));

        float a =  ks.get(i-1)*(xs.get(i)-xs.get(i-1)) - (ys.get(i)-ys.get(i-1));
        float b = -ks.get(i)*(xs.get(i)-xs.get(i-1)) + (ys.get(i)-ys.get(i-1));

        float q = (1-t)*ys.get(i-1) + t*ys.get(i) + t*(1-t)*(a*(1-t)+b*t);

        return q;
    }

    // For debugging.
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        return str.toString();
    }

    public void release() {
        xs.clear();
        ys.clear();
        ks.clear();
    }

}
