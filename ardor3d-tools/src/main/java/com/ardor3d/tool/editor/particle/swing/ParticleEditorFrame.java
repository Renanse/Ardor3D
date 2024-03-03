/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.tool.editor.particle.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.lwjgl.opengl.awt.GLData;

import com.ardor3d.extension.effect.EffectUtils;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.extension.effect.particle.SimpleParticleInfluenceFactory;
import com.ardor3d.extension.effect.particle.SwarmInfluence;
import com.ardor3d.extension.effect.particle.WanderInfluence;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.framework.lwjgl3.awt.Lwjgl3AwtCanvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.image.util.awt.AwtColorUtil;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.awt.AwtFocusWrapper;
import com.ardor3d.input.awt.AwtKeyboardWrapper;
import com.ardor3d.input.awt.AwtMouseManager;
import com.ardor3d.input.awt.AwtMouseWrapper;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.renderer.material.reader.YamlMaterialReader;
import com.ardor3d.renderer.material.uniform.AlphaTestConsts;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.ComplexSpatialController.RepeatType;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.tool.editor.particle.swing.panel.ParticleAppearancePanel;
import com.ardor3d.tool.editor.particle.swing.panel.ParticleEmissionPanel;
import com.ardor3d.tool.editor.particle.swing.panel.ParticleFlowPanel;
import com.ardor3d.tool.editor.particle.swing.panel.ParticleInfluencePanel;
import com.ardor3d.tool.editor.particle.swing.panel.ParticleWorldPanel;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.export.binary.BinaryImporter;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

public class ParticleEditorFrame extends JFrame {
  private static final Logger logger = Logger.getLogger(ParticleEditorFrame.class.getName());

  public static final Object GLOBAL_CONTEXT = new Object();

  @Serial
  private static final long serialVersionUID = 1L;
  private static final String[] EXAMPLE_NAMES =
      {"Fire", "Fountain", "Lava", "Smoke", "Jet", "Snow", "Rain", "Explosion", "Ground Fog", "Fireflies"};
  int width = 640, height = 480;

  private ParticleEditorScene editScene;
  private CameraHandler camhand;

  private Action spawnAction;

  // edit panels
  private ParticleAppearancePanel appearancePanel;
  private ParticleFlowPanel flowPanel;
  private ParticleEmissionPanel emissionPanel;
  private ParticleWorldPanel scenePanel;
  private ParticleInfluencePanel influencePanel;

  // layer panel components
  private final LayerTableModel layerModel = new LayerTableModel();
  private final JTable layerTable = new JTable(layerModel);
  private JButton newLayerButton;
  private JButton deleteLayerButton;

  // examples panel components
  private JList<String> exampleList;
  private JButton exampleButton;

  private final JFileChooser fileChooser = new JFileChooser();
  private File openFile;

  private final Preferences prefs = Preferences.userNodeForPackage(ParticleEditorFrame.class);

  private JCheckBoxMenuItem yUp;

  private JCheckBoxMenuItem zUp;

  private Lwjgl3AwtCanvas _canvas;

  public ParticleEditorFrame() {
    try {
      AWTImageLoader.registerLoader();

      addDefaultResourceLocators();
      EffectUtils.addDefaultResourceLocators();

      init();

      // center the frame
      setLocationRelativeTo(null);

      // show frame
      setVisible(true);

      // init some location dependent sub frames
      initFileChooser();

    } catch (final Exception ex) {
      logger.logp(Level.SEVERE, this.getClass().toString(), "ParticleEditorFrame()", "Exception", ex);
    }
  }

  private void init() throws Exception {
    updateTitle();
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setFont(new Font("Arial", 0, 12));

    setJMenuBar(createMenuBar());

    appearancePanel = new ParticleAppearancePanel(prefs) {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      protected void requestParticleSystemOverwrite(final ParticleSystem newParticles) {
        editScene.particleNode.getChildren().set(editScene.particleNode.getChildren().indexOf(editScene.particleGeom),
            newParticles);
        editScene.particleGeom = newParticles;
        updateFromManager();
      }
    };
    flowPanel = new ParticleFlowPanel();
    emissionPanel = new ParticleEmissionPanel();
    scenePanel = new ParticleWorldPanel();
    influencePanel = new ParticleInfluencePanel();

    final JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.add(new JScrollPane(appearancePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), "Appearance");
    tabbedPane.add(new JScrollPane(emissionPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), "Emission");
    tabbedPane.add(new JScrollPane(flowPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), "Flow");
    tabbedPane.add(new JScrollPane(scenePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), "Scene");
    tabbedPane.add(new JScrollPane(influencePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), "Influences");
    tabbedPane.add(new JScrollPane(createExamplesPanel(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), "Examples");
    tabbedPane.setPreferredSize(new Dimension(300, 150));

    final JPanel canvasPanel = new JPanel();
    canvasPanel.setLayout(new BorderLayout());
    _canvas = createParticleCanvas();
    canvasPanel.add(_canvas, BorderLayout.CENTER);

    final Dimension minimumSize = new Dimension(150, 150);
    tabbedPane.setMinimumSize(minimumSize);
    canvasPanel.setMinimumSize(minimumSize);

    final JSplitPane sideSplit = new JSplitPane();
    sideSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    sideSplit.setTopComponent(createLayerPanel());
    sideSplit.setBottomComponent(tabbedPane);
    sideSplit.setDividerLocation(150);

    final JSplitPane mainSplit = new JSplitPane();
    mainSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    mainSplit.setLeftComponent(sideSplit);
    mainSplit.setRightComponent(canvasPanel);
    mainSplit.setDividerLocation(300);
    getContentPane().add(mainSplit, BorderLayout.CENTER);

    yUp.addActionListener(e -> {
      final Callable<Void> exe = () -> {
        camhand.worldUpVector.set(Vector3.UNIT_Y);
        final Camera cam = Camera.getCurrentCamera();
        cam.setLocation(new Vector3(0, 850, -850));
        camhand.recenterCamera();
        editScene.grid.setRotation(new Matrix3().fromAngleAxis(0, Vector3.UNIT_X));
        prefs.putBoolean("yUp", true);
        return null;
      };
      GameTaskQueueManager.getManager(ParticleEditorFrame.GLOBAL_CONTEXT).render(exe);
    });
    zUp.addActionListener(e -> {
      final Callable<Void> exe = () -> {
        camhand.worldUpVector.set(Vector3.UNIT_Z);
        final Camera cam = Camera.getCurrentCamera();
        cam.setLocation(new Vector3(0, -850, 850));
        camhand.recenterCamera();
        editScene.grid.setRotation(new Matrix3().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_X));
        prefs.putBoolean("yUp", false);
        return null;
      };
      GameTaskQueueManager.getManager(ParticleEditorFrame.GLOBAL_CONTEXT).render(exe);
    });

    final Callable<Void> exe = () -> {
      if (prefs.getBoolean("yUp", true)) {
        yUp.doClick();
      } else {
        zUp.doClick();
      }
      return null;
    };
    GameTaskQueueManager.getManager(ParticleEditorFrame.GLOBAL_CONTEXT).update(exe);

    setSize(new Dimension(1024, 768));

    newLayerButton.doClick();
  }

  private void updateTitle() {
    setTitle("Ardor3D Particle System Editor" + (openFile == null ? "" : " - " + openFile));
  }

  private JMenuBar createMenuBar() {
    final Action newAction = new AbstractAction("New") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        createNewSystem();
      }
    };
    newAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);

    final Action open = new AbstractAction("Open...") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        showOpenDialog();
      }
    };
    open.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);

    final Action importAction = new AbstractAction("Merge Layers...") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        showMergeDialog();
      }
    };
    importAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);

    final AbstractAction save = new AbstractAction("Save") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        saveAs(openFile);
      }
    };
    save.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);

    final Action saveAs = new AbstractAction("Save As...") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        saveAs(null);
      }
    };
    saveAs.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);

    final Action quit = new AbstractAction("Quit") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        System.exit(0);
      }
    };
    quit.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Q);

    final JMenu file = new JMenu("File");
    file.setMnemonic(KeyEvent.VK_F);
    file.add(newAction);
    file.add(open);
    file.add(importAction);
    file.add(save);
    file.add(saveAs);
    file.addSeparator();
    file.add(quit);

    spawnAction = new AbstractAction("Force Spawn") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        for (final Spatial child : editScene.particleNode.getChildren()) {
          if (child instanceof ParticleSystem) {
            ((ParticleSystem) child).forceRespawn();
          }
        }
      }
    };
    spawnAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
    spawnAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));

    final JMenu edit = new JMenu("Edit");
    edit.setMnemonic(KeyEvent.VK_E);
    edit.add(spawnAction);

    final Action showGrid = new AbstractAction("Show Grid") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        editScene.grid.getSceneHints().setCullHint(
            editScene.grid.getSceneHints().getCullHint() == CullHint.Always ? CullHint.Dynamic : CullHint.Always);
        prefs.putBoolean("showgrid", editScene.grid.getSceneHints().getCullHint() != CullHint.Always);
      }
    };
    showGrid.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_G);

    final Action changeBackground = new AbstractAction("Change Background Color...") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        showBackgroundDialog();
      }
    };
    changeBackground.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_B);

    final Action recenter = new AbstractAction("Recenter Camera") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        camhand.recenterCamera();
      }
    };
    recenter.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);

    yUp = new JCheckBoxMenuItem("Y-Up Camera");
    yUp.setMnemonic(KeyEvent.VK_Y);
    zUp = new JCheckBoxMenuItem("Z-Up Camera");
    zUp.setMnemonic(KeyEvent.VK_Y);
    final ButtonGroup upGroup = new ButtonGroup();
    upGroup.add(yUp);
    upGroup.add(zUp);

    final JMenu view = new JMenu("View");
    view.setMnemonic(KeyEvent.VK_V);
    final JCheckBoxMenuItem sgitem = new JCheckBoxMenuItem(showGrid);
    sgitem.setSelected(prefs.getBoolean("showgrid", true));
    view.add(sgitem);
    view.add(changeBackground);
    view.addSeparator();
    view.add(recenter);
    view.add(yUp);
    view.add(zUp);

    final JMenuBar mbar = new JMenuBar();
    mbar.add(file);
    mbar.add(edit);
    mbar.add(view);
    return mbar;
  }

  private JPanel createLayerPanel() {
    final JLabel layerLabel = createBoldLabel("Particle Layers:");

    layerTable.setColumnSelectionAllowed(false);
    layerTable.setRowSelectionAllowed(true);
    layerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final int vwidth = layerTable.getTableHeader().getDefaultRenderer()
        .getTableCellRendererComponent(layerTable, "Visible", false, false, -1, 1).getMinimumSize().width;
    final TableColumn vcol = layerTable.getColumnModel().getColumn(1);
    vcol.setMinWidth(vwidth);
    vcol.setPreferredWidth(vwidth);
    vcol.setMaxWidth(vwidth);
    layerTable.getSelectionModel().addListSelectionListener(e -> {
      if (layerTable.getSelectedRow() != -1) {
        editScene.particleGeom = (ParticleSystem) editScene.particleNode.getChild(layerTable.getSelectedRow());
        updateFromManager();
      }
    });

    newLayerButton = new JButton(new AbstractAction("New") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        final int idx = editScene.particleNode.getNumberOfChildren();
        createNewLayer();
        layerModel.fireTableRowsInserted(idx, idx);
        layerTable.setRowSelectionInterval(idx, idx);
        deleteLayerButton.setEnabled(true);
      }
    });
    newLayerButton.setMargin(new Insets(2, 14, 2, 14));

    deleteLayerButton = new JButton(new AbstractAction("Delete") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        deleteLayer();
      }
    });
    deleteLayerButton.setMargin(new Insets(2, 14, 2, 14));
    deleteLayerButton.setEnabled(false);

    final JPanel layerPanel = new JPanel(new GridBagLayout());
    layerPanel.add(layerLabel, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 5, 10), 0, 0));
    layerPanel.add(new JScrollPane(layerTable), new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER,
        GridBagConstraints.BOTH, new Insets(0, 10, 0, 10), 0, 0));
    layerPanel.add(newLayerButton, new GridBagConstraints(0, 2, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(5, 10, 10, 10), 0, 0));
    layerPanel.add(deleteLayerButton, new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(5, 10, 10, 10), 0, 0));
    return layerPanel;
  }

  private JPanel createExamplesPanel() {
    final JLabel examplesLabel = createBoldLabel("Prebuilt Examples:");

    exampleList = new JList<>(EXAMPLE_NAMES);
    exampleList.addListSelectionListener(e -> {
      if (exampleList.getSelectedValue() instanceof String) {
        exampleButton.setEnabled(true);
      } else {
        exampleButton.setEnabled(false);
      }
    });

    exampleButton = new JButton(new AbstractAction("Apply") {
      @Serial
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        final Callable<Void> exe = () -> {
          applyExample();
          return null;
        };
        GameTaskQueueManager.getManager(ParticleEditorFrame.GLOBAL_CONTEXT).update(exe);
      }
    });
    exampleButton.setMargin(new Insets(2, 14, 2, 14));
    exampleButton.setEnabled(false);

    final JPanel examplesPanel = new JPanel(new GridBagLayout());
    examplesPanel.add(examplesLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 5, 10), 0, 0));
    examplesPanel.add(new JScrollPane(exampleList), new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 0, 10), 0, 0));
    examplesPanel.add(exampleButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(5, 10, 10, 10), 0, 0));
    return examplesPanel;
  }

  private JLabel createBoldLabel(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(new Font("Arial", Font.BOLD, 13));
    return label;
  }

  private void createNewSystem() {
    layerTable.clearSelection();
    editScene.particleNode.detachAllChildren();
    createNewLayer();
    layerModel.fireTableDataChanged();
    layerTable.setRowSelectionInterval(0, 0);
    deleteLayerButton.setEnabled(false);
    openFile = null;
    updateTitle();
  }

  private void showOpenDialog() {
    fileChooser.setSelectedFile(new File(""));
    if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    final File file = fileChooser.getSelectedFile();
    prefs.put("particle_dir", file.getParent().toString());
    SimpleResourceLocator locator = null;
    try {
      locator = new SimpleResourceLocator(file.getParentFile().toURI());
      ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, locator);
    } catch (final URISyntaxException ex) {
      ex.printStackTrace();
    }
    try {
      final Spatial obj = (Spatial) new BinaryImporter().load(file);
      if (obj instanceof Node && !(obj instanceof ParticleSystem)) {
        final Node node = (Node) obj;
        for (int ii = node.getNumberOfChildren() - 1; ii >= 0; ii--) {
          if (!(node.getChild(ii) instanceof ParticleSystem)) {
            node.detachChildAt(ii);
          }
        }
        if (node.getNumberOfChildren() == 0) {
          throw new Exception("Node contains no particle meshes");
        }
        layerTable.clearSelection();
        editScene.rootNode.detachChild(editScene.particleNode);
        editScene.particleNode = node;
        editScene.rootNode.attachChild(editScene.particleNode);
        deleteLayerButton.setEnabled(true);

      } else { // obj instanceof ParticleSystem
        editScene.particleGeom = (ParticleSystem) obj;
        layerTable.clearSelection();
        editScene.particleNode.detachAllChildren();
        editScene.particleNode.attachChild(editScene.particleGeom);
        deleteLayerButton.setEnabled(false);
      }
      layerModel.fireTableDataChanged();
      layerTable.setRowSelectionInterval(0, 0);
      openFile = file;
      updateTitle();

    } catch (final Exception e) {
      JOptionPane.showMessageDialog(this, "Couldn't open '" + file + "': " + e, "File Error",
          JOptionPane.ERROR_MESSAGE);
      logger.log(Level.WARNING, "Couldn't open '" + file, e);
    } finally {
      if (locator != null) {
        ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, locator);
      }
    }
  }

  private void showMergeDialog() {
    fileChooser.setSelectedFile(new File(""));
    if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    final File file = fileChooser.getSelectedFile();
    prefs.put("particle_dir", file.getParent());
    SimpleResourceLocator locator = null;
    try {
      locator = new SimpleResourceLocator(file.getParentFile().toURI());
      ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, locator);
    } catch (final URISyntaxException ex) {
      ex.printStackTrace();
    }
    try {
      final Spatial obj = (Spatial) new BinaryImporter().load(file);
      final int lidx = editScene.particleNode.getNumberOfChildren();
      if (obj instanceof Node) {
        final Node node = (Node) obj;
        final ArrayList<Spatial> meshes = new ArrayList<>();
        for (int ii = 0, nn = node.getNumberOfChildren(); ii < nn; ii++) {
          if (node.getChild(ii) instanceof ParticleSystem) {
            meshes.add(node.getChild(ii));
          }
        }
        if (meshes.size() == 0) {
          throw new Exception("Node contains no particle meshes");
        }
        layerTable.clearSelection();
        for (final Spatial mesh : meshes) {
          editScene.particleNode.attachChild(mesh);
        }

      } else { // obj instanceof ParticleSystem
        editScene.particleGeom = (ParticleSystem) obj;
        layerTable.clearSelection();
        editScene.particleNode.attachChild(editScene.particleGeom);
      }
      layerModel.fireTableRowsInserted(lidx, editScene.particleNode.getNumberOfChildren() - 1);
      layerTable.setRowSelectionInterval(lidx, lidx);
      deleteLayerButton.setEnabled(true);

    } catch (final Exception e) {
      JOptionPane.showMessageDialog(this, "Couldn't open '" + file + "': " + e, "File Error",
          JOptionPane.ERROR_MESSAGE);
      logger.log(Level.WARNING, "Couldn't open '" + file, e);
    } finally {
      if (locator != null) {
        ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, locator);
      }
    }
  }

  private void saveAs(File file) {
    if (file == null) {
      fileChooser.setSelectedFile(openFile == null ? new File("") : openFile);
      if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
        return;
      }
      file = fileChooser.getSelectedFile();
      prefs.put("particle_dir", file.getParent().toString());
    }
    try {
      new BinaryExporter().save(
          editScene.particleNode.getNumberOfChildren() > 1 ? editScene.particleNode : editScene.particleGeom, file);
      openFile = file;
      updateTitle();
    } catch (final IOException e) {
      JOptionPane.showMessageDialog(this, "Couldn't save '" + file + "': " + e, "File Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void showBackgroundDialog() {
    final Color bg = JColorChooser.showDialog(this, "Choose Background Color",
        AwtColorUtil.makeColor(_canvas.getCanvasRenderer().getRenderer().getBackgroundColor(), false));
    if (bg != null) {
      prefs.putInt("bg_color", bg.getRGB());
      final Callable<Void> exe = () -> {
        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(AwtColorUtil.makeColorRGBA(bg));
        return null;
      };
      GameTaskQueueManager.getManager(ParticleEditorFrame.GLOBAL_CONTEXT).render(exe);
    }
  }

  private void createNewLayer() {
    editScene.particleGeom = ParticleFactory.buildParticles(createLayerName(), 300);
    editScene.particleGeom
        .addInfluence(SimpleParticleInfluenceFactory.createBasicGravity(new Vector3(0, -3.0, 0), true));
    editScene.particleGeom.setEmissionDirection(new Vector3(0.0, 1.0, 0.0));
    editScene.particleGeom.setMaximumAngle(0.2268928);
    editScene.particleGeom.getParticleController().setSpeed(1.0);
    editScene.particleGeom.setMinimumLifeTime(2000.0);
    editScene.particleGeom.setStartSize(10.0);
    editScene.particleGeom.setEndSize(10.0);
    editScene.particleGeom.setStartColor(new ColorRGBA(0.0f, 0.0625f, 1.0f, 1.0f));
    editScene.particleGeom.setEndColor(new ColorRGBA(0.0f, 0.0625f, 1.0f, 0.0f));
    editScene.particleGeom.warmUp(60);

    // set alpha testing
    editScene.particleGeom.setProperty(AlphaTestConsts.KEY_AlphaTestType, AlphaTestConsts.TestFunction.GreaterThan);
    editScene.particleGeom.setProperty(AlphaTestConsts.KEY_AlphaReference, 0f);

    BlendState blend = (BlendState) editScene.particleGeom.getLocalRenderState(StateType.Blend);
    if (blend == null) {
      blend = new BlendState();
      blend.setBlendEnabled(true);
      blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
      editScene.particleGeom.setRenderState(blend);
    }
    blend.setDestinationFunction(BlendState.DestinationFunction.One);
    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("com/ardor3d/tool/editor/particle/flaresmall.jpg",
        Texture.MinificationFilter.BilinearNearestMipMap, TextureStoreFormat.GuessCompressedFormat, true));
    ts.getTexture().setWrap(WrapMode.BorderClamp);
    editScene.particleGeom.setRenderState(ts);

    editScene.particleNode.attachChild(editScene.particleGeom);
  }

  private String createLayerName() {
    int max = -1;
    for (int ii = 0, nn = editScene.particleNode.getNumberOfChildren(); ii < nn; ii++) {
      final String name = editScene.particleNode.getChild(ii).getName();
      if (name.startsWith("Layer #")) {
        try {
          max = Math.max(max, Integer.parseInt(name.substring(7)));
        } catch (final NumberFormatException e) {}
      }
    }
    return "Layer #" + (max + 1);
  }

  private void deleteLayer() {
    final int idx = layerTable.getSelectedRow(),
        sidx = idx == editScene.particleNode.getNumberOfChildren() - 1 ? idx - 1 : idx;
    layerTable.clearSelection();
    editScene.particleNode.detachChildAt(idx);
    layerModel.fireTableRowsDeleted(idx, idx);
    layerTable.setRowSelectionInterval(sidx, sidx);

    if (editScene.particleNode.getNumberOfChildren() == 1) {
      deleteLayerButton.setEnabled(false);
    }
  }

  /**
   * applyExample
   */
  private void applyExample() {
    if (exampleList == null || exampleList.getSelectedValue() == null) {
      return;
    }
    final String examType = exampleList.getSelectedValue().toString();
    editScene.particleGeom.clearInfluences();
    if ("FIRE".equalsIgnoreCase(examType)) {
      editScene.particleGeom.setEmissionDirection(new Vector3(0.0f, 1.0f, 0.0f));
      editScene.particleGeom.setMaximumAngle(0.20943952f);
      editScene.particleGeom.setMinimumAngle(0);
      editScene.particleGeom.getParticleController().setSpeed(1.0f);
      editScene.particleGeom.setMinimumLifeTime(1000.0f);
      editScene.particleGeom.setMaximumLifeTime(1500.0f);
      editScene.particleGeom.setStartSize(40.0f);
      editScene.particleGeom.setEndSize(40.0f);
      editScene.particleGeom.setStartColor(new ColorRGBA(1.0f, 0.312f, 0.121f, 1.0f));
      editScene.particleGeom.setEndColor(new ColorRGBA(1.0f, 0.312f, 0.121f, 0.0f));
      editScene.particleGeom.getParticleController().setControlFlow(true);
      editScene.particleGeom.setReleaseRate(300);
      editScene.particleGeom.setReleaseVariance(0.0f);
      editScene.particleGeom.setInitialVelocity(0.3f);
      editScene.particleGeom.getParticleController().setRepeatType(RepeatType.WRAP);
    } else if ("FOUNTAIN".equalsIgnoreCase(examType)) {
      editScene.particleGeom
          .addInfluence(SimpleParticleInfluenceFactory.createBasicGravity(new Vector3(0, -3f, 0), true));
      editScene.particleGeom.setEmissionDirection(new Vector3(0.0f, 1.0f, 0.0f));
      editScene.particleGeom.setMaximumAngle(0.2268928f);
      editScene.particleGeom.setMinimumAngle(0);
      editScene.particleGeom.getParticleController().setSpeed(1.0f);
      editScene.particleGeom.setMinimumLifeTime(1300.0f);
      editScene.particleGeom.setMaximumLifeTime(1950.0f);
      editScene.particleGeom.setStartSize(10.0f);
      editScene.particleGeom.setEndSize(10.0f);
      editScene.particleGeom.setStartColor(new ColorRGBA(0.0f, 0.0625f, 1.0f, 1.0f));
      editScene.particleGeom.setEndColor(new ColorRGBA(0.0f, 0.0625f, 1.0f, 0.0f));
      editScene.particleGeom.getParticleController().setControlFlow(false);
      editScene.particleGeom.setReleaseRate(300);
      editScene.particleGeom.setReleaseVariance(0.0f);
      editScene.particleGeom.setInitialVelocity(1.1f);
      editScene.particleGeom.getParticleController().setRepeatType(RepeatType.WRAP);
    } else if ("LAVA".equalsIgnoreCase(examType)) {
      editScene.particleGeom
          .addInfluence(SimpleParticleInfluenceFactory.createBasicGravity(new Vector3(0, -3f, 0), true));
      editScene.particleGeom.setEmissionDirection(new Vector3(0.0f, 1.0f, 0.0f));
      editScene.particleGeom.setMaximumAngle(0.418f);
      editScene.particleGeom.setMinimumAngle(0);
      editScene.particleGeom.getParticleController().setSpeed(1.0f);
      editScene.particleGeom.setMinimumLifeTime(1057.0f);
      editScene.particleGeom.setMaximumLifeTime(1500.0f);
      editScene.particleGeom.setStartSize(40.0f);
      editScene.particleGeom.setEndSize(40.0f);
      editScene.particleGeom.setStartColor(new ColorRGBA(1.0f, 0.18f, 0.125f, 1.0f));
      editScene.particleGeom.setEndColor(new ColorRGBA(1.0f, 0.18f, 0.125f, 0.0f));
      editScene.particleGeom.getParticleController().setControlFlow(false);
      editScene.particleGeom.setReleaseRate(300);
      editScene.particleGeom.setReleaseVariance(0.0f);
      editScene.particleGeom.setInitialVelocity(1.1f);
      editScene.particleGeom.getParticleController().setRepeatType(RepeatType.WRAP);
    } else if ("SMOKE".equalsIgnoreCase(examType)) {
      editScene.particleGeom.setEmissionDirection(new Vector3(0.0f, 0.6f, 0.0f));
      editScene.particleGeom.setMaximumAngle(0.36651915f);
      editScene.particleGeom.setMinimumAngle(0);
      editScene.particleGeom.getParticleController().setSpeed(0.2f);
      editScene.particleGeom.setMinimumLifeTime(1000.0f);
      editScene.particleGeom.setMaximumLifeTime(1500.0f);
      editScene.particleGeom.setStartSize(32.5f);
      editScene.particleGeom.setEndSize(40.0f);
      editScene.particleGeom.setStartColor(new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f));
      editScene.particleGeom.setEndColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 0.0f));
      editScene.particleGeom.getParticleController().setControlFlow(false);
      editScene.particleGeom.setReleaseRate(300);
      editScene.particleGeom.setReleaseVariance(0.0f);
      editScene.particleGeom.setInitialVelocity(0.58f);
      editScene.particleGeom.setParticleSpinSpeed(0.08f);
    } else if ("RAIN".equalsIgnoreCase(examType)) {
      editScene.particleGeom
          .addInfluence(SimpleParticleInfluenceFactory.createBasicGravity(new Vector3(0, -3f, 0), true));
      editScene.particleGeom.setEmissionDirection(new Vector3(0.0f, -1.0f, 0.0f));
      editScene.particleGeom.setMaximumAngle(MathUtils.PI);
      editScene.particleGeom.setMinimumAngle(0);
      editScene.particleGeom.getParticleController().setSpeed(0.5f);
      editScene.particleGeom.setMinimumLifeTime(1626.0f);
      editScene.particleGeom.setMaximumLifeTime(2400.0f);
      editScene.particleGeom.setStartSize(9.1f);
      editScene.particleGeom.setEndSize(13.6f);
      editScene.particleGeom.setStartColor(new ColorRGBA(0.16078432f, 0.16078432f, 1.0f, 1.0f));
      editScene.particleGeom.setEndColor(new ColorRGBA(0.16078432f, 0.16078432f, 1.0f, 0.15686275f));
      editScene.particleGeom.getParticleController().setControlFlow(false);
      editScene.particleGeom.setReleaseRate(300);
      editScene.particleGeom.setReleaseVariance(0.0f);
      editScene.particleGeom.setInitialVelocity(0.58f);
      editScene.particleGeom.getParticleController().setRepeatType(RepeatType.WRAP);
    } else if ("SNOW".equalsIgnoreCase(examType)) {
      editScene.particleGeom
          .addInfluence(SimpleParticleInfluenceFactory.createBasicGravity(new Vector3(0, -3f, 0), true));
      editScene.particleGeom.setEmissionDirection(new Vector3(0.0f, -1.0f, 0.0f));
      editScene.particleGeom.setMaximumAngle(MathUtils.HALF_PI);
      editScene.particleGeom.setMinimumAngle(0);
      editScene.particleGeom.getParticleController().setSpeed(0.2f);
      editScene.particleGeom.setMinimumLifeTime(1057.0f);
      editScene.particleGeom.setMaximumLifeTime(1500.0f);
      editScene.particleGeom.setStartSize(30.0f);
      editScene.particleGeom.setEndSize(30.0f);
      editScene.particleGeom.setStartColor(new ColorRGBA(0.3764706f, 0.3764706f, 0.3764706f, 1.0f));
      editScene.particleGeom.setEndColor(new ColorRGBA(0.3764706f, 0.3764706f, 0.3764706f, 0.1882353f));
      editScene.particleGeom.getParticleController().setControlFlow(false);
      editScene.particleGeom.setReleaseRate(300);
      editScene.particleGeom.setReleaseVariance(0.0f);
      editScene.particleGeom.setInitialVelocity(0.59999996f);
      editScene.particleGeom.getParticleController().setRepeatType(RepeatType.WRAP);
    } else if ("JET".equalsIgnoreCase(examType)) {
      editScene.particleGeom.setEmissionDirection(new Vector3(-1.0f, 0.0f, 0.0f));
      editScene.particleGeom.setMaximumAngle(0.034906585f);
      editScene.particleGeom.setMinimumAngle(0);
      editScene.particleGeom.getParticleController().setSpeed(1.0f);
      editScene.particleGeom.setMinimumLifeTime(100.0f);
      editScene.particleGeom.setMaximumLifeTime(150.0f);
      editScene.particleGeom.setStartSize(6.6f);
      editScene.particleGeom.setEndSize(30.0f);
      editScene.particleGeom.setStartColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
      editScene.particleGeom.setEndColor(new ColorRGBA(0.6f, 0.2f, 0.0f, 0.0f));
      editScene.particleGeom.getParticleController().setControlFlow(false);
      editScene.particleGeom.setReleaseRate(300);
      editScene.particleGeom.setReleaseVariance(0.0f);
      editScene.particleGeom.setInitialVelocity(1.4599999f);
      editScene.particleGeom.getParticleController().setRepeatType(RepeatType.WRAP);
    } else if ("EXPLOSION".equalsIgnoreCase(examType)) {
      editScene.particleGeom.setEmissionDirection(new Vector3(0.0f, 1.0f, 0.0f));
      editScene.particleGeom.setMaximumAngle(MathUtils.PI);
      editScene.particleGeom.setMinimumAngle(0);
      editScene.particleGeom.getParticleController().setSpeed(1.4f);
      editScene.particleGeom.setMinimumLifeTime(1000.0f);
      editScene.particleGeom.setMaximumLifeTime(1500.0f);
      editScene.particleGeom.setStartSize(40.0f);
      editScene.particleGeom.setEndSize(40.0f);
      editScene.particleGeom.setStartColor(new ColorRGBA(1.0f, 0.312f, 0.121f, 1.0f));
      editScene.particleGeom.setEndColor(new ColorRGBA(1.0f, 0.24313726f, 0.03137255f, 0.0f));
      editScene.particleGeom.getParticleController().setControlFlow(false);
      editScene.particleGeom.getParticleController().setRepeatType(RepeatType.CLAMP);
    } else if ("GROUND FOG".equalsIgnoreCase(examType)) {
      editScene.particleGeom.setEmissionDirection(new Vector3(0.0f, 0.3f, 0.0f));
      editScene.particleGeom.setMaximumAngle(MathUtils.HALF_PI);
      editScene.particleGeom.setMinimumAngle(MathUtils.HALF_PI);
      editScene.particleGeom.getParticleController().setSpeed(0.5f);
      editScene.particleGeom.setMinimumLifeTime(1774.0f);
      editScene.particleGeom.setMaximumLifeTime(2800.0f);
      editScene.particleGeom.setStartSize(35.4f);
      editScene.particleGeom.setEndSize(40.0f);
      editScene.particleGeom.setStartColor(new ColorRGBA(0.87058824f, 0.87058824f, 0.87058824f, 1.0f));
      editScene.particleGeom.setEndColor(new ColorRGBA(0.0f, 0.8f, 0.8f, 0.0f));
      editScene.particleGeom.getParticleController().setControlFlow(false);
      editScene.particleGeom.setReleaseRate(300);
      editScene.particleGeom.setReleaseVariance(0.0f);
      editScene.particleGeom.setInitialVelocity(1.0f);
      editScene.particleGeom.setParticleSpinSpeed(0.0f);
    } else if ("FIREFLIES".equalsIgnoreCase(examType)) {
      editScene.particleGeom.setEmissionDirection(new Vector3(0, 1, 0));
      editScene.particleGeom.setStartSize(3f);
      editScene.particleGeom.setEndSize(1.5f);
      editScene.particleGeom.setOriginOffset(new Vector3(0, 0, 0));
      editScene.particleGeom.setInitialVelocity(.05f);
      editScene.particleGeom.setMinimumLifeTime(5000f);
      editScene.particleGeom.setMaximumLifeTime(15000f);
      editScene.particleGeom.setStartColor(new ColorRGBA(1, 0, 0, 1));
      editScene.particleGeom.setEndColor(new ColorRGBA(0, 1, 0, 1));
      editScene.particleGeom.setMaximumAngle(MathUtils.PI);
      editScene.particleGeom.getParticleController().setControlFlow(false);
      editScene.particleGeom.getParticleController().setSpeed(0.75f);
      final SwarmInfluence swarm = new SwarmInfluence(new Vector3(0, 0, 0), .001f);
      swarm.setMaxSpeed(.2f);
      swarm.setSpeedBump(0.025f);
      swarm.setTurnSpeed(MathUtils.DEG_TO_RAD * 360);
      editScene.particleGeom.addInfluence(swarm);
      final WanderInfluence wander = new WanderInfluence();
      editScene.particleGeom.addInfluence(wander);
    }
    editScene.particleGeom.warmUp(60);

    updateFromManager();
  }

  /**
   * updateFromManager
   */
  public void updateFromManager() {
    // update appearance controls
    appearancePanel.setEdittedParticles(editScene.particleGeom);
    appearancePanel.updateWidgets();

    // update flow controls
    flowPanel.setEdittedParticles(editScene.particleGeom);
    flowPanel.updateWidgets();

    // update emission controls
    emissionPanel.setEdittedParticles(editScene.particleGeom);
    emissionPanel.updateWidgets();

    // update world controls
    scenePanel.setEdittedParticles(editScene.particleGeom);
    scenePanel.updateWidgets();

    // update influence controls
    influencePanel.setEdittedParticles(editScene.particleGeom);
    influencePanel.updateWidgets();

    validate();
  }

  /**
   * updateManager
   *
   * @param particles
   *          number of particles to reset manager with.
   */
  public void resetManager(final int particles) {
    editScene.particleGeom.recreate(particles);
    validate();
  }

  private void initFileChooser() {
    final String pdir = prefs.get("particle_dir", null);
    if (pdir != null) {
      fileChooser.setCurrentDirectory(new File(pdir));
    }
    fileChooser.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(final File f) {
        return f.isDirectory() || f.toString().toLowerCase().endsWith(".a3d");
      }

      @Override
      public String getDescription() { return "Ardor3D Files (*.a3d)"; }
    });
  }

  protected Lwjgl3AwtCanvas createParticleCanvas() {

    final LogicalLayer logicalLayer = new LogicalLayer();
    editScene = new ParticleEditorScene(prefs, logicalLayer);
    final Lwjgl3CanvasRenderer canvasRenderer = new Lwjgl3CanvasRenderer(editScene) {
      @Override
      public void init(final Canvas canvas, final DisplaySettings settings, final boolean doSwap) {
        super.init(canvas, settings, doSwap);
        _camera.setFrustumFar(10000);
      }
    };

    final GLData data = new GLData();
    data.depthSize = 16;
    data.doubleBuffer = true;
    data.profile = GLData.Profile.CORE;
    data.majorVersion = 3;
    data.minorVersion = 3;

    final Lwjgl3AwtCanvas theCanvas = new Lwjgl3AwtCanvas(data, canvasRenderer);
    theCanvas.setSize(new Dimension(width, height));
    theCanvas.setMinimumSize(new Dimension(100, 100));
    theCanvas.setVisible(true);

    theCanvas.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        if (canvasRenderer.getCamera() != null) {
          canvasRenderer.getCamera().resize(theCanvas.getWidth(), theCanvas.getHeight());
          canvasRenderer.getCamera().setFrustumPerspective(45.0, theCanvas.getWidth() / (float) theCanvas.getHeight(),
              1, 10000);
        }
      }
    });

    final AwtKeyboardWrapper keyboardWrapper = new AwtKeyboardWrapper(theCanvas);
    final AwtFocusWrapper focusWrapper = new AwtFocusWrapper(theCanvas);
    final AwtMouseWrapper mouseWrapper = new AwtMouseWrapper(theCanvas, new AwtMouseManager(theCanvas));

    final PhysicalLayer pl = new PhysicalLayer.Builder() //
        .with((KeyboardWrapper) keyboardWrapper) //
        .with(mouseWrapper) //
        .with(focusWrapper) //
        .build();

    final FrameHandler handler = new FrameHandler(new Timer());
    handler.addCanvas(theCanvas);
    handler.addUpdater(editScene);

    logicalLayer.registerInput(theCanvas, pl);

    final Color bg = new Color(prefs.getInt("bg_color", 0));
    final RendererCallable<Void> exe = new RendererCallable<>() {
      @Override
      public Void call() {
        getRenderer().setBackgroundColor(AwtColorUtil.makeColorRGBA(bg));
        return null;
      }
    };
    GameTaskQueueManager.getManager(ParticleEditorFrame.GLOBAL_CONTEXT).render(exe);

    camhand = new CameraHandler();

    theCanvas.addMouseWheelListener(camhand);
    theCanvas.addMouseListener(camhand);
    theCanvas.addMouseMotionListener(camhand);

    editScene.init();

    final Thread t = new Thread() {
      @Override
      public void run() {
        while (true) {
          handler.updateFrame();
        }
      }
    };
    t.start();

    return theCanvas;
  }

  class CameraHandler extends MouseAdapter implements MouseMotionListener, MouseWheelListener {
    Point last = new Point(0, 0);
    Vector3 focus = new Vector3();
    private final Vector3 vector = new Vector3();
    private final Quaternion rot = new Quaternion();
    public Vector3 worldUpVector = new Vector3(Vector3.UNIT_Y);

    @Override
    public void mouseDragged(final MouseEvent arg0) {
      final int difX = last.x - arg0.getX();
      final int difY = last.y - arg0.getY();
      final int mult = arg0.isShiftDown() ? 10 : 1;
      last.x = arg0.getX();
      last.y = arg0.getY();

      final int mods = arg0.getModifiersEx();
      if ((mods & InputEvent.BUTTON1_DOWN_MASK) != 0) {
        rotateCamera(worldUpVector, difX * 0.0025f);
        rotateCamera(Camera.getCurrentCamera().getLeft(), -difY * 0.0025);
      }
      if ((mods & InputEvent.BUTTON2_DOWN_MASK) != 0 && difY != 0) {
        zoomCamera(difY * mult);
      }
      if ((mods & InputEvent.BUTTON3_DOWN_MASK) != 0) {
        panCamera(-difX, -difY);
      }
    }

    @Override
    public void mouseMoved(final MouseEvent arg0) {}

    @Override
    public void mousePressed(final MouseEvent arg0) {
      last.x = arg0.getX();
      last.y = arg0.getY();
    }

    @Override
    public void mouseWheelMoved(final MouseWheelEvent arg0) {
      zoomCamera(arg0.getWheelRotation() * (arg0.isShiftDown() ? -100 : -20));
    }

    public void recenterCamera() {
      final Camera cam = Camera.getCurrentCamera();
      if (cam == null) {
        return;
      }
      Vector3.ZERO.subtract(focus, vector);
      final Vector3 store = Vector3.fetchTempInstance();
      store.set(vector).addLocal(cam.getLocation());
      cam.setLocation(store);
      Vector3.releaseTempInstance(store);
      focus.addLocal(vector);
      cam.lookAt(focus, worldUpVector);
      cam.onFrameChange();
    }

    private void rotateCamera(final ReadOnlyVector3 axis, double amount) {
      final Camera cam = Camera.getCurrentCamera();
      if (cam == null) {
        return;
      }
      if (axis.equals(cam.getLeft())) {
        final double elevation = -Math.asin(cam.getDirection().getZ());
        // keep the camera constrained to -89 -> 89 degrees elevation
        amount =
            Math.min(Math.max(elevation + amount, -(MathUtils.DEG_TO_RAD * 89)), MathUtils.DEG_TO_RAD * 89) - elevation;
      }
      rot.fromAngleAxis(amount, axis);
      cam.getLocation().subtract(focus, vector);
      rot.apply(vector, vector);
      final Vector3 store = Vector3.fetchTempInstance();
      focus.add(vector, store);
      cam.setLocation(store);
      Vector3.releaseTempInstance(store);
      cam.lookAt(focus, worldUpVector);
    }

    private void panCamera(final float left, final float up) {
      final Camera cam = Camera.getCurrentCamera();
      if (cam == null) {
        return;
      }
      cam.getLeft().multiply(left, vector);
      cam.getUp().scaleAdd(up, vector, vector);
      final Vector3 store = Vector3.fetchTempInstance();
      store.set(vector).addLocal(cam.getLocation());
      cam.setLocation(store);
      Vector3.releaseTempInstance(store);
      focus.addLocal(vector);
      cam.onFrameChange();
    }

    private void zoomCamera(double amount) {
      final Camera cam = Camera.getCurrentCamera();
      if (cam == null) {
        return;
      }
      final double dist = cam.getLocation().distance(focus);
      amount = dist - Math.max(0.0, dist - amount);
      vector.set(cam.getDirection()).scaleAdd(amount, cam.getLocation(), vector);
      cam.setLocation(vector);
      cam.onFrameChange();
    }
  }

  class LayerTableModel extends AbstractTableModel {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public int getRowCount() {
      return editScene.particleNode == null ? 0 : editScene.particleNode.getNumberOfChildren();
    }

    @Override
    public int getColumnCount() { return 2; }

    @Override
    public String getColumnName(final int columnIndex) {
      return columnIndex == 0 ? "Name" : "Visible";
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
      return columnIndex == 0 ? String.class : Boolean.class;
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
      return true;
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      final ParticleSystem pmesh = (ParticleSystem) editScene.particleNode.getChild(rowIndex);
      return columnIndex == 0 ? pmesh.getName()
          : Boolean.valueOf(pmesh.getSceneHints().getCullHint() != CullHint.Always);
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
      final ParticleSystem pmesh = (ParticleSystem) editScene.particleNode.getChild(rowIndex);
      if (columnIndex == 0) {
        pmesh.setName((String) aValue);
      } else {
        pmesh.getSceneHints().setCullHint(((Boolean) aValue).booleanValue() ? CullHint.Dynamic : CullHint.Always);
      }
    }
  }

  public static void addDefaultResourceLocators() {
    try {
      ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MATERIAL, new SimpleResourceLocator(
          ResourceLocatorTool.getClassPathResource(MaterialManager.class, "com/ardor3d/renderer/material")));
      ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_SHADER, new SimpleResourceLocator(
          ResourceLocatorTool.getClassPathResource(MaterialManager.class, "com/ardor3d/renderer/shader")));
      MaterialManager.INSTANCE.setDefaultMaterial(YamlMaterialReader
          .load(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MATERIAL, "basic_white.yaml")));
    } catch (final URISyntaxException ex) {
      ex.printStackTrace();
    }
  }
}
