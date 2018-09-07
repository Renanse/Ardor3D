/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example;

import java.awt.BorderLayout;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.ardor3d.util.Ardor3dException;

public final class PropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(PropertiesDialog.class.getName());

    private static final long serialVersionUID = 1L;

    // connection to properties file.
    private final PropertiesGameSettings source;

    // Title Image
    private URL imageFile = null;

    // Array of supported display modes
    private DisplayMode[] modes = null;

    // Array of windowed resolutions
    private final String[] windowedResolutions = { "640 x 480", "800 x 600", "1024 x 768", "1152 x 864",
            "1920 x 1080" };

    // Array of possible samples
    private final String[] samples = { "0 samples", "1 samples", "2 samples", "4 samples", "8 samples" };

    // UI components
    private JCheckBox fullscreenBox = null;

    private JComboBox<String> displayResCombo = null;

    private JComboBox<String> samplesCombo = null;

    private JComboBox<String> colorDepthCombo = null;

    private JComboBox<String> displayFreqCombo = null;

    private JLabel icon = null;

    private boolean cancelled = false;

    private final Stack<Runnable> mainThreadTasks;

    /**
     * Constructor for the <code>PropertiesDialog</code>. Creates a properties dialog initialized for the primary
     * display.
     *
     * @param source
     *            the <code>GameSettings</code> object to use for working with the properties file.
     * @param imageFile
     *            the image file to use as the title of the dialog; <code>null</code> will result in to image being
     *            displayed
     * @throws Ardor3dException
     *             if the source is <code>null</code>
     */
    public PropertiesDialog(final PropertiesGameSettings source, final String imageFile) {
        this(source, imageFile, null);
    }

    /**
     * Constructor for the <code>PropertiesDialog</code>. Creates a properties dialog initialized for the primary
     * display.
     *
     * @param source
     *            the <code>GameSettings</code> object to use for working with the properties file.
     * @param imageFile
     *            the image file to use as the title of the dialog; <code>null</code> will result in to image being
     *            displayed
     * @throws Ardor3dException
     *             if the source is <code>null</code>
     */
    public PropertiesDialog(final PropertiesGameSettings source, final URL imageFile) {
        this(source, imageFile, null);
    }

    /**
     * Constructor for the <code>PropertiesDialog</code>. Creates a properties dialog initialized for the primary
     * display.
     *
     * @param source
     *            the <code>GameSettings</code> object to use for working with the properties file.
     * @param imageFile
     *            the image file to use as the title of the dialog; <code>null</code> will result in to image being
     *            displayed
     * @throws Ardor3dException
     *             if the source is <code>null</code>
     */
    public PropertiesDialog(final PropertiesGameSettings source, final String imageFile,
            final Stack<Runnable> mainThreadTasks) {
        this(source, getURL(imageFile), mainThreadTasks);
    }

    /**
     * Constructor for the <code>PropertiesDialog</code>. Creates a properties dialog initialized for the primary
     * display.
     *
     * @param source
     *            the <code>GameSettings</code> object to use for working with the properties file.
     * @param imageFile
     *            the image file to use as the title of the dialog; <code>null</code> will result in to image being
     *            displayed
     * @param mainThreadTasks
     * @throws Ardor3dException
     *             if the source is <code>null</code>
     */
    public PropertiesDialog(final PropertiesGameSettings source, final URL imageFile,
            final Stack<Runnable> mainThreadTasks) {
        if (null == source) {
            throw new Ardor3dException("PropertyIO source cannot be null");
        }

        this.source = source;
        this.imageFile = imageFile;
        this.mainThreadTasks = mainThreadTasks;

        final ModesRetriever retrieval = new ModesRetriever();
        if (mainThreadTasks != null) {
            mainThreadTasks.add(retrieval);
        } else {
            retrieval.run();
        }
        modes = retrieval.getModes();
        Arrays.sort(modes, new DisplayModeSorter());

        createUI();
    }

    /**
     * <code>setImage</code> sets the background image of the dialog.
     *
     * @param image
     *            <code>String</code> representing the image file.
     */
    public void setImage(final String image) {
        try {
            final URL file = new URL("file:" + image);
            setImage(file);
            // We can safely ignore the exception - it just means that the user
            // gave us a bogus file
        } catch (final MalformedURLException e) {
        }
    }

    /**
     * <code>setImage</code> sets the background image of this dialog.
     *
     * @param image
     *            <code>URL</code> pointing to the image file.
     */
    public void setImage(final URL image) {
        icon.setIcon(new ImageIcon(image));
        pack(); // Resize to accomodate the new image
        center();
    }

    /**
     * <code>showDialog</code> sets this dialog as visble, and brings it to the front.
     */
    private void showDialog() {
        setVisible(true);
        toFront();
    }

    /**
     * <code>center</code> places this <code>PropertiesDialog</code> in the center of the screen.
     */
    private void center() {
        int x, y;
        x = (Toolkit.getDefaultToolkit().getScreenSize().width - getWidth()) / 2;
        y = (Toolkit.getDefaultToolkit().getScreenSize().height - getHeight()) / 2;
        this.setLocation(x, y);
    }

    /**
     * <code>init</code> creates the components to use the dialog.
     */
    private void createUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
            logger.warning("Could not set native look and feel.");
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                cancelled = true;
                dispose();
            }
        });

        setTitle("Select Display Settings");

        // The panels...
        final JPanel mainPanel = new JPanel();
        final JPanel centerPanel = new JPanel();
        final JPanel optionsPanel = new JPanel();
        final JPanel buttonPanel = new JPanel();
        // The buttons...
        final JButton ok = new JButton("Ok");
        final JButton cancel = new JButton("Cancel");

        icon = new JLabel(imageFile != null ? new ImageIcon(imageFile) : null);

        mainPanel.setLayout(new BorderLayout());

        centerPanel.setLayout(new BorderLayout());

        final KeyListener aListener = new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (verifyAndSaveCurrentSelection()) {
                        dispose();
                    }
                }
            }
        };

        displayResCombo = setUpResolutionChooser();
        displayResCombo.addKeyListener(aListener);
        samplesCombo = setUpSamplesChooser();
        samplesCombo.addKeyListener(aListener);
        colorDepthCombo = new JComboBox<String>();
        colorDepthCombo.addKeyListener(aListener);
        displayFreqCombo = new JComboBox<String>();
        displayFreqCombo.addKeyListener(aListener);
        fullscreenBox = new JCheckBox("Fullscreen?");
        fullscreenBox.setSelected(source.isFullscreen());
        fullscreenBox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                updateResolutionChoices();
            }
        });

        updateResolutionChoices();
        displayResCombo.setSelectedItem(source.getWidth() + " x " + source.getHeight());

        samplesCombo.setSelectedItem(source.getSamples() + " samples");

        optionsPanel.add(displayResCombo);
        optionsPanel.add(colorDepthCombo);
        optionsPanel.add(displayFreqCombo);
        optionsPanel.add(samplesCombo);
        optionsPanel.add(fullscreenBox);

        // Set the button action listeners. Cancel disposes without saving, OK
        // saves.
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (verifyAndSaveCurrentSelection()) {
                    dispose();
                }
            }
        });

        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                cancelled = true;
                dispose();
            }
        });

        buttonPanel.add(ok);
        buttonPanel.add(cancel);

        if (icon != null) {
            centerPanel.add(icon, BorderLayout.NORTH);
        }
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);

        pack();
        center();
        showDialog();
    }

    /**
     * <code>verifyAndSaveCurrentSelection</code> first verifies that the display mode is valid for this system, and
     * then saves the current selection as a properties.cfg file.
     *
     * @return if the selection is valid
     */
    private boolean verifyAndSaveCurrentSelection() {
        String display = (String) displayResCombo.getSelectedItem();
        final boolean fullscreen = fullscreenBox.isSelected();

        final int width = Integer.parseInt(display.substring(0, display.indexOf(" x ")));
        display = display.substring(display.indexOf(" x ") + 3);
        final int height = Integer.parseInt(display);

        String depthString = (String) colorDepthCombo.getSelectedItem();
        int depth = 0;
        if (depthString != null) {
            depthString = depthString.substring(0, depthString.indexOf(' '));
            if (depthString.equals("?")) {
                depth = DisplayMode.BIT_DEPTH_MULTI;
            } else {
                depth = Integer.parseInt(depthString);
            }
        }

        final String freqString = (String) displayFreqCombo.getSelectedItem();
        int freq = -1;
        if (fullscreen) {
            freq = Integer.parseInt(freqString.substring(0, freqString.indexOf(' ')));
        }

        final String samplesString = (String) samplesCombo.getSelectedItem();
        int samples = -1;
        samples = Integer.parseInt(samplesString.substring(0, samplesString.indexOf(' ')));

        boolean valid = false;

        // test valid display mode when going full screen
        if (!fullscreen) {
            valid = true;
        } else {
            final ModeValidator validator = new ModeValidator("LWJGL", width, height, depth, freq, samples);
            if (mainThreadTasks != null) {
                mainThreadTasks.add(validator);
            } else {
                validator.run();
            }

            valid = validator.isValid();
        }

        if (valid) {
            // use the GameSettings class to save it.
            source.setWidth(width);
            source.setHeight(height);
            source.setDepth(depth);
            source.setFrequency(freq);
            source.setFullscreen(fullscreen);
            source.setRenderer("LWJGL");
            source.setSamples(samples);
            try {
                source.save();
            } catch (final IOException ioe) {
                logger.log(Level.WARNING, "Failed to save setting changes", ioe);
            }
        } else {
            showError(this, "Your monitor claims to not support the display mode you've selected.\n"
                    + "The combination of bit depth and refresh rate is not supported.");
        }

        return valid;
    }

    /**
     * <code>setUpChooser</code> retrieves all available display modes and places them in a <code>JComboBox</code>. The
     * resolution specified by GameSettings is used as the default value.
     *
     * @return the combo box of display modes.
     */
    private JComboBox<String> setUpResolutionChooser() {
        final String[] res = getResolutions(modes);
        final JComboBox<String> resolutionBox = new JComboBox<String>(res);

        resolutionBox.setSelectedItem(source.getWidth() + " x " + source.getHeight());
        resolutionBox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                updateDisplayChoices();
            }
        });

        return resolutionBox;
    }

    private JComboBox<String> setUpSamplesChooser() {
        final JComboBox<String> nameBox = new JComboBox<String>(samples);
        nameBox.setSelectedItem(source.getSamples() + " samples");
        return nameBox;
    }

    /**
     * <code>updateDisplayChoices</code> updates the available color depth and display frequency options to match the
     * currently selected resolution.
     */
    private void updateDisplayChoices() {
        if (!fullscreenBox.isSelected()) {
            // don't run this function when changing windowed settings
            return;
        }
        final String resolution = (String) displayResCombo.getSelectedItem();
        String colorDepth = (String) colorDepthCombo.getSelectedItem();
        if (colorDepth == null) {
            colorDepth = (source.getDepth() != DisplayMode.BIT_DEPTH_MULTI) ? source.getDepth() + " bpp" : "? bpp";
        }
        String displayFreq = (String) displayFreqCombo.getSelectedItem();
        if (displayFreq == null) {
            displayFreq = source.getFrequency() + " Hz";
        }

        // grab available depths
        final String[] depths = getDepths(resolution, modes);
        colorDepthCombo.setModel(new DefaultComboBoxModel<String>(depths));
        colorDepthCombo.setSelectedItem(colorDepth);
        // grab available frequencies
        final String[] freqs = getFrequencies(resolution, modes);
        displayFreqCombo.setModel(new DefaultComboBoxModel<String>(freqs));
        // Try to reset freq
        displayFreqCombo.setSelectedItem(displayFreq);
    }

    /**
     * <code>updateResolutionChoices</code> updates the available resolutions list to match the currently selected
     * window mode (fullscreen or windowed). It then sets up a list of standard options (if windowed) or calls
     * <code>updateDisplayChoices</code> (if fullscreen).
     */
    private void updateResolutionChoices() {
        if (!fullscreenBox.isSelected()) {
            displayResCombo.setModel(new DefaultComboBoxModel<String>(windowedResolutions));
            colorDepthCombo.setModel(new DefaultComboBoxModel<String>(new String[] { "24 bpp", "16 bpp" }));
            displayFreqCombo.setModel(new DefaultComboBoxModel<String>(new String[] { "n/a" }));
            displayFreqCombo.setEnabled(false);
        } else {
            displayResCombo.setModel(new DefaultComboBoxModel<String>(getResolutions(modes)));
            displayFreqCombo.setEnabled(true);
            updateDisplayChoices();
        }
        pack();
    }

    //
    // Utility methods
    //

    /**
     * Utility method for converting a String denoting a file into a URL.
     *
     * @return a URL pointing to the file or null
     */
    private static URL getURL(final String file) {
        URL url = null;
        try {
            url = new URL("file:" + file);
        } catch (final MalformedURLException e) {
        }
        return url;
    }

    private static void showError(final java.awt.Component parent, final String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Returns every unique resolution from an array of <code>DisplayMode</code>s.
     */
    private static String[] getResolutions(final DisplayMode[] modes) {
        final List<String> resolutions = new ArrayList<String>(modes.length);
        for (int i = 0; i < modes.length; i++) {
            final String res = modes[i].getWidth() + " x " + modes[i].getHeight();
            if (!resolutions.contains(res)) {
                resolutions.add(res);
            }
        }

        final String[] res = new String[resolutions.size()];
        resolutions.toArray(res);
        return res;
    }

    /**
     * Returns every possible bit depth for the given resolution.
     */
    private static String[] getDepths(final String resolution, final DisplayMode[] modes) {
        final Set<String> depths = new TreeSet<String>(new Comparator<String>() {
            public int compare(final String o1, final String o2) {
                // reverse order
                return -o1.compareTo(o2);
            }
        });
        for (int i = 0; i < modes.length; i++) {
            // Filter out modes with bit depths that we don't care about.
            if (modes[i].getBitDepth() < 16 && modes[i].getBitDepth() != DisplayMode.BIT_DEPTH_MULTI) {
                continue;
            }

            final String res = modes[i].getWidth() + " x " + modes[i].getHeight();
            final String depth = (modes[i].getBitDepth() != DisplayMode.BIT_DEPTH_MULTI)
                    ? modes[i].getBitDepth() + " bpp"
                    : "? bpp";
            if (res.equals(resolution) && !depths.contains(depth)) {
                depths.add(depth);
            }
        }

        final String[] res = new String[depths.size()];
        depths.toArray(res);
        return res;
    }

    /**
     * Returns every possible refresh rate for the given resolution.
     */
    private static String[] getFrequencies(final String resolution, final DisplayMode[] modes) {
        final List<String> freqs = new ArrayList<String>(4);
        for (int i = 0; i < modes.length; i++) {
            final String res = modes[i].getWidth() + " x " + modes[i].getHeight();
            final String freq = modes[i].getRefreshRate() + " Hz";
            if (res.equals(resolution) && !freqs.contains(freq)) {
                freqs.add(freq);
            }
        }

        final String[] res = new String[freqs.size()];
        freqs.toArray(res);
        return res;
    }

    /**
     * Utility class for sorting <code>DisplayMode</code>s. Sorts by resolution, then bit depth, and then finally
     * refresh rate.
     */
    private static class DisplayModeSorter implements Comparator<DisplayMode> {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(final DisplayMode a, final DisplayMode b) {
            // Width
            if (a.getWidth() != b.getWidth()) {
                return (a.getWidth() > b.getWidth()) ? 1 : -1;
            }
            // Height
            if (a.getHeight() != b.getHeight()) {
                return (a.getHeight() > b.getHeight()) ? 1 : -1;
            }
            // Bit depth
            if (a.getBitDepth() != b.getBitDepth()) {
                return (a.getBitDepth() > b.getBitDepth()) ? 1 : -1;
            }
            // Refresh rate
            if (a.getRefreshRate() != b.getRefreshRate()) {
                return (a.getRefreshRate() > b.getRefreshRate()) ? 1 : -1;
            }
            // All fields are equal
            return 0;
        }
    }

    /**
     * @return Returns true if this dialog was cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    private static class ModeValidator implements Runnable {

        boolean ready = false, valid = true;

        String renderer;

        ModeValidator(final String renderer, final int width, final int height, final int depth, final int freq,
                final int samples) {
            this.renderer = renderer;
        }

        public void run() {
            if (renderer.startsWith("LWJGL")) {
                // TODO: can we implement this?
            } else if (renderer.startsWith("JOGL")) {
                // TODO: can we implement this?
            }
            ready = true;
        }

        public boolean isValid() {
            while (!ready) {
                try {
                    Thread.sleep(10);
                } catch (final Exception e) {
                }
            }
            return valid;
        }
    }

    private static class ModesRetriever implements Runnable {

        boolean ready = false;
        DisplayMode[] modes = null;

        ModesRetriever() {}

        public void run() {
            try {
                modes = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayModes();
            } catch (final Exception e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "PropertiesDialog(GameSettings, URL)",
                        "Exception", e);
                return;
            }
            ready = true;
        }

        public DisplayMode[] getModes() {
            while (!ready) {
                try {
                    Thread.sleep(10);
                } catch (final Exception e) {
                }
            }
            return modes;
        }
    }
}
