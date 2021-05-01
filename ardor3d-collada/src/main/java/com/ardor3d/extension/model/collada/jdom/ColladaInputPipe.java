/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Element;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.FloatBufferData;
import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.scenegraph.MeshData;

/**
 * The purpose of this class is to tie a <source> and accessor together to pull out data.
 */
public class ColladaInputPipe {
  private static final Logger logger = Logger.getLogger(ColladaInputPipe.class.getName());

  private final int _offset;
  private final int _set;
  private final Element _source;
  private int _paramCount;
  private SourceData _sourceData = null;
  private Type _type;
  private FloatBuffer _buffer;
  private int _texCoord = 0;

  public enum Type {
    VERTEX, POSITION, NORMAL, TEXCOORD, COLOR, JOINT, WEIGHT, TEXTANGENT, TEXBINORMAL, //
    INV_BIND_MATRIX, INPUT, IN_TANGENT, OUT_TANGENT, OUTPUT, INTERPOLATION, UNKNOWN
  }

  static class SourceData {
    int count;
    int stride;
    int offset;

    ParamType paramType;
    float[] floatArray;
    boolean[] boolArray;
    int[] intArray;
    String[] stringArray;

    @Override
    public String toString() {
      switch (paramType) {
        case bool_param:
          return "SourceData [boolArray=" + Arrays.toString(boolArray) + "]";
        case float_param:
          return "SourceData [floatArray=" + Arrays.toString(floatArray) + "]";
        case idref_param:
          return "SourceData [idrefArray=" + Arrays.toString(stringArray) + "]";
        case int_param:
          return "SourceData [intArray=" + Arrays.toString(intArray) + "]";
        case name_param:
          return "SourceData [nameArray=" + Arrays.toString(stringArray) + "]";
        default:
          return "Unknown paramType";
      }
    }
  }

  public enum ParamType {
    float_param, bool_param, int_param, name_param, idref_param
  }

  public ColladaInputPipe(final ColladaDOMUtil colladaDOMUtil, final Element input) {
    // Setup our type
    try {
      _type = Type.valueOf(input.getAttributeValue("semantic"));
    } catch (final Exception ex) {
      ColladaInputPipe.logger.warning("Unknown input type: " + input.getAttributeValue("semantic"));
      _type = Type.UNKNOWN;
    }

    // Locate our source
    final Element n = colladaDOMUtil.findTargetWithId(input.getAttributeValue("source"));
    if (n == null) {
      throw new ColladaException("Input source not found: " + input.getAttributeValue("source"), input);
    }

    if ("source".equals(n.getName())) {
      _source = n;
    } else if ("vertices".equals(n.getName())) {
      _source = colladaDOMUtil.getPositionSource(n);
    } else {
      throw new ColladaException("Input source not found: " + input.getAttributeValue("source"), input);
    }

    // TODO: Need to go through the params and see if they have a name set, and skip values if not when
    // parsing the array?

    _sourceData = new SourceData();
    if (_source.getChild("float_array") != null) {
      _sourceData.floatArray = colladaDOMUtil.parseFloatArray(_source.getChild("float_array"));
      _sourceData.paramType = ParamType.float_param;
    } else if (_source.getChild("bool_array") != null) {
      _sourceData.boolArray = colladaDOMUtil.parseBooleanArray(_source.getChild("bool_array"));
      _sourceData.paramType = ParamType.bool_param;
    } else if (_source.getChild("int_array") != null) {
      _sourceData.intArray = colladaDOMUtil.parseIntArray(_source.getChild("int_array"));
      _sourceData.paramType = ParamType.int_param;
    } else if (_source.getChild("Name_array") != null) {
      _sourceData.stringArray = colladaDOMUtil.parseStringArray(_source.getChild("Name_array"));
      _sourceData.paramType = ParamType.name_param;
    } else if (_source.getChild("IDREF_array") != null) {
      _sourceData.stringArray = colladaDOMUtil.parseStringArray(_source.getChild("IDREF_array"));
      _sourceData.paramType = ParamType.idref_param;
    }

    // add a hook to our params from the technique_common
    final Element accessor = getCommonAccessor(_source);
    if (accessor != null) {
      if (ColladaInputPipe.logger.isLoggable(Level.FINE)) {
        ColladaInputPipe.logger.fine("Creating buffers for: " + _source.getAttributeValue("id"));
      }

      final List<Element> params = accessor.getChildren("param");
      _paramCount = params.size();

      // Might use this info for real later, but use for testing for unsupported param skipping.
      boolean skippedParam = false;
      for (final Element param : params) {
        final String paramName = param.getAttributeValue("name");
        if (paramName == null) {
          skippedParam = true;
          break;
        }
        // String paramType = param.getAttributeValue("type");
      }
      if (_paramCount > 1 && skippedParam) {
        ColladaInputPipe.logger
            .warning("Parameter skipping not yet supported when parsing sources. " + _source.getAttributeValue("id"));
      }

      _sourceData.count = colladaDOMUtil.getAttributeIntValue(accessor, "count", 0);
      _sourceData.stride = colladaDOMUtil.getAttributeIntValue(accessor, "stride", 1);
      _sourceData.offset = colladaDOMUtil.getAttributeIntValue(accessor, "offset", 0);
    }

    // save our offset
    _offset = colladaDOMUtil.getAttributeIntValue(input, "offset", 0);
    _set = colladaDOMUtil.getAttributeIntValue(input, "set", 0);

    _texCoord = 0;
  }

  public int getOffset() { return _offset; }

  public int getSet() { return _set; }

  public Type getType() { return _type; }

  public SourceData getSourceData() { return _sourceData; }

  public void setupBuffer(final int numEntries, final MeshData meshData, final DataCache cache) {
    // use our source and the number of params to determine our buffer length
    // we'll use the params from the common technique accessor:
    final int size = _paramCount * numEntries;
    switch (_type) {
      case POSITION:
        _buffer = BufferUtils.createFloatBuffer(size);
        meshData.setVertexCoords(new FloatBufferData(_buffer, _paramCount));
        break;
      case NORMAL:
        _buffer = BufferUtils.createFloatBuffer(size);
        meshData.setNormalCoords(new FloatBufferData(_buffer, _paramCount));
        break;
      case TEXCOORD:
        _buffer = BufferUtils.createFloatBuffer(size);
        meshData.setTextureCoords(new FloatBufferData(_buffer, _paramCount), _texCoord);
        break;
      case COLOR:
        _buffer = BufferUtils.createFloatBuffer(size);
        meshData.setColorCoords(new FloatBufferData(_buffer, _paramCount));
        cache.getParsedVertexColors().put(meshData, _buffer);
        break;
      case TEXTANGENT:
        _buffer = BufferUtils.createFloatBuffer(size);
        meshData.setTangentCoords(new FloatBufferData(_buffer, _paramCount));
        break;
      // case TEXBINORMAL:
      // _buffer = BufferUtils.createFloatBuffer(size);
      // meshData.setTangentBuffer(_buffer);
      // break;
      default:
    }
  }

  void pushValues(final int memberIndex) {
    if (_buffer == null) {
      return;
    }

    if (_sourceData == null) {
      throw new ColladaException("No source data found in pipe!", _source);
    }

    if (memberIndex >= _sourceData.count) {
      ColladaInputPipe.logger.warning(
          "Accessed invalid index " + memberIndex + " on source " + _source + ".  Count: " + _sourceData.count);
      return;
    }

    int index = memberIndex * _sourceData.stride + _sourceData.offset;
    final ParamType paramType = _sourceData.paramType;
    for (int i = 0; i < _paramCount; i++) {
      if (ParamType.float_param == paramType) {
        _buffer.put(_sourceData.floatArray[index]);
      } else if (ParamType.int_param == paramType) {
        _buffer.put(_sourceData.intArray[index]);
      }
      index++;
    }
  }

  public void setTexCoord(final int texCoord) { _texCoord = texCoord; }

  private Element getCommonAccessor(final Element source) {
    final Element techniqueCommon = source.getChild("technique_common");
    if (techniqueCommon != null) {
      return techniqueCommon.getChild("accessor");
    }
    return null;
  }
}
