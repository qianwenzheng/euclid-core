package us.ihmc.euclid.tuple4D.interfaces;

import us.ihmc.euclid.axisAngle.interfaces.AxisAngleBasics;
import us.ihmc.euclid.exceptions.NotAMatrix2DException;
import us.ihmc.euclid.matrix.Matrix3D;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.matrix.interfaces.Matrix3DReadOnly;
import us.ihmc.euclid.orientation.interfaces.Orientation3DReadOnly;
import us.ihmc.euclid.rotationConversion.RotationVectorConversion;
import us.ihmc.euclid.rotationConversion.YawPitchRollConversion;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tools.QuaternionTools;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DBasics;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DBasics;

/**
 * Read-only interface for unit-quaternion used to represent 3D orientations.
 * <p>
 * When describing a 4D tuple, its 4 components are often gathered in two groups: the scalar part
 * {@code s} and the vector part ({@code x}, {@code y}, {@code z}).
 * </p>
 * <p>
 * Note on the difference between applying a 3D transform on a quaternion and a 4D vector:
 * <ul>
 * <li>When transformed by a homogeneous transformation matrix, a quaternion is only pre-multiplied
 * by the rotation part of the transform, resulting in concatenating the orientations of the
 * transform and the quaternion.
 * <li>When transformed by a homogeneous transformation matrix, a 4D vector scalar part {@code s}
 * remains unchanged. The vector part ({@code x}, {@code y}, {@code z}) is scaled and rotated, and
 * translated by {@code s} times the translation part of the transform. Note that for {@code s = 0},
 * a 4D vector behaves as a 3D vector, and for {@code s = 1} it behaves as a 3D point.
 * </ul>
 * </p>
 *
 * @author Sylvain Bertrand
 */
public interface QuaternionReadOnly extends Tuple4DReadOnly, Orientation3DReadOnly
{
   /** Threshold used to trigger a more expensive comparison between two quaternions. */
   public static final double GEOMETRICALLY_EQUALS_THRESHOLD = 0.005;
   /** Default tolerance used to verify that this quaternion is a unit-quaternion. */
   public static final double EPS_UNITARY = 1.0e-7;

   /**
    * Tests if this quaternion has a norm equal to 1+/-{@code epsilon}.
    *
    * @param epsilon the tolerance to use.
    * @return {@code true} if this quaternion is a proper unit-quaternion, {@code false} otherwise.
    */
   default boolean isUnitary(double epsilon)
   {
      return Math.abs(norm() - 1.0) < epsilon;
   }

   /**
    * Tests if this quaternion represents a rotation around the z-axis.
    * <p>
    * This is commonly used to test if the quaternion can be used to transform 2D geometry object.
    * </p>
    *
    * @param epsilon the tolerance to use.
    * @return {@code true} if this quaternion represents a rotation around the z-axis, {@code false}
    *         otherwise.
    * @deprecated Use {@link #isOrientation2D(double)} instead
    */
   @Deprecated
   default boolean isZOnly(double epsilon)
   {
      return isOrientation2D(epsilon);
   }

   /**
    * {@inheritDoc}
    * <p>
    * A quaternion is an orientation 2D if:
    * <ul>
    * <li>the absolute value of the x component is less than {@code epsilon}.
    * <li>the absolute value of the y component is less than {@code epsilon}.
    * </ul>
    * </p>
    */
   @Override
   default boolean isOrientation2D(double epsilon)
   {
      return Math.abs(getX()) < epsilon && Math.abs(getY()) < epsilon;
   }

   /**
    * Asserts that this quaternion has a norm equal to 1+/-{@value #EPS_UNITARY}.
    *
    * @param epsilon the tolerance to use.
    * @throws RuntimeException if this quaternion is not a proper unit-quaternion.
    */
   default void checkIfUnitary()
   {
      checkIfUnitary(EPS_UNITARY);
   }

   /**
    * Asserts that this quaternion has a norm equal to 1+/-{@code epsilon}.
    *
    * @param epsilon the tolerance to use.
    * @throws RuntimeException if this quaternion is not a proper unit-quaternion.
    */
   default void checkIfUnitary(double epsilon)
   {
      if (!isUnitary(epsilon))
         throw new RuntimeException("This quaternion is not a unit-quaternion.");
   }

   /**
    * Asserts that this quaternion represents a rotation around the z-axis.
    * <p>
    * This is commonly used to test if the quaternion can be used to transform 2D geometry object.
    * </p>
    *
    * @param epsilon the tolerance to use.
    * @throws NotAMatrix2DException if this quaternion does not represent a rotation around the z-axis.
    * @deprecated Use {@link #checkIfOrientation2D(double)} instead
    */
   @Deprecated
   default void checkIfIsZOnly(double epsilon)
   {
      checkIfOrientation2D(epsilon);
   }

   /**
    * Efficiently compute the norm of this quaternion.
    */
   @Override
   default double norm()
   {
      return EuclidCoreTools.fastSquareRoot(normSquared());
   }

   /**
    * Computes and returns the distance from this quaternion to {@code other}.
    *
    * @param other the other quaternion to measure the distance. Not modified.
    * @return the angle representing the distance between the two quaternions. It is contained in [0,
    *         2<i>pi</i>]
    */
   default double distance(QuaternionReadOnly other)
   {
      double dot = dot(other);
      if (dot > 1.0)
         dot = 1.0;
      else if (dot < -1.0)
         dot = -1.0;
      return 2.0 * Math.acos(dot);
   }

   /**
    * Computes and returns the distance from this quaternion to {@code other}.
    * <p>
    * This method is equivalent to {@link #distance(QuaternionReadOnly)} but is more accurate when
    * computing the distance between two quaternions that are very close. Note that it is also more
    * expensive.
    * </p>
    *
    * @param other the other quaternion to measure the distance. Not modified.
    * @return the angle representing the distance between the two quaternions. It is contained in [0,
    *         2<i>pi</i>]
    */
   default double distancePrecise(QuaternionReadOnly other)
   {
      return QuaternionTools.distancePrecise(this, other);
   }

   /**
    * Calculates and returns the angle of the rotation this quaternion represents.
    *
    * @return the angle &in; [-2<i>pi</i>;2<i>pi</i>].
    */
   default double getAngle()
   {
      double sinHalfTheta = Math.sqrt(EuclidCoreTools.normSquared(getX(), getY(), getZ()));
      return 2.0 * Math.atan2(sinHalfTheta, getS());
   }

   /**
    * Computes and packs the orientation described by this quaternion as a rotation vector.
    * <p>
    * WARNING: a rotation vector is different from a yaw-pitch-roll or Euler angles representation. A
    * rotation vector is equivalent to the axis of an axis-angle that is multiplied by the angle of the
    * same axis-angle.
    * </p>
    *
    * @param rotationVectorToPack the vector in which the rotation vector is stored. Modified.
    * @deprecated Use {@link #getRotationVector(Vector3DBasics)} instead
    */
   @Deprecated
   default void get(Vector3DBasics rotationVectorToPack)
   {
      getRotationVector(rotationVectorToPack);
   }

   @Override
   default void get(RotationMatrix rotationMatrixToPack)
   {
      rotationMatrixToPack.setQuaternion(getX(), getY(), getZ(), getS());
   }

   @Override
   default void get(AxisAngleBasics axisAngleToPack)
   {
      axisAngleToPack.setQuaternion(getX(), getY(), getZ(), getS());
   }

   @Override
   default void get(QuaternionBasics quaternionToPack)
   {
      quaternionToPack.setQuaternion(getX(), getY(), getZ(), getS());
   }

   /**
    * Computes and packs the orientation described by this quaternion as a rotation vector.
    * <p>
    * WARNING: a rotation vector is different from a yaw-pitch-roll or Euler angles representation. A
    * rotation vector is equivalent to the axis of an axis-angle that is multiplied by the angle of the
    * same axis-angle.
    * </p>
    *
    * @param rotationVectorToPack the vector in which the rotation vector is stored. Modified.
    */
   @Override
   default void getRotationVector(Vector3DBasics rotationVectorToPack)
   {
      RotationVectorConversion.convertQuaternionToRotationVector(this, rotationVectorToPack);
   }

   /**
    * Computes and packs the orientation described by this quaternion as the yaw-pitch-roll angles.
    * <p>
    * WARNING: the Euler angles or yaw-pitch-roll representation is sensitive to gimbal lock and is
    * sometimes undefined.
    * </p>
    *
    * @param yawPitchRollToPack the array in which the yaw-pitch-roll angles are stored. Modified.
    */
   @Override
   default void getYawPitchRoll(double[] yawPitchRollToPack)
   {
      YawPitchRollConversion.convertQuaternionToYawPitchRoll(this, yawPitchRollToPack);
   }

   /**
    * Computes and packs the orientation described by this quaternion as the Euler angles.
    * <p>
    * WARNING: the Euler angles or yaw-pitch-roll representation is sensitive to gimbal lock and is
    * sometimes undefined.
    * </p>
    *
    * @param eulerAnglesToPack the tuple in which the Euler angles are stored. Modified.
    */
   default void getEuler(Vector3DBasics eulerAnglesToPack)
   {
      YawPitchRollConversion.convertQuaternionToYawPitchRoll(this, eulerAnglesToPack);
   }

   /**
    * Computes and returns the yaw angle from the yaw-pitch-roll representation of this quaternion.
    * <p>
    * WARNING: the Euler angles or yaw-pitch-roll representation is sensitive to gimbal lock and is
    * sometimes undefined.
    * </p>
    *
    * @return the yaw angle around the z-axis.
    */
   @Override
   default double getYaw()
   {
      return YawPitchRollConversion.computeYaw(this);
   }

   /**
    * Computes and returns the pitch angle from the yaw-pitch-roll representation of this quaternion.
    * <p>
    * WARNING: the Euler angles or yaw-pitch-roll representation is sensitive to gimbal lock and is
    * sometimes undefined.
    * </p>
    *
    * @return the pitch angle around the y-axis.
    */
   @Override
   default double getPitch()
   {
      return YawPitchRollConversion.computePitch(this);
   }

   /**
    * Computes and returns the roll angle from the yaw-pitch-roll representation of this quaternion.
    * <p>
    * WARNING: the Euler angles or yaw-pitch-roll representation is sensitive to gimbal lock and is
    * sometimes undefined.
    * </p>
    *
    * @return the roll angle around the x-axis.
    */
   @Override
   default double getRoll()
   {
      return YawPitchRollConversion.computeRoll(this);
   }

   /**
    * Transforms the given tuple {@code tupleOriginal} by this quaternion and stores the result in
    * {@code tupleTransformed}.
    * <p>
    * tupleTransformed = quaternion * tupleOriginal * quaternion<sup>-1</sup>
    * </p>
    *
    * @param tupleOriginal the tuple to transform. Not modified.
    * @param tupleTransformed the tuple to store the result. Modified.
    */
   @Override
   default void transform(Tuple3DReadOnly tupleOriginal, Tuple3DBasics tupleTransformed)
   {
      QuaternionTools.transform(this, tupleOriginal, tupleTransformed);
   }

   @Override
   default void addTransform(Tuple3DReadOnly tupleOriginal, Tuple3DBasics tupleTransformed)
   {
      QuaternionTools.addTransform(this, tupleOriginal, tupleTransformed);
   }

   /**
    * Transforms the given tuple {@code tupleOriginal} by this quaternion and stores the result in
    * {@code tupleTransformed}.
    * <p>
    * tupleTransformed = quaternion * tupleOriginal * quaternion<sup>-1</sup>
    * </p>
    *
    * @param tupleOriginal the tuple to transform. Not modified.
    * @param tupleTransformed the tuple to store the result. Modified.
    * @param checkIfOrientation2D whether this method should assert that this quaternion represents a
    *           transformation in the XY plane.
    * @throws NotAMatrix2DException if {@code checkIfTransformInXYPlane == true} and this matrix does
    *            not represent a transformation in the XY plane.
    */
   @Override
   default void transform(Tuple2DReadOnly tupleOriginal, Tuple2DBasics tupleTransformed, boolean checkIfOrientation2D)
   {
      QuaternionTools.transform(this, tupleOriginal, tupleTransformed, checkIfOrientation2D);
   }

   /**
    * Transforms the given 3D matrix {@code matrixOriginal} by this quaternion and stores the result in
    * {@code matrixTransformed}.
    * <p>
    * matrixTransformed = this * matrixOriginal * this<sup>-1</sup>
    * </p>
    *
    * @param matrixOriginal the matrix to transform. Not modified.
    * @param matrixTransformed the matrix in which the result is stored. Modified.
    */
   @Override
   default void transform(Matrix3DReadOnly matrixOriginal, Matrix3D matrixTransformed)
   {
      QuaternionTools.transform(this, matrixOriginal, matrixTransformed);
   }

   /**
    * Transforms the vector part of the given 4D vector {@code vectorOriginal} and stores the result
    * into {@code vectorTransformed}.
    * <p>
    * vectorTransformed.s = vectorOriginal.s <br>
    * vectorTransformed.xyz = this * vectorOriginal.xyz * this<sup>-1</sup>
    * </p>
    *
    * @param vectorOriginal the vector to transform. Not modified.
    * @param vectorTransformed the vector in which the result is stored. Modified.
    */
   @Override
   default void transform(Vector4DReadOnly vectorOriginal, Vector4DBasics vectorTransformed)
   {
      QuaternionTools.transform(this, vectorOriginal, vectorTransformed);
   }

   /**
    * Performs the inverse of the transform to the given tuple by this quaternion.
    * <p>
    * tupleToTransform = this<sup>-1</sup> * tupleToTransform * this
    * </p>
    *
    * @param tupleToTransform the tuple to transform. Modified.
    * @throws NotAMatrix2DException if this quaternion does not represent a transformation in the XY
    *            plane.
    */
   @Override
   default void inverseTransform(Tuple3DReadOnly tupleOriginal, Tuple3DBasics tupleTransformed)
   {
      QuaternionTools.inverseTransform(this, tupleOriginal, tupleTransformed);
   }

   /**
    * Performs the inverse of the transform to the given tuple {@code tupleOriginal} by this quaternion
    * and stores the result in {@code tupleTransformed}.
    * <p>
    * tupleTransformed = this<sup>-1</sup> * tupleOriginal * this
    * </p>
    *
    * @param tupleOriginal the tuple to transform. Not modified.
    * @param tupleTransformed the tuple in which the result is stored. Modified.
    * @param checkIfOrientation2D whether this method should assert that this quaternion represents a
    *           transformation in the XY plane.
    * @throws NotAMatrix2DException if {@code checkIfTransformInXYPlane == true} and this quaternion
    *            does not represent a transformation in the XY plane.
    */
   @Override
   default void inverseTransform(Tuple2DReadOnly tupleOriginal, Tuple2DBasics tupleTransformed, boolean checkIfOrientation2D)
   {
      QuaternionTools.inverseTransform(this, tupleOriginal, tupleTransformed, checkIfOrientation2D);
   }

   /**
    * Performs the inverse of the transform to the vector part the given 4D vector
    * {@code vectorOriginal} by this quaternion and stores the result in {@code vectorTransformed}.
    * <p>
    * vectorTransformed.s = vectorOriginal.s <br>
    * vectorTransformed.xyz = this<sup>-1</sup> * vectorOriginal.xyz * this
    * </p>
    *
    * @param vectorOriginal the vector to transform. Not modified.
    * @param vectorTransformed the vector in which the result is stored. Modified.
    */
   @Override
   default void inverseTransform(Vector4DReadOnly vectorOriginal, Vector4DBasics vectorTransformed)
   {
      QuaternionTools.inverseTransform(this, vectorOriginal, vectorTransformed);
   }

   /**
    * Performs the inverse of the transforms to the given 3D matrix {@code matrixOriginal} by this
    * quaternion and stores the result in {@code matrixTransformed}.
    * <p>
    * s matrixTransformed = this<sup>-1</sup> * matrixOriginal * this
    * </p>
    *
    * @param matrixOriginal the matrix to transform. Not modified.
    * @param matrixTransformed the matrix in which the result is stored. Modified.
    */
   @Override
   default void inverseTransform(Matrix3DReadOnly matrixOriginal, Matrix3D matrixTransformed)
   {
      QuaternionTools.inverseTransform(this, matrixOriginal, matrixTransformed);
   }

   /**
    * Tests if {@code this} and {@code other} represent the same orientation to an {@code epsilon}.
    * <p>
    * Two quaternions are considered geometrically equal if the magnitude of their difference is less
    * than or equal to {@code epsilon}.
    * </p>
    * <p>
    * Note that two quaternions of opposite sign are considered equal, such that the two quaternions
    * {@code q1 = (x, y, z, s)} and {@code q2 = (-x, -y, -z, -s)} are considered geometrically equal.
    * </p>
    * <p>
    * Note that {@code this.geometricallyEquals(other, epsilon) == true} does not necessarily imply
    * {@code this.epsilonEquals(other, epsilon)} and vice versa.
    * </p>
    *
    * @param other the other quaternion to compare against this. Not modified.
    * @param epsilon the maximum angle of the difference quaternion can be for the two quaternions to
    *           be considered equal.
    * @return {@code true} if the two quaternions represent the same geometry, {@code false} otherwise.
    */
   default boolean geometricallyEquals(QuaternionReadOnly other, double epsilon)
   {
      if (epsilon >= Math.PI)
         return true; // Trivial case. If epsilon is greater than pi, then any pair of quaternions are equal.

      double angle;
      if (epsilon > GEOMETRICALLY_EQUALS_THRESHOLD)
         angle = distance(other);
      else
         angle = distancePrecise(other);
      return Math.abs(EuclidCoreTools.trimAngleMinusPiToPi(angle)) <= epsilon;
   }
}