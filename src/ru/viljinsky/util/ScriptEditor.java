package ru.viljinsky.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import ru.viljinsky.forms.CommandListener;
import ru.viljinsky.forms.CommandMngr;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileFilter;
import ru.viljinsky.sqlite.DataModule;

/**
 *
 * @author вадик
 */


public class ScriptEditor extends JEditorPane implements CommandListener{
    CommandMngr commands = new CommandMngr();
    JFileChooser fc = new JFileChooser(new File("."));
    Document doc;
    
    public ScriptEditor(String type, String text) {
        super("text",text);
        fc.setFileFilter(new SqlFilter());
        doc = getDocument();
        setPreferredSize(new Dimension(500,400));
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }
            
            private void showPopup(MouseEvent e){
                if (e.isPopupTrigger()){
                    getPopupMenu().show(ScriptEditor.this, e.getX(),e.getY());
                }
            }
            
        });
        commands.setCommands(new String[]{"LOAD","EXECUTE"});
        commands.addCommandListener(this);
        
    }
    
    public JPopupMenu getPopupMenu(){
        JPopupMenu popupMenu = new JPopupMenu();
        for (Action a:commands.getActions()){
            popupMenu.add(a);
        }
        return popupMenu;
    }
    
    public Action[] getEditorActions(){
        return commands.getActions();
    }
    
    private void loadScript(File file) throws Exception{
        BufferedReader r = null;
        InputStreamReader istr = null ;
        try {
            istr = new InputStreamReader(new FileInputStream(file),"UTF-8");
            r = new BufferedReader(istr);
            String line;
            doc.remove(0, doc.getLength());
            while ((line=r.readLine())!=null){
                doc.insertString(doc.getLength(), line+"\n", null);
            }
        } finally {
            if (istr!=null) istr.close();
            if (r!=null) r.close();
            setCaretPosition(0);
        }
    }
    
    public boolean executeScript() throws Exception{
        Pattern p= Pattern.compile(";");
        Pattern p2 = Pattern.compile("--.*");
        String s = doc.getText(0, doc.getLength());
        String[] items = p.split(s);
        int n=0;
        String res="";
        try{
            for (String ss:items){
                Matcher m = p2.matcher(ss);
                res= m.replaceAll("");
                if (!res.trim().isEmpty()){
                    System.out.println(n+++"-> "+res.trim());
                    DataModule.execute(res.trim());
                }
            }
            DataModule.commit();
        } catch (Exception e){
            DataModule.rollback();
            throw new Exception("Ошибка при выполнении скрипта\n\""+res+"\"\n"+e.getMessage());
        }
        return true;
    }
    
    class SqlFilter extends FileFilter{

        @Override
        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            String fName = f.getName();
            return fName.endsWith(".sql");
        }

        @Override
        public String getDescription() {
            return "SQL скрит";
        }
    }
    
    @Override
    public void doCommand(String command) {
        
        System.out.println(command);
        try{
            switch (command){
                case "LOAD":
                    int retval = fc.showOpenDialog(null);
                    if (retval==JFileChooser.APPROVE_OPTION){
                        loadScript(fc.getSelectedFile());
                    }
                    break;
                case "EXECUTE":
                    executeScript();
                    JOptionPane.showMessageDialog(null, "Script успешно выполнен");
                    break;
            }
        } catch (Exception e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    @Override
    public void updateAction(Action action) {
    }
    
    public static void main(String[] args){
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout());
        String text="qwwerty;\n12345\n--67890;\nHello\nworld";
        panel.add(new JScrollPane(new ScriptEditor("text",text)));
        
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
    }

    
}
