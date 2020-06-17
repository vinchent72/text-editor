package editor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import java.util.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class TextEditor extends JFrame implements ActionListener {
    private final JTextArea textArea;
    private final JFileChooser jfc;
    private Pattern searchPattern;
    private Matcher searchMatcher;
    private final Deque<Integer> storedIndex = new ArrayDeque<>();
    private int index = 0;
    private boolean backward = false;
    private final JMenuBar menuBar;

    private final JMenu fileMenu, searchMenu;
    private final JMenuItem openMenu, saveMenu, exitMenu, startSearchMenu, previousMatchMenu, nextMatchMenu, useRegexpMenu;
    private final JButton openButton, saveButton, searchButton, previousButton, nextButton;
    private final JTextField searchField;
    private final JCheckBox regexCheck;
    private SearchWorker searchWorker;

    private JButton makeButton (ImageIcon icon, String caption, String name, JPanel jp) {
        JButton jb = new JButton(icon);
        jb.setName(name);
        jb.setMargin(new Insets(0, 0, 0, 0));
        jb.setActionCommand(caption);
        jb.addActionListener(this);
        jp.add(jb);
        return jb;
    }

    private JMenuItem makeMenuItem (String mText, String miName, JMenu jm) {
        JMenuItem mi = new JMenuItem(mText);
        mi.setName(miName);
        mi.setActionCommand(mText);
        mi.addActionListener(this);
        jm.add(mi);
        return mi;
    }

    public TextEditor() {
        super("Text Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setMinimumSize(new Dimension(600, 600));
        setLocationRelativeTo(null);

         //File Chooser
        jfc = new JFileChooser((FileSystemView.getFileSystemView().getHomeDirectory()));
        jfc.setName("FileChooser");
        add(jfc);

        //Build Menu
        fileMenu = new JMenu("File");
        fileMenu.setName("MenuFile");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        openMenu = makeMenuItem("Open", "MenuOpen", fileMenu);
        saveMenu = makeMenuItem("Save", "MenuSave", fileMenu);
        fileMenu.addSeparator();
        exitMenu = makeMenuItem("Exit", "MenuExit", fileMenu);

        searchMenu = new JMenu("Search");
        searchMenu.setName("MenuSearch");
        searchMenu.setMnemonic(KeyEvent.VK_S);
        startSearchMenu = makeMenuItem("Start search", "MenuStartSearch", searchMenu);
        previousMatchMenu = makeMenuItem("Previous match", "MenuPreviousMatch", searchMenu);
        nextMatchMenu = makeMenuItem("Next match", "MenuNextMatch", searchMenu);
        useRegexpMenu = makeMenuItem("Use regular expressions", "MenuUseRegExp",searchMenu);

        menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(searchMenu);
        setJMenuBar(menuBar);

        //Control Panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(controlPanel, BorderLayout.NORTH);

        //Open Button
        ImageIcon openIcon = resizeIcon("../Icons/open.png", 24, 24);
        openButton = makeButton(openIcon, "Open", "OpenButton", controlPanel);
        //Save Button
        ImageIcon saveIcon = resizeIcon("../Icons/save.png", 24, 24);
        saveButton = makeButton(saveIcon,"Save", "SaveButton", controlPanel);
        //Search Field
        searchField = new JTextField(12);
        searchField.setName("SearchField");
        controlPanel.add(searchField);
        //Search Button
        ImageIcon searchIcon = resizeIcon("../Icons/search.png", 24, 24);
        searchButton = makeButton(searchIcon, "Start search", "StartSearchButton", controlPanel);
        //Previous Button
        ImageIcon previousIcon = resizeIcon("../Icons/previous.png", 24, 24);
        previousButton = makeButton(previousIcon, "Previous match", "PreviousMatchButton", controlPanel);
        //Next Button
        ImageIcon nextIcon = resizeIcon("../Icons/next.png", 24, 24);
        nextButton = makeButton(nextIcon, "Next match", "NextMatchButton", controlPanel);
        //Regex Checkbox
        regexCheck = new JCheckBox("Use regex");
        regexCheck.setName("UseRegExCheckbox");
        regexCheck.setActionCommand("Use regular expressions");
        regexCheck.addActionListener(this);
        controlPanel.add(regexCheck);

        //Add Editor Pane
        JPanel editorPane = new JPanel();
        editorPane.setBounds(40, 250, 500, 400);
        editorPane.setLayout(new BorderLayout());
        //editorPane.setBackground(Color.GREEN);
        add(editorPane);
        //Text Area
        textArea = new JTextArea();
        textArea.setName("TextArea");
        JScrollPane scrollableTextArea = new JScrollPane(textArea);
        scrollableTextArea.setName("ScrollPane");
        scrollableTextArea.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollableTextArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        //Add Scroll Pane to Editor Pane
        setMargin(scrollableTextArea, 0, 5, 5, 5);
        editorPane.add(scrollableTextArea);

        setVisible(true);
    }

    static String readFileAsString(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    static void writeStringToFile(String fileName, String text) throws IOException {
        Files.writeString(Paths.get(fileName),text);
    }

    // set J Component Margin
    public static void setMargin(JComponent aComponent, int aTop,
                                 int aRight, int aBottom, int aLeft) {

        Border border = aComponent.getBorder();

        Border marginBorder = new EmptyBorder(new Insets(aTop, aLeft,
                aBottom, aRight)); //from www.java2s.com
        aComponent.setBorder(border == null ? marginBorder
                : new CompoundBorder(marginBorder, border));
    }

    public static ImageIcon resizeIcon( String iconFile, int width, int height) {
        ImageIcon imageIcon = new ImageIcon(iconFile); // load the image to a imageIcon
        Image image = imageIcon.getImage(); // transform it
        Image newimg = image.getScaledInstance(width, height,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
        return new ImageIcon(newimg);  // transform it back
    }

    private String loadText() {
        String text = "";
        index = 0;
        int dialogReturn = jfc.showOpenDialog(null);
        if (dialogReturn == JFileChooser.APPROVE_OPTION) {
            String openFileName = jfc.getSelectedFile().getAbsolutePath();
            if (openFileName.trim().length() > 0) {
                try {
                    text = readFileAsString(openFileName);
                } catch (IOException e) {
                }
            }
        }
        return text;
    }

    //Action Listener for Save
    private void saveText(String text) {
        int dialogReturn = jfc.showSaveDialog(null);
        if (dialogReturn == JFileChooser.APPROVE_OPTION) {
            String saveFileName = jfc.getSelectedFile().getAbsolutePath();
            if (saveFileName.trim().length() > 0) {
                try {
                    writeStringToFile(saveFileName, text);
                } catch (IOException e) {
                }
            }
        }
    }

    private class SearchWorker extends SwingWorker<Matcher, Void> {
        @Override
        public Matcher doInBackground() {
            String searchText = searchField.getText();
            String fullText = textArea.getText();
            if (searchText.trim().equals("") || fullText.trim().equals("")) {
                return null;
            }
            Pattern ptn = regexCheck.isSelected() ? Pattern.compile(searchText) : Pattern.compile(searchText, Pattern.LITERAL);
            Matcher mth = ptn.matcher(fullText);
            while (mth.find()) {
                storedIndex.offerLast(mth.start());
            }
            System.out.println(storedIndex);
            return mth;
        }

        @Override
        protected void done() {
            try {
                searchMatcher = get();
            } catch (Exception ignoreForNow) {
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "Exit":
                dispose();
                System.exit(0);
                break;
            case "Open" :
                textArea.setText(loadText());
                previousButton.setEnabled(false);
                previousButton.setEnabled(false);
                nextMatchMenu.setEnabled(false);
                nextButton.setEnabled(false);
                break;
            case "Save" :
                saveText(textArea.getText());
                break;
            case "Start search" :
                storedIndex.clear();
                (searchWorker = new SearchWorker()).run();
                if (searchMatcher == null || storedIndex.isEmpty()) {
                    break;
                }
                backward = false;
                index = storedIndex.pollFirst();
                if (searchMatcher.find(index)) {
                    previousButton.setEnabled(true);
                    previousButton.setEnabled(true);
                    nextMatchMenu.setEnabled(true);
                    nextButton.setEnabled(true);
                    storedIndex.offerLast(index);
                    textArea.setCaretPosition(index + searchMatcher.group().length());
                    textArea.select(index, index + searchMatcher.group().length());
                    textArea.grabFocus();
                }
                break;
            case "Previous match" :
                if (searchMatcher == null || storedIndex.isEmpty()) {
                    break;
                }
                if (!backward) {
                    storedIndex.offerFirst(storedIndex.pollLast());
                    backward = true;
                }
                index = storedIndex.pollLast();
                if (searchMatcher.find(index)) {
                    index = searchMatcher.start();
                    storedIndex.offerFirst(index);
                    textArea.setCaretPosition(index + searchMatcher.group().length());
                    System.out.println(textArea.getCaretPosition());
                    textArea.select(index, index + searchMatcher.group().length());
                    textArea.grabFocus();
                }
                break;
            case "Next match" :
                if (searchMatcher == null || storedIndex.isEmpty()) {
                    break;
                }
                if (backward) {
                    storedIndex.offerLast(storedIndex.pollFirst());
                    backward = false;
                }
                index = storedIndex.pollFirst();
                if (!searchMatcher.find(index)) {
                    index = storedIndex.pollFirst();
                }
                if (searchMatcher.find(index)) {
                    index = searchMatcher.start();
                    storedIndex.offerLast(index);
                    textArea.setCaretPosition(index + searchMatcher.group().length());
                    System.out.println(textArea.getCaretPosition());
                    textArea.select(index, index + searchMatcher.group().length());
                    textArea.grabFocus();
                }
                break;
            case "Use regular expressions" :
                regexCheck.setSelected(true);
            default:
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TextEditor();
            }
        });
    }
}
