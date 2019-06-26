/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://github.com/Renanse/Ardor3D/blob/master/LICENSE>.
 */

package com.ardor3d.extension.ui.nuklear;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.nuklear.NkAllocator;
import org.lwjgl.nuklear.NkBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkConvertConfig;
import org.lwjgl.nuklear.NkDrawCommand;
import org.lwjgl.nuklear.NkDrawNullTexture;
import org.lwjgl.nuklear.NkDrawVertexLayoutElement;
import org.lwjgl.nuklear.NkUserFont;
import org.lwjgl.nuklear.NkUserFontGlyph;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import com.ardor3d.extension.ui.stb.StbTtfInfo;
import com.ardor3d.extension.ui.stb.StbTtfReader;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.InputState;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.BasicTriggersApplier;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.GrabbedState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseManager;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.Ardor3dException;

/**
 * Provides a way to connect Nuklear UI content into Ardor3D.
 *
 * Parts of this file are adapted from Lwjgl's Nuklear-GLFWDemo.
 */
public class NuklearHud extends Spatial implements Renderable {

	private static final Logger _logger = Logger.getLogger(NuklearHud.class.getName());

	protected final Canvas _canvas;
	protected final List<NuklearWindow> _windows = new ArrayList<>();
	protected boolean inited;

	/**
	 * The logical layer used by this UI to receive input events.
	 */
	protected final LogicalLayer _logicalLayer = new LogicalLayer();

	/**
	 * Internal flag indicating whether the last input event was consumed by the UI.
	 * This is used to decide if we will forward the event to the next LogicalLayer.
	 */
	protected boolean _inputConsumed;

	/**
	 * Flag used to determine if we should use mouse input when mouse is grabbed.
	 * Defaults to true.
	 */
	private boolean _ignoreMouseInputOnGrabbed = true;

	/**
	 * An optional mouseManager, required in order to test mouse is grabbed.
	 */
	private MouseManager _mouseManager;

	private InputPostHook _postKeyboardHook;

	private InputPostHook _postMouseHook;

	// NUKLEAR FIELDS
	protected final NkContext _ctx = NkContext.create();

	protected static final int BUFFER_INITIAL_SIZE = 4 * 1024;

	protected static final int MAX_VERTEX_BUFFER = 512 * 1024;
	protected static final int MAX_ELEMENT_BUFFER = 128 * 1024;

	protected static final NkAllocator ALLOCATOR;

	protected static final NkDrawVertexLayoutElement.Buffer VERTEX_LAYOUT;

	static {
		ALLOCATOR = NkAllocator.create() //
				.alloc((handle, old, size) -> nmemAllocChecked(size)) //
				.mfree((handle, ptr) -> nmemFree(ptr));

		VERTEX_LAYOUT = NkDrawVertexLayoutElement.create(4) //
				.position(0).attribute(NK_VERTEX_POSITION).format(NK_FORMAT_FLOAT).offset(0) //
				.position(1).attribute(NK_VERTEX_TEXCOORD).format(NK_FORMAT_FLOAT).offset(8) //
				.position(2).attribute(NK_VERTEX_COLOR).format(NK_FORMAT_R8G8B8A8).offset(16) //
				.position(3).attribute(NK_VERTEX_ATTRIBUTE_COUNT).format(NK_FORMAT_COUNT).offset(0) //
				.flip();
	}

	protected final NkUserFont default_font = NkUserFont.create();

	protected final NkBuffer cmds = NkBuffer.create();
	protected final NkDrawNullTexture null_texture = NkDrawNullTexture.create();

	protected int vbo, vao, ebo;
	protected int prog;
	protected int vert_shdr;
	protected int frag_shdr;
	protected int uniform_tex;
	protected int uniform_proj;

	public NuklearHud(final Canvas canvas) {
		setName("UIHud");
		_canvas = canvas;

		setRenderMaterial("ui/textured/vertex_color.yaml");

		getSceneHints().setCullHint(CullHint.Never);
		getSceneHints().setRenderBucketType(RenderBucketType.PostBucket);
		getSceneHints().setLightCombineMode(LightCombineMode.Off);
		getSceneHints().setTextureCombineMode(TextureCombineMode.Off);

		setupLogicalLayer();
	}

	public void add(final NuklearWindow window) {
		_windows.add(window);
	}

	@Override
	public void updateWorldBound(final boolean recurse) {
		; // nothing to do here.
	}

	/**
	 * Set up our logical layer with a trigger that hands input to the UI and saves
	 * whether it was "consumed".
	 */
	protected void setupLogicalLayer() {
		_logicalLayer.registerTrigger(new InputTrigger(new Predicate<TwoInputStates>() {
			@Override
			public boolean test(final TwoInputStates arg0) {
				// always trigger this.
				return true;
			}
		}, new TriggerAction() {
			@Override
			public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
				nk_input_begin(_ctx);
				offerMouseInputToUI(inputStates);
				offerKeyInputToUI(inputStates);
				_inputConsumed = nk_item_is_any_active(_ctx);
				nk_input_end(_ctx);
			}
		}));
	}

	protected void offerKeyInputToUI(final TwoInputStates inputStates) {
//		final InputState current = inputStates.getCurrent();

		// Keyboard checks
		{
//			final KeyboardState previousKState = inputStates.getPrevious().getKeyboardState();
//			final KeyboardState currentKState = current.getKeyboardState();
//			if (!currentKState.getKeysDown().isEmpty()) {
//				// new presses
//				final EnumSet<Key> pressed = currentKState.getKeysPressedSince(previousKState);
//				if (!pressed.isEmpty()) {
//					for (final Key key : pressed) {
//						consumed |= nk_(key, current);
//					}
//				}
//
//				// repeats
//				final EnumSet<Key> repeats = currentKState.getKeysHeldSince(previousKState);
//				if (!repeats.isEmpty() && _focusedComponent != null) {
//					for (final Key key : repeats) {
//						consumed |= fireKeyHeldEvent(key, current);
//					}
//				}
//			}
//
//			// key releases
//			if (!previousKState.getKeysDown().isEmpty()) {
//				final EnumSet<Key> released = currentKState.getKeysReleasedSince(previousKState);
//				if (!released.isEmpty()) {
//					for (final Key key : released) {
//						consumed |= fireKeyReleasedEvent(key, current);
//					}
//				}
//			}
		}

		// Post hook, if we have one
		{
			if (_postKeyboardHook != null) {
				_postKeyboardHook.process(this, inputStates);
			}
		}
	}

	protected void offerMouseInputToUI(final TwoInputStates inputStates) {
		final InputState current = inputStates.getCurrent();

		// Mouse checks.
		if (!isIgnoreMouseInputOnGrabbed() || _mouseManager == null
				|| _mouseManager.getGrabbed() != GrabbedState.GRABBED) {
			final MouseState previousMState = inputStates.getPrevious().getMouseState();
			final MouseState currentMState = current.getMouseState();
			final int x = currentMState.getX();
			final int y = _canvas.getContentHeight() - currentMState.getY();
			if (previousMState != currentMState) {
				// Check for presses.
				if (currentMState.hasButtonState(ButtonState.DOWN)) {
					final EnumSet<MouseButton> pressed = currentMState.getButtonsPressedSince(previousMState);
					if (!pressed.isEmpty()) {
						for (final MouseButton button : pressed) {
							int nkButton;
							switch (button) {
							case MIDDLE:
								nkButton = NK_BUTTON_MIDDLE;
								break;
							case RIGHT:
								nkButton = NK_BUTTON_RIGHT;
								break;
							default:
								nkButton = NK_BUTTON_LEFT;
								break;
							}
							nk_input_button(_ctx, nkButton, x, y, true);
						}
					}
				}

				// Check for releases.
				if (previousMState.hasButtonState(ButtonState.DOWN)) {
					final EnumSet<MouseButton> released = currentMState.getButtonsReleasedSince(previousMState);
					if (!released.isEmpty()) {
						for (final MouseButton button : released) {
							int nkButton;
							switch (button) {
							case MIDDLE:
								nkButton = NK_BUTTON_MIDDLE;
								break;
							case RIGHT:
								nkButton = NK_BUTTON_RIGHT;
								break;
							default:
								nkButton = NK_BUTTON_LEFT;
								break;
							}
							nk_input_button(_ctx, nkButton, x, y, false);
						}
					}
				}

				// Check for mouse movement
				if (currentMState.getDx() != 0 || currentMState.getDy() != 0) {
					nk_input_motion(_ctx, x, y);
				}

				// Check for wheel change
				if (currentMState.getDwheel() != 0) {
					try (MemoryStack stack = stackPush()) {
						final NkVec2 scroll = NkVec2.mallocStack(stack).y(currentMState.getDwheel());
						nk_input_scroll(_ctx, scroll);
					}
				}
			}

			// Post hook, if we have one
			{
				if (_postMouseHook != null) {
					_postMouseHook.process(this, inputStates);
				}
			}
		}
	}

	@Override
	public void draw(final Renderer r) {
		if (!r.isProcessingQueue()) {
			if (r.checkAndAdd(this)) {
				return;
			}
		}

		r.draw((Renderable) this);
	}

	@Override
	public boolean render(final Renderer renderer) {
		// check if we have any opengl stuff to call
		setupContext();

		// Ask our windows to submit their draw calls
		for (final NuklearWindow window : _windows) {
			window.layout(_ctx);
		}

		// Draw the calls
		render(NK_ANTI_ALIASING_ON, MAX_VERTEX_BUFFER, MAX_ELEMENT_BUFFER);

		// Reset tracking on things we touched directly
		fixStateTracking();

		return true;
	}

	private void fixStateTracking() {
		final RenderContext rCtx = ContextManager.getCurrentContext();
		rCtx.getRendererRecord().invalidate();
		rCtx.getStateRecord(StateType.Blend).invalidate();
		rCtx.getStateRecord(StateType.Cull).invalidate();
		rCtx.getStateRecord(StateType.ZBuffer).invalidate();
		rCtx.getStateRecord(StateType.Texture).invalidate();
	}

	private void setupContext() {
		if (inited) {
			return;
		}

		if (!nk_init(_ctx, ALLOCATOR, null)) {
			throw new IllegalStateException();
		}
		inited = true;

		setupDefaultFont();

		final String NK_SHADER_VERSION = Platform.get() == Platform.MACOSX ? "#version 150\n" : "#version 300 es\n";
		final String vertex_shader = //
				NK_SHADER_VERSION + //
						"uniform mat4 ProjMtx;\n" + //
						"in vec2 Position;\n" + //
						"in vec2 TexCoord;\n" + //
						"in vec4 Color;\n" + //
						"out vec2 Frag_UV;\n" + //
						"out vec4 Frag_Color;\n" + //
						"void main() {\n" + //
						"   Frag_UV = TexCoord;\n" + //
						"   Frag_Color = Color;\n" + //
						"   gl_Position = ProjMtx * vec4(Position.xy, 0, 1);\n" + //
						"}\n";

		final String fragment_shader = //
				NK_SHADER_VERSION + //
						"precision mediump float;\n" + //
						"uniform sampler2D Texture;\n" + //
						"in vec2 Frag_UV;\n" + //
						"in vec4 Frag_Color;\n" + //
						"out vec4 Out_Color;\n" + //
						"void main(){\n" + //
						"   Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" + //
						"}\n";

		nk_buffer_init(cmds, ALLOCATOR, BUFFER_INITIAL_SIZE);
		prog = GL20C.glCreateProgram();
		vert_shdr = GL20C.glCreateShader(GL20C.GL_VERTEX_SHADER);
		frag_shdr = GL20C.glCreateShader(GL20C.GL_FRAGMENT_SHADER);
		GL20C.glShaderSource(vert_shdr, vertex_shader);
		GL20C.glShaderSource(frag_shdr, fragment_shader);
		GL20C.glCompileShader(vert_shdr);
		GL20C.glCompileShader(frag_shdr);
		if (GL20C.glGetShaderi(vert_shdr, GL20C.GL_COMPILE_STATUS) != GL11C.GL_TRUE) {
			final String log = GL20C.glGetShaderInfoLog(vert_shdr);
			_logger.log(Level.SEVERE, "Error Compiling Vertex Shader for NuklearHud: " + log);
			throw new Ardor3dException("Error compiling vertex shader: " + log);
		}
		if (GL20C.glGetShaderi(frag_shdr, GL20C.GL_COMPILE_STATUS) != GL11C.GL_TRUE) {
			final String log = GL20C.glGetShaderInfoLog(frag_shdr);
			_logger.log(Level.SEVERE, "Error Compiling Fragment Shader for NuklearHud: " + log);
			throw new Ardor3dException("Error compiling fragment shader: " + log);
		}
		GL20C.glAttachShader(prog, vert_shdr);
		GL20C.glAttachShader(prog, frag_shdr);
		GL20C.glLinkProgram(prog);
		if (GL20C.glGetProgrami(prog, GL20C.GL_LINK_STATUS) != GL11C.GL_TRUE) {
			throw new IllegalStateException();
		}

		uniform_tex = GL20C.glGetUniformLocation(prog, "Texture");
		uniform_proj = GL20C.glGetUniformLocation(prog, "ProjMtx");
		final int attrib_pos = GL20C.glGetAttribLocation(prog, "Position");
		final int attrib_uv = GL20C.glGetAttribLocation(prog, "TexCoord");
		final int attrib_col = GL20C.glGetAttribLocation(prog, "Color");

		{
			// buffer setup
			vbo = GL15C.glGenBuffers();
			ebo = GL15C.glGenBuffers();
			vao = GL30C.glGenVertexArrays();

			GL30C.glBindVertexArray(vao);
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
			GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ebo);

			GL20C.glEnableVertexAttribArray(attrib_pos);
			GL20C.glEnableVertexAttribArray(attrib_uv);
			GL20C.glEnableVertexAttribArray(attrib_col);

			GL20C.glVertexAttribPointer(attrib_pos, 2, GL11C.GL_FLOAT, false, 20, 0);
			GL20C.glVertexAttribPointer(attrib_uv, 2, GL11C.GL_FLOAT, false, 20, 8);
			GL20C.glVertexAttribPointer(attrib_col, 4, GL11C.GL_UNSIGNED_BYTE, true, 20, 16);
		}

		{
			// null texture setup
			final int nullTexID = GL11C.glGenTextures();

			null_texture.texture().id(nullTexID);
			null_texture.uv().set(0.5f, 0.5f);

			GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, nullTexID);
			try (MemoryStack stack = stackPush()) {
				GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, 1, 1, 0, GL11C.GL_RGBA,
						GL12C.GL_UNSIGNED_INT_8_8_8_8_REV, stack.ints(0xFFFFFFFF));
			}
			GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
			GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
		}

		GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL30C.glBindVertexArray(0);
	}

	private void render(final int AA, final int max_vertex_buffer, final int max_element_buffer) {

		final int width = _canvas.getContentWidth();
		final int height = _canvas.getContentHeight();

		try (MemoryStack stack = stackPush()) {
			// setup global state
			GL11C.glEnable(GL11C.GL_BLEND);
			GL14C.glBlendEquation(GL14C.GL_FUNC_ADD);
			GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA);
			GL11C.glDisable(GL11C.GL_CULL_FACE);
			GL11C.glDisable(GL11C.GL_DEPTH_TEST);
			GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
			GL13C.glActiveTexture(GL13C.GL_TEXTURE0);

			// setup program
			GL20C.glUseProgram(prog);
			GL20C.glUniform1i(uniform_tex, 0);
			GL20C.glUniformMatrix4fv(uniform_proj, false, stack.floats(2.0f / width, 0.0f, 0.0f, 0.0f, 0.0f,
					-2.0f / height, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f));
		}

		{
			// convert from command queue into draw list and draw to screen

			// allocate vertex and element buffer
			GL30C.glBindVertexArray(vao);
			GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
			GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ebo);

			GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, max_vertex_buffer, GL15C.GL_STREAM_DRAW);
			GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, max_element_buffer, GL15C.GL_STREAM_DRAW);

			// load draw vertices & elements directly into vertex + element buffer
			final ByteBuffer vertices = Objects.requireNonNull(
					GL15C.glMapBuffer(GL15C.GL_ARRAY_BUFFER, GL15C.GL_WRITE_ONLY, max_vertex_buffer, null));
			final ByteBuffer elements = Objects.requireNonNull(
					GL15C.glMapBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, GL15C.GL_WRITE_ONLY, max_element_buffer, null));
			try (MemoryStack stack = stackPush()) {
				// fill convert configuration
				final NkConvertConfig config = NkConvertConfig.callocStack(stack) //
						.vertex_layout(VERTEX_LAYOUT) //
						.vertex_size(20) //
						.vertex_alignment(4) //
						.null_texture(null_texture) //
						.circle_segment_count(22) //
						.curve_segment_count(22) //
						.arc_segment_count(22) //
						.global_alpha(1.0f) //
						.shape_AA(AA) //
						.line_AA(AA);

				// setup buffers to load vertices and elements
				final NkBuffer vbuf = NkBuffer.mallocStack(stack);
				final NkBuffer ebuf = NkBuffer.mallocStack(stack);

				nk_buffer_init_fixed(vbuf, vertices/* , max_vertex_buffer */);
				nk_buffer_init_fixed(ebuf, elements/* , max_element_buffer */);
				nk_convert(_ctx, cmds, vbuf, ebuf, config);
			}
			GL15C.glUnmapBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER);
			GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);

			// iterate over and execute each draw command
			long offset = NULL;
			for (NkDrawCommand cmd = nk__draw_begin(_ctx, cmds); cmd != null; cmd = nk__draw_next(cmd, cmds, _ctx)) {
				if (cmd.elem_count() == 0) {
					continue;
				}
				GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, cmd.texture().id());
				GL11C.glScissor( //
						(int) cmd.clip_rect().x(), //
						height - (int) (cmd.clip_rect().y() + cmd.clip_rect().h()), //
						(int) cmd.clip_rect().w(), //
						(int) cmd.clip_rect().h());
				GL11C.glDrawElements(GL11C.GL_TRIANGLES, cmd.elem_count(), GL11C.GL_UNSIGNED_SHORT, offset);
				offset += cmd.elem_count() * 2;
			}
			nk_clear(_ctx);
		}

		// default OpenGL state
		GL20C.glUseProgram(0);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
		GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL30C.glBindVertexArray(0);
		GL11C.glDisable(GL11C.GL_BLEND);
		GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
	}

	private void setupDefaultFont() {
		final int fontHeight = 18;
		final int texWidth = 1024;
		final int textHeight = 1024;

		final StbTtfInfo font = StbTtfReader.createFont("FiraSans-Regular.ttf", fontHeight, texWidth, textHeight, 32,
				95);

		default_font.width((handle, h, text, len) -> {
			float text_width = 0;
			try (MemoryStack stack = stackPush()) {
				final IntBuffer unicode = stack.mallocInt(1);

				int glyph_len = nnk_utf_decode(text, memAddress(unicode), len);
				int text_len = glyph_len;

				if (glyph_len == 0) {
					return 0;
				}

				final IntBuffer advance = stack.mallocInt(1);
				while (text_len <= len && glyph_len != 0) {
					if (unicode.get(0) == NK_UTF_INVALID) {
						break;
					}

					/* query currently drawn glyph information */
					stbtt_GetCodepointHMetrics(font.info, unicode.get(0), advance, null);
					text_width += advance.get(0) * font.scale;

					/* offset next glyph */
					glyph_len = nnk_utf_decode(text + text_len, memAddress(unicode), len - text_len);
					text_len += glyph_len;
				}
			}
			return text_width;
		}).height(fontHeight).query((handle, font_height, glyph, codepoint, next_codepoint) -> {
			try (MemoryStack stack = stackPush()) {
				final FloatBuffer x = stack.floats(0.0f);
				final FloatBuffer y = stack.floats(0.0f);

				final STBTTAlignedQuad q = STBTTAlignedQuad.mallocStack(stack);
				final IntBuffer advance = stack.mallocInt(1);

				stbtt_GetPackedQuad(font.charData, texWidth, textHeight, codepoint - 32, x, y, q, false);
				stbtt_GetCodepointHMetrics(font.info, codepoint, advance, null);

				final NkUserFontGlyph ufg = NkUserFontGlyph.create(glyph);

				ufg.width(q.x1() - q.x0());
				ufg.height(q.y1() - q.y0());
				ufg.offset().set(q.x0(), q.y0() + (fontHeight + font.descent));
				ufg.xadvance(advance.get(0) * font.scale);
				ufg.uv(0).set(q.s0(), q.t0());
				ufg.uv(1).set(q.s1(), q.t1());
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}).texture(it -> it.id(font.textureId));

		nk_style_set_font(_ctx, default_font);
	}

	/**
	 * @return the logical layer associated with this hud. When chaining UI logic to
	 *         game logic, this LogicalLayer is the one to call checkTriggers on.
	 */
	public LogicalLayer getLogicalLayer() {
		return _logicalLayer;
	}

	/**
	 * @return true if we should ignore (only forward) mouse input when the mouse is
	 *         set to grabbed. If true, requires mouse manager to be set or this
	 *         param is ignored.
	 */
	public boolean isIgnoreMouseInputOnGrabbed() {
		return _ignoreMouseInputOnGrabbed;
	}

	/**
	 * @param mouseInputOnGrabbed true if we should ignore (only forward) mouse
	 *                            input when the mouse is set to grabbed. If true,
	 *                            requires mouse manager to be set or this param is
	 *                            ignored.
	 * @see #setMouseManager(MouseManager)
	 */
	public void setIgnoreMouseInputOnGrabbed(final boolean mouseInputOnGrabbed) {
		_ignoreMouseInputOnGrabbed = mouseInputOnGrabbed;
	}

	/**
	 * @return a MouseManager used to test if mouse is grabbed, or null if none was
	 *         set.
	 */
	public MouseManager getMouseManager() {
		return _mouseManager;
	}

	/**
	 * @param manager a MouseManager used to test if mouse is grabbed.
	 */
	public void setMouseManager(final MouseManager manager) {
		_mouseManager = manager;
	}

	/**
	 * Convenience method for setting up the UI's connection to the Ardor3D input
	 * system, along with a forwarding address for input events that the UI does not
	 * care about.
	 *
	 * @param canvas        the canvas to register with
	 * @param physicalLayer the physical layer to register with
	 * @param forwardTo     a LogicalLayer to send unconsumed (by the UI) input
	 *                      events to.
	 */
	public void setupInput(final PhysicalLayer physicalLayer, final LogicalLayer forwardTo) {
		// Set up this logical layer to listen for events from the given canvas and
		// PhysicalLayer
		_logicalLayer.registerInput(_canvas, physicalLayer);

		// Set up forwarding for events not consumed.
		if (forwardTo != null) {
			_logicalLayer.setApplier(new BasicTriggersApplier() {

				@Override
				public void checkAndPerformTriggers(final Set<InputTrigger> triggers, final Canvas source,
						final TwoInputStates states, final double tpf) {
					super.checkAndPerformTriggers(triggers, source, states, tpf);
					if (!_inputConsumed) {
						// nothing consumed
						forwardTo.getApplier().checkAndPerformTriggers(forwardTo.getTriggers(), source, states, tpf);
					} else {
						// both consumed, do nothing.
					}

					_inputConsumed = false;
				}
			});
		}
	}

	public interface InputPostHook {
		boolean process(NuklearHud source, TwoInputStates inputStates);
	}

}
