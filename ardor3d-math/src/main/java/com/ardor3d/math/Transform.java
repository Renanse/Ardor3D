/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Transform models a transformation in 3d space as: Y = M*X+T, with M being a Matrix3 and T is a
 * Vector3. Generally M will be a rotation only matrix in which case it is represented by the matrix
 * and scale fields as R*S, where S is a positive scale vector. For non-uniform scales and
 * reflections, use setMatrix, which will consider M as being a general 3x3 matrix and disregard
 * anything set in scale.
 */
public class Transform implements Cloneable, Savable, Externalizable, ReadOnlyTransform, Poolable {
  /**
   * Used with equals method to determine if two Transforms are close enough to be considered equal.
   */
  public static final double ALLOWED_DEVIANCE = 0.00000001;

  private static final long serialVersionUID = 1L;

  private static final ObjectPool<Transform> TRANS_POOL =
      ObjectPool.create(Transform.class, MathConstants.maxMathPoolSize);

  /**
   * Identity transform.
   */
  public static final ReadOnlyTransform IDENTITY =
      new Transform(Matrix3.IDENTITY, Vector3.ONE, Vector3.ZERO, true, true, true);

  protected final Matrix3 _matrix = new Matrix3(Matrix3.IDENTITY);
  protected final Vector3 _translation = new Vector3(Vector3.ZERO);
  protected final Vector3 _scale = new Vector3(Vector3.ONE);

  /**
   * true if this transform is guaranteed to be the identity matrix.
   */
  protected boolean _identity;

  /**
   * true if the matrix portion of this transform is only rotation.
   */
  protected boolean _rotationMatrix;

  /**
   * true if scale is used and scale is guaranteed to be uniform.
   */
  protected boolean _uniformScale;

  /**
   * Constructs a new Transform object.
   */
  public Transform() {
    _identity = true;
    _rotationMatrix = true;
    _uniformScale = true;

  }

  /**
   * Constructs a new Transform object from the information stored in the given source Transform.
   *
   * @param source
   * @throws NullPointerException
   *           if source is null.
   */
  public Transform(final ReadOnlyTransform source) {
    _matrix.set(source.getMatrix());
    _scale.set(source.getScale());
    _translation.set(source.getTranslation());

    _identity = source.isIdentity();
    _rotationMatrix = source.isRotationMatrix();
    _uniformScale = source.isUniformScale();

  }

  /**
   * Internal only constructor, generally used for making an immutable transform.
   *
   * @param matrix
   * @param scale
   * @param translation
   * @param identity
   * @param rotationMatrix
   * @param uniformScale
   * @throws NullPointerException
   *           if a param is null.
   */
  protected Transform(final ReadOnlyMatrix3 matrix, final ReadOnlyVector3 scale, final ReadOnlyVector3 translation,
    final boolean identity, final boolean rotationMatrix, final boolean uniformScale) {
    _matrix.set(matrix);
    _scale.set(scale);
    _translation.set(translation);

    _identity = identity;
    _rotationMatrix = rotationMatrix;
    _uniformScale = uniformScale;
  }

  @Override
  public ReadOnlyMatrix3 getMatrix() { return _matrix; }

  @Override
  public ReadOnlyVector3 getTranslation() { return _translation; }

  @Override
  public ReadOnlyVector3 getScale() { return _scale; }

  /**
   * @return true if this transform is guaranteed to be the identity matrix.
   */
  @Override
  public boolean isIdentity() { return _identity; }

  /**
   * @return true if the matrix portion of this transform is only rotation.
   */
  @Override
  public boolean isRotationMatrix() { return _rotationMatrix; }

  /**
   * @return true if scale is used and scale is guaranteed to be uniform.
   */
  @Override
  public boolean isUniformScale() { return _uniformScale; }

  /**
   * Resets this transform to identity and resets all flags.
   *
   * @return this Transform for chaining.
   */
  public Transform setIdentity() {
    _matrix.set(Matrix3.IDENTITY);
    _scale.set(Vector3.ONE);
    _translation.set(Vector3.ZERO);
    _identity = true;
    _rotationMatrix = true;
    _uniformScale = true;
    return this;
  }

  /**
   * Sets the matrix portion of this transform to the given value.
   *
   * NB: Calling this with a matrix that is not purely rotational (orthonormal) will result in a
   * Transform whose scale comes from its matrix. Further attempts to set scale directly will throw an
   * error.
   *
   * @param rotation
   *          our new matrix.
   * @return this transform for chaining.
   * @throws NullPointerException
   *           if rotation is null.
   * @see Matrix3#isOrthonormal()
   */
  public Transform setRotation(final ReadOnlyMatrix3 rotation) {
    _matrix.set(rotation);
    updateFlags(false);
    return this;
  }

  /**
   * Sets the matrix portion of this transform to the rotational value of the given Quaternion.
   * Calling this allows scale to be set and used.
   *
   * @param rotation
   * @return this transform for chaining.
   * @throws NullPointerException
   *           if rotation is null.
   */
  public Transform setRotation(final ReadOnlyQuaternion rotation) {
    _matrix.set(rotation);
    updateFlags(true);
    return this;
  }

  /**
   * Sets the translation portion of this transform to the given value.
   *
   * @param translation
   * @return this transform for chaining.
   * @throws NullPointerException
   *           if translation is null.
   */
  public Transform setTranslation(final ReadOnlyVector3 translation) {
    _translation.set(translation);
    _identity = false;
    return this;
  }

  /**
   * Sets the translation portion of this transform to the given values.
   *
   * @param x
   * @param y
   * @param z
   * @return this transform for chaining.
   */
  public Transform setTranslation(final double x, final double y, final double z) {
    _translation.set(x, y, z);
    _identity = false;
    return this;
  }

  /**
   * Sets the scale portion of this transform to the given value.
   *
   * @param scale
   * @return this transform for chaining.
   * @throws NullPointerException
   *           if scale is null.
   * @throws TransformException
   *           if this transform has a generic 3x3 matrix set.
   * @throws IllegalArgumentException
   *           if scale is (0,0,0)
   */
  public Transform setScale(final ReadOnlyVector3 scale) {
    if (!_rotationMatrix) {
      throw new TransformException(
          "Scale is already provided by 3x3 matrix.  If this is a mistake, consider using setRotation instead of setMatrix.");
    }
    if (scale.getX() == 0.0 && scale.getY() == 0.0 && scale.getZ() == 0.0) {
      throw new IllegalArgumentException("scale may not be ZERO.");
    }

    _scale.set(scale);
    _identity = _identity && scale.getX() == 1.0 && scale.getY() == 1.0 && scale.getZ() == 1.0;
    _uniformScale = scale.getX() == scale.getY() && scale.getY() == scale.getZ();
    return this;
  }

  /**
   * Sets the scale portion of this transform to the given values.
   *
   * @param x
   * @param y
   * @param z
   * @return this transform for chaining.
   * @throws NullPointerException
   *           if scale is null.
   * @throws TransformException
   *           if this transform has a generic 3x3 matrix set.
   * @throws IllegalArgumentException
   *           if scale is (0,0,0)
   */
  public Transform setScale(final double x, final double y, final double z) {
    if (!_rotationMatrix) {
      throw new TransformException(
          "Scale is already provided by 3x3 matrix.  If this is a mistake, consider using setRotation instead of setMatrix.");
    }
    if (x == 0.0 && y == 0.0 && z == 0.0) {
      throw new IllegalArgumentException("scale may not be ZERO.");
    }

    _scale.set(x, y, z);
    _identity = false;
    _uniformScale = x == y && y == z;
    return this;
  }

  /**
   * Sets the scale portion of this transform to the given value as a vector (u, u, u)
   *
   * @param uniformScale
   * @return this transform for chaining.
   * @throws TransformException
   *           if this transform has a generic 3x3 matrix set.
   * @throws IllegalArgumentException
   *           if uniformScale is 0
   */
  public Transform setScale(final double uniformScale) {
    if (!_rotationMatrix) {
      throw new TransformException(
          "Scale is already provided by 3x3 matrix.  If this is a mistake, consider using setRotation instead of setMatrix.");
    }
    if (uniformScale == 0.0) {
      throw new IllegalArgumentException("scale may not be ZERO.");
    }

    _scale.set(uniformScale, uniformScale, uniformScale);
    _identity = false;
    _uniformScale = true;
    return this;
  }

  /**
   * Copies the given transform values into this transform object.
   *
   * @param source
   * @return this transform for chaining.
   * @throws NullPointerException
   *           if source is null.
   */
  public Transform set(final ReadOnlyTransform source) {
    if (source.isIdentity()) {
      setIdentity();
    } else {
      _matrix.set(source.getMatrix());
      _scale.set(source.getScale());
      _translation.set(source.getTranslation());

      _identity = false;
      _rotationMatrix = source.isRotationMatrix();
      _uniformScale = source.isUniformScale();
    }
    return this;
  }

  /**
   * Locally adds to the translation of this transform.
   *
   * @param x
   * @param y
   * @param z
   * @return this transform for chaining.
   */
  public Transform translate(final double x, final double y, final double z) {
    _translation.addLocal(x, y, z);
    _identity = _identity && _translation.equals(Vector3.ZERO);
    return this;
  }

  /**
   * Locally adds to the translation of this transform.
   *
   * @param vec
   * @return this transform for chaining.
   */
  public Transform translate(final ReadOnlyVector3 vec) {
    _translation.addLocal(vec);
    _identity = _identity && _translation.equals(Vector3.ZERO);
    return this;
  }

  /**
   * Locally applies this transform to the given point: P' = M*P+T
   *
   * @param point
   * @return the transformed point.
   * @throws NullPointerException
   *           if point is null.
   */
  @Override
  public Vector3 applyForward(final Vector3 point) {
    if (point == null) {
      throw new NullPointerException();
    }

    if (_identity) {
      // No need to make changes
      // Y = X
      return point;
    }

    if (_rotationMatrix) {
      // Scale is separate from matrix
      // Y = R*S*X + T
      point.set(point.getX() * _scale.getX(), point.getY() * _scale.getY(), point.getZ() * _scale.getZ());
      _matrix.applyPost(point, point);
      point.addLocal(_translation);
      return point;
    }

    // scale is part of matrix.
    // Y = M*X + T
    _matrix.applyPost(point, point);
    point.addLocal(_translation);
    return point;

  }

  /**
   * Applies this transform to the given point and returns the result in the given store vector: P' =
   * M*P+T
   *
   * @param point
   * @param store
   *          the vector to store our result in. if null, a new vector will be created.
   * @return the transformed point.
   * @throws NullPointerException
   *           if point is null.
   */
  @Override
  public Vector3 applyForward(final ReadOnlyVector3 point, final Vector3 store) {
    Vector3 result = store;
    if (result == null) {
      result = new Vector3();
    }
    result.set(point);
    return applyForward(result);
  }

  /**
   * Locally applies the inverse of this transform to the given point: P' = M^{-1}*(P-T)
   *
   * @param point
   * @return the transformed point.
   * @throws NullPointerException
   *           if point is null.
   */
  @Override
  public Vector3 applyInverse(final Vector3 point) {
    if (point == null) {
      throw new NullPointerException();
    }

    if (_identity) {
      // No need to make changes
      // P' = P
      return point;
    }

    // Back track translation
    point.subtractLocal(_translation);

    if (_rotationMatrix) {
      // Scale is separate from matrix so...
      // P' = S^{-1}*R^t*(P - T)
      _matrix.applyPre(point, point);
      if (_uniformScale) {
        point.divideLocal(_scale.getX());
      } else {
        point.setX(point.getX() / _scale.getX());
        point.setY(point.getY() / _scale.getY());
        point.setZ(point.getZ() / _scale.getZ());
      }
    } else {
      // P' = M^{-1}*(P - T)
      final Matrix3 invertedMatrix = _matrix.invert(Matrix3.fetchTempInstance());
      invertedMatrix.applyPost(point, point);
      Matrix3.releaseTempInstance(invertedMatrix);
    }

    return point;
  }

  /**
   * Applies the inverse of this transform to the given point and returns the result in the given
   * store vector: P' = M^{-1}*(P-T)
   *
   * @param point
   * @param store
   *          the vector to store our result in. if null, a new vector will be created.
   * @return the transformed point.
   * @throws NullPointerException
   *           if point is null.
   */
  @Override
  public Vector3 applyInverse(final ReadOnlyVector3 point, final Vector3 store) {
    Vector3 result = store;
    if (result == null) {
      result = new Vector3();
    }
    result.set(point);
    return applyInverse(result);
  }

  /**
   * Locally applies this transform to the given vector: V' = M*V
   *
   * @param vector
   * @return the transformed vector.
   * @throws NullPointerException
   *           if vector is null.
   */
  @Override
  public Vector3 applyForwardVector(final Vector3 vector) {
    if (vector == null) {
      throw new NullPointerException();
    }

    if (_identity) {
      // No need to make changes
      // V' = V
      return vector;
    }

    if (_rotationMatrix) {
      // Scale is separate from matrix
      // V' = R*S*V
      vector.set(vector.getX() * _scale.getX(), vector.getY() * _scale.getY(), vector.getZ() * _scale.getZ());
      _matrix.applyPost(vector, vector);
      return vector;
    }

    // scale is part of matrix.
    // V' = M*V
    _matrix.applyPost(vector, vector);
    return vector;

  }

  /**
   * Applies this transform to the given vector and returns the result in the given store vector: V' =
   * M*V
   *
   * @param vector
   * @param store
   *          the vector to store our result in. if null, a new vector will be created.
   * @return the transformed vector.
   * @throws NullPointerException
   *           if vector is null.
   */
  @Override
  public Vector3 applyForwardVector(final ReadOnlyVector3 vector, final Vector3 store) {
    Vector3 result = store;
    if (result == null) {
      result = new Vector3();
    }
    result.set(vector);
    return applyForwardVector(result);
  }

  /**
   * Locally applies the inverse of this transform to the given vector: V' = M^{-1}*V
   *
   * @param vector
   * @return the transformed vector.
   * @throws NullPointerException
   *           if vector is null.
   */
  @Override
  public Vector3 applyInverseVector(final Vector3 vector) {
    if (vector == null) {
      throw new NullPointerException();
    }

    if (_identity) {
      // No need to make changes
      // V' = V
      return vector;
    }

    if (_rotationMatrix) {
      // Scale is separate from matrix so...
      // V' = S^{-1}*R^t*V
      _matrix.applyPre(vector, vector);
      if (_uniformScale) {
        vector.divideLocal(_scale.getX());
      } else {
        vector.setX(vector.getX() / _scale.getX());
        vector.setY(vector.getY() / _scale.getY());
        vector.setZ(vector.getZ() / _scale.getZ());
      }
    } else {
      // V' = M^{-1}*V
      final Matrix3 invertedMatrix = _matrix.invert(Matrix3.fetchTempInstance());
      invertedMatrix.applyPost(vector, vector);
      Matrix3.releaseTempInstance(invertedMatrix);
    }

    return vector;
  }

  /**
   * Applies the inverse of this transform to the given vector and returns the result in the given
   * store vector: V' = M^{-1}*V
   *
   * @param vector
   * @param store
   *          the vector to store our result in. if null, a new vector will be created.
   * @return the transformed vector.
   * @throws NullPointerException
   *           if vector is null.
   */
  @Override
  public Vector3 applyInverseVector(final ReadOnlyVector3 vector, final Vector3 store) {
    Vector3 result = store;
    if (result == null) {
      result = new Vector3();
    }
    result.set(vector);
    return applyInverseVector(result);
  }

  /**
   * Calculates the product of this transform with the given "transformBy" transform (P = this * T)
   * and stores this in store.
   *
   * @param transformBy
   * @param store
   *          the transform to store the result in for return. If null, a new transform object is
   *          created and returned. It is NOT safe for store to be the same object as transformBy or
   *          "this".
   * @return the product
   * @throws NullPointerException
   *           if transformBy is null.
   */
  @Override
  public Transform multiply(final ReadOnlyTransform transformBy, final Transform store) {
    Transform result = store;
    if (result == null) {
      result = new Transform();
    }

    if (_identity) {
      return result.set(transformBy);
    }

    if (transformBy.isIdentity()) {
      return result.set(this);
    }

    if (_rotationMatrix && transformBy.isRotationMatrix() && _uniformScale) {
      result._rotationMatrix = true;
      final Matrix3 newRotation = result._matrix;
      newRotation.set(_matrix).multiplyLocal(transformBy.getMatrix());

      final Vector3 newTranslation = result._translation.set(transformBy.getTranslation());
      _matrix.applyPost(newTranslation, newTranslation);
      // uniform scale, so just use X.
      newTranslation.multiplyLocal(_scale.getX());
      newTranslation.addLocal(_translation);

      if (transformBy.isUniformScale()) {
        result.setScale(_scale.getX() * transformBy.getScale().getX());
      } else {
        final Vector3 scale = result._scale.set(transformBy.getScale());
        scale.multiplyLocal(_scale.getX());
      }

      // update our flags in one place.
      result.updateFlags(true);

      return result;
    }

    // In all remaining cases, the matrix cannot be written as R*S*X+T.
    final ReadOnlyMatrix3 matrixA =
        isRotationMatrix() ? _matrix.multiplyDiagonalPost(_scale, Matrix3.fetchTempInstance()) : _matrix;

    final ReadOnlyMatrix3 matrixB = transformBy.isRotationMatrix()
        ? transformBy.getMatrix().multiplyDiagonalPost(transformBy.getScale(), Matrix3.fetchTempInstance())
        : transformBy.getMatrix();

    final Matrix3 newMatrix = result._matrix;
    newMatrix.set(matrixA).multiplyLocal(matrixB);

    final Vector3 newTranslate = result._translation;
    matrixA.applyPost(transformBy.getTranslation(), newTranslate).addLocal(getTranslation());

    if (isRotationMatrix()) {
      Matrix3.releaseTempInstance((Matrix3) matrixA);
    }
    if (transformBy.isRotationMatrix()) {
      Matrix3.releaseTempInstance((Matrix3) matrixB);
    }

    // prevent scale bleeding since we don't set it.
    result._scale.set(1.0, 1.0, 1.0);

    // update our flags in one place.
    result.updateFlags(false);

    return result;
  }

  /**
   * Updates _rotationMatrix, _uniformScale and _identity based on the current contents of this
   * Transform.
   *
   * @param rotationMatrixGuaranteed
   *          true if we know for sure that the _matrix component is rotational.
   */
  protected void updateFlags(final boolean rotationMatrixGuaranteed) {
    _identity = _translation.equals(Vector3.ZERO) && _matrix.isIdentity() && _scale.equals(Vector3.ONE);
    if (_identity) {
      _rotationMatrix = true;
      _uniformScale = true;
    } else {
      _rotationMatrix = rotationMatrixGuaranteed ? true : _matrix.isOrthonormal();
      _uniformScale = _rotationMatrix && _scale.getX() == _scale.getY() && _scale.getY() == _scale.getZ();
    }
  }

  /**
   * Calculates the inverse of this transform.
   *
   * @param store
   *          the transform to store the result in for return. If null, a new transform object is
   *          created and returned. It IS safe for store to be the same object as "this".
   * @return the inverted transform
   */
  @Override
  public Transform invert(final Transform store) {
    Transform result = store;
    if (result == null) {
      result = new Transform();
    }

    if (_identity) {
      result.setIdentity();
      return result;
    }

    final Matrix3 newMatrix = result._matrix.set(_matrix);
    if (_rotationMatrix) {
      if (_uniformScale) {
        final double sx = _scale.getX();
        newMatrix.transposeLocal();
        if (sx != 1.0) {
          newMatrix.multiplyLocal(1.0 / sx);
        }
      } else {
        newMatrix.multiplyDiagonalPost(_scale, newMatrix).invertLocal();
      }
    } else {
      newMatrix.invertLocal();
    }

    result._matrix.applyPost(_translation, result._translation).negateLocal();
    result.updateFlags(_rotationMatrix);

    return result;
  }

  /**
   * @param store
   *          the matrix to store the result in for return. If null, a new matrix object is created
   *          and returned.
   * @return this transform represented as a 4x4 matrix:
   *
   *         <pre>
   * R R R Tx
   * R R R Ty
   * R R R Tz
   * 0 0 0 1
   *         </pre>
   */
  @Override
  public Matrix4 getHomogeneousMatrix(final Matrix4 store) {
    Matrix4 result = store;
    if (result == null) {
      result = new Matrix4();
    }

    if (_rotationMatrix) {
      result._m00 = _scale.getX() * _matrix._m00;
      result._m01 = _scale.getX() * _matrix._m01;
      result._m02 = _scale.getX() * _matrix._m02;
      result._m10 = _scale.getY() * _matrix._m10;
      result._m11 = _scale.getY() * _matrix._m11;
      result._m12 = _scale.getY() * _matrix._m12;
      result._m20 = _scale.getZ() * _matrix._m20;
      result._m21 = _scale.getZ() * _matrix._m21;
      result._m22 = _scale.getZ() * _matrix._m22;
    } else {
      result._m00 = _matrix._m00;
      result._m01 = _matrix._m01;
      result._m02 = _matrix._m02;
      result._m10 = _matrix._m10;
      result._m11 = _matrix._m11;
      result._m12 = _matrix._m12;
      result._m20 = _matrix._m20;
      result._m21 = _matrix._m21;
      result._m22 = _matrix._m22;
    }

    result._m30 = 0.0;
    result._m31 = 0.0;
    result._m32 = 0.0;

    result._m03 = _translation.getX();
    result._m13 = _translation.getY();
    result._m23 = _translation.getZ();
    result._m33 = 1.0;

    return result;
  }

  @Override
  public void getGLApplyMatrix(final DoubleBuffer store) {
    store.put(3, 0.0);
    store.put(7, 0.0);
    store.put(11, 0.0);

    if (_rotationMatrix) {
      store.put(0, _scale.getX() * _matrix._m00);
      store.put(1, _scale.getX() * _matrix._m10);
      store.put(2, _scale.getX() * _matrix._m20);
      store.put(4, _scale.getY() * _matrix._m01);
      store.put(5, _scale.getY() * _matrix._m11);
      store.put(6, _scale.getY() * _matrix._m21);
      store.put(8, _scale.getZ() * _matrix._m02);
      store.put(9, _scale.getZ() * _matrix._m12);
      store.put(10, _scale.getZ() * _matrix._m22);
    } else {
      store.put(0, _matrix._m00);
      store.put(1, _matrix._m10);
      store.put(2, _matrix._m20);
      store.put(4, _matrix._m01);
      store.put(5, _matrix._m11);
      store.put(6, _matrix._m21);
      store.put(8, _matrix._m02);
      store.put(9, _matrix._m12);
      store.put(10, _matrix._m22);
    }

    store.put(12, _translation.getX());
    store.put(13, _translation.getY());
    store.put(14, _translation.getZ());
    store.put(15, 1.0);
  }

  @Override
  public void getGLApplyMatrix(final FloatBuffer store) {
    store.put(3, 0f);
    store.put(7, 0f);
    store.put(11, 0f);

    if (_rotationMatrix) {
      store.put(0, (float) (_scale.getX() * _matrix._m00));
      store.put(1, (float) (_scale.getX() * _matrix._m10));
      store.put(2, (float) (_scale.getX() * _matrix._m20));
      store.put(4, (float) (_scale.getY() * _matrix._m01));
      store.put(5, (float) (_scale.getY() * _matrix._m11));
      store.put(6, (float) (_scale.getY() * _matrix._m21));
      store.put(8, (float) (_scale.getZ() * _matrix._m02));
      store.put(9, (float) (_scale.getZ() * _matrix._m12));
      store.put(10, (float) (_scale.getZ() * _matrix._m22));
    } else {
      store.put(0, (float) _matrix._m00);
      store.put(1, (float) _matrix._m10);
      store.put(2, (float) _matrix._m20);
      store.put(4, (float) _matrix._m01);
      store.put(5, (float) _matrix._m11);
      store.put(6, (float) _matrix._m21);
      store.put(8, (float) _matrix._m02);
      store.put(9, (float) _matrix._m12);
      store.put(10, (float) _matrix._m22);
    }

    store.put(12, _translation.getXf());
    store.put(13, _translation.getYf());
    store.put(14, _translation.getZf());
    store.put(15, 1f);
  }

  /**
   * Reads in a 4x4 matrix as a 3x3 matrix and translation.
   *
   * @param matrix
   * @return this matrix for chaining.
   * @throws NullPointerException
   *           if matrix is null.
   */
  public Transform fromHomogeneousMatrix(final ReadOnlyMatrix4 matrix) {
    _matrix.set(matrix.getM00(), matrix.getM01(), matrix.getM02(), matrix.getM10(), matrix.getM11(), matrix.getM12(),
        matrix.getM20(), matrix.getM21(), matrix.getM22());
    _translation.set(matrix.getM03(), matrix.getM13(), matrix.getM23());

    updateFlags(false);
    return this;
  }

  /**
   * Check a transform... if it is null or one of its members are invalid, return false. Else return
   * true.
   *
   * @param transform
   *          the transform to check
   *
   * @return true or false as stated above.
   */
  public static boolean isValid(final ReadOnlyTransform transform) {
    if (transform == null) {
      return false;
    }
    if (!Vector3.isValid(transform.getScale()) || !Vector3.isValid(transform.getTranslation())
        || !Matrix3.isValid(transform.getMatrix())) {
      return false;
    }

    return true;
  }

  /**
   * @return the string representation of this triangle.
   */
  @Override
  public String toString() {
    return "com.ardor3d.math.Transform [\n M: " + _matrix + "\n S: " + _scale + "\n T: " + _translation + "\n]";
  }

  /**
   * @return returns a unique code for this transform object based on its values.
   */
  @Override
  public int hashCode() {
    int result = 17;

    result += 31 * result + _matrix.hashCode();
    result += 31 * result + _scale.hashCode();
    result += 31 * result + _translation.hashCode();

    return result;
  }

  /**
   * @param o
   *          the object to compare for equality
   * @return true if this transform and the provided transform have the same values.
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReadOnlyTransform)) {
      return false;
    }
    final ReadOnlyTransform comp = (ReadOnlyTransform) o;
    return _matrix.equals(comp.getMatrix())
        && Math.abs(_translation.getX() - comp.getTranslation().getX()) < Transform.ALLOWED_DEVIANCE
        && Math.abs(_translation.getY() - comp.getTranslation().getY()) < Transform.ALLOWED_DEVIANCE
        && Math.abs(_translation.getZ() - comp.getTranslation().getZ()) < Transform.ALLOWED_DEVIANCE
        && Math.abs(_scale.getX() - comp.getScale().getX()) < Transform.ALLOWED_DEVIANCE
        && Math.abs(_scale.getY() - comp.getScale().getY()) < Transform.ALLOWED_DEVIANCE
        && Math.abs(_scale.getZ() - comp.getScale().getZ()) < Transform.ALLOWED_DEVIANCE;
  }

  /**
   * @param o
   *          the object to compare for equality
   * @return true if this transform and the provided transform have the exact same double values.
   */
  @Override
  public boolean strictEquals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReadOnlyTransform)) {
      return false;
    }
    final ReadOnlyTransform comp = (ReadOnlyTransform) o;
    return _matrix.strictEquals(comp.getMatrix()) && _scale.equals(comp.getScale())
        && _translation.equals(comp.getTranslation());
  }

  // /////////////////
  // Method for Cloneable
  // /////////////////

  @Override
  public Transform clone() {
    return new Transform(this);
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends Transform> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_matrix, "rotation", (Matrix3) Matrix3.IDENTITY);
    capsule.write(_scale, "scale", (Vector3) Vector3.ONE);
    capsule.write(_translation, "translation", (Vector3) Vector3.ZERO);
    capsule.write(_identity, "identity", true);
    capsule.write(_rotationMatrix, "rotationMatrix", true);
    capsule.write(_uniformScale, "uniformScale", true);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _matrix.set(capsule.readSavable("rotation", (Matrix3) Matrix3.IDENTITY));
    _scale.set(capsule.readSavable("scale", (Vector3) Vector3.ONE));
    _translation.set(capsule.readSavable("translation", (Vector3) Vector3.ZERO));
    _identity = capsule.readBoolean("identity", true);
    _rotationMatrix = capsule.readBoolean("rotationMatrix", true);
    _uniformScale = capsule.readBoolean("uniformScale", true);
  }

  // /////////////////
  // Methods for Externalizable
  // /////////////////

  /**
   * Used with serialization. Not to be called manually.
   *
   * @param in
   *          ObjectInput
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    _matrix.set((Matrix3) in.readObject());
    _scale.set((Vector3) in.readObject());
    _translation.set((Vector3) in.readObject());
    _identity = in.readBoolean();
    _rotationMatrix = in.readBoolean();
    _uniformScale = in.readBoolean();
  }

  /*
   * Used with serialization. Not to be called manually.
   *
   * @param out ObjectOutput
   *
   * @throws IOException
   */
  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeObject(_matrix);
    out.writeObject(_scale);
    out.writeObject(_translation);
    out.writeBoolean(_identity);
    out.writeBoolean(_rotationMatrix);
    out.writeBoolean(_uniformScale);
  }

  // /////////////////
  // Methods for creating temp variables (pooling)
  // /////////////////

  /**
   * @return An instance of Transform that is intended for temporary use in calculations and so forth.
   *         Multiple calls to the method should return instances of this class that are not currently
   *         in use.
   */
  public final static Transform fetchTempInstance() {
    if (MathConstants.useMathPools) {
      return Transform.TRANS_POOL.fetch();
    } else {
      return new Transform();
    }
  }

  /**
   * Releases a Transform back to be used by a future call to fetchTempInstance. TAKE CARE: this
   * Transform object should no longer have other classes referencing it or "Bad Things" will happen.
   *
   * @param trans
   *          the Transform to release.
   */
  public final static void releaseTempInstance(final Transform trans) {
    if (MathConstants.useMathPools) {
      Transform.TRANS_POOL.release(trans);
    }
  }
}
