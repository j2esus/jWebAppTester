/*
 * Web application tester- Utility to test web applications via Selenium 
 * Copyright (C) 2021-Nestor Arias
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package oa.com.tests.globals;

import oa.com.tests.Utils;
import oa.com.tests.actionrunners.exceptions.BadSyntaxException;
import oa.com.tests.actionrunners.exceptions.NoActionSupportedException;
import oa.com.tests.actionrunners.interfaces.AbstractDefaultScriptActionRunner;
import oa.com.tests.actions.TestAction;
import oa.com.tests.webapptester.MainApp;
import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import lombok.Data;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.safari.SafariDriver;
import oa.com.tests.actionrunners.interfaces.ScriptActionRunner;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oa.com.tests.actionrunners.exceptions.InvalidVarNameException;
import oa.com.tests.actionrunners.interfaces.PathKeeper;
import oa.com.tests.actionrunners.interfaces.VariableProvider;
import oa.com.tests.lang.Variable;
import oa.com.tests.lang.SelectorVariable;
import oa.com.utils.Encryption;
import org.openqa.selenium.Keys;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.ie.InternetExplorerDriverService;

/**
 * Gestor de acciones.
 *
 * @author nesto
 */
@Data
public final class ActionRunnerManager {

    public enum BROWSERTYPE {
        CHROME,
        EDGE,
        FIREFOX,
        INTERNET_EXPLORER,
        OPERA,
        SAFARI
    }

    public enum ACTIONTYPE {
        START,
        END
    }

    private static ResourceBundle globals = ResourceBundle.getBundle("application");

    /**
     * Listado con todas las clases de funciones registradas.
     */
    private static List<Class<? extends ScriptActionRunner>> runnersCls = null;

    static {
        try {
            runnersCls = findRunnersCls();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ActionRunnerManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ActionRunnerManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * Listado con todas las funciones registradas.
     */
    private TreeModel rootTree;
    private BROWSERTYPE browserType;
    private WebDriver driver;
    @SuppressWarnings("unchecked")
    private List<Variable> variables = new LinkedList();
    private static ActionRunnerManager instance = new ActionRunnerManager();
    final private static String sqOpen = Pattern.quote("[");
    final private static String sqClose = Pattern.quote("]");
    final private static String KEY_SEPARATOR = ",";

    private ActionRunnerManager() {
        //Clases
        try {
            //Carpetas
            rootTree = asTree();
            //Acciones
        } //TODO: Mandar estas excepciones a UI
        catch (Exception ex) {
            Logger.getLogger("Probador Web").log(Level.SEVERE, null, ex);
        }
    }

    public static void quit() {
        if (instance.driver != null) {
            instance.driver.quit();
        }
    }

    private AbstractDefaultScriptActionRunner findRunner(String actionCommand) throws BadSyntaxException {
        TestAction tester;
        tester = new TestAction(actionCommand);
        AbstractDefaultScriptActionRunner runner = null;
        for (Class runnerCls : ActionRunnerManager.runnersCls) {
            try {
                @SuppressWarnings("unchecked")
                final Constructor constructor = runnerCls.getConstructor(TestAction.class);
                runner = (AbstractDefaultScriptActionRunner) constructor.newInstance(tester);
                break;
            } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                //Runner incorrecto, va con otro
//                ex.printStackTrace();
            }
        }
        return runner;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (driver != null) {
            driver.quit();
        }
    }

    public static TreeModel getTreeModel() {
        try {
            instance.rootTree = asTree();
        } catch (IOException ex) {
            Logger.getLogger("Probador Web").log(Level.SEVERE, null, ex);
        }
        return instance.getRootTree();
    }

    /**
     * Establece el tipo de navegador a utilizar PRE: btype != null
     *
     * @param btype
     */
    public static void set(BROWSERTYPE btype) {
        final BROWSERTYPE instanceBrowserType = instance.getBrowserType();
        if (instanceBrowserType != null) {
            if (instanceBrowserType == btype) {
                return;
            }
        }
        instance.setBrowserType(btype);
        final WebDriver instance_driver = instance.getDriver();
        if (instance_driver != null) {
            instance_driver.quit();
        }
        WebDriver driver = null;
        try {
            switch (btype) {
                case CHROME:
                    driver = new ChromeDriver();
                    break;
                case EDGE:
                    driver = new EdgeDriver();
                    break;
                case FIREFOX:
                    driver = new FirefoxDriver();
                    break;
                case INTERNET_EXPLORER:
                    driver = new InternetExplorerDriver();
                    break;
                case OPERA:
                    driver = new OperaDriver();
                    break;
                case SAFARI:
                default:
                    driver = new SafariDriver();
                    break;
            }
        } catch (Exception e) {
            String message = globals.getString("settings.driver.createException")
                    .replace("{0}", btype.name());
            e.printStackTrace();
//            JOptionPane.showMessageDialog(null, message,
//                    globals.getString("globals.error.title"),
//                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        instance.setDriver(driver);
    }

    /**
     * Ejecuta una opcion del arbol
     *
     * @param item
     */
    public static void exec(TreePath item, Logger log)
            throws InvalidVarNameException, FileNotFoundException, IOException {
        File file = Utils.getFile(item);
        final String ERR_TITLE = globals.getString("globals.error.title");
        if (file.isDirectory()) {
            JOptionPane.showMessageDialog(MainApp.getInstance(), globals.getString("exec.err.isFolder"), ERR_TITLE, JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Exception> exceptions = new LinkedList<>();
        for (File f : Utils.getRunnableFiles(item)) {
            exceptions.addAll(instance.exec(f, log));
        }
        //Errores de la ejecucion
        if (!exceptions.isEmpty()) {
            MainApp.addException(exceptions.toArray(new Exception[]{}));
        }
    }

    /**
     * Busca en un directorio y retorna un arbol dependiendo de lo que encuentre
     * alli.
     *
     * @param folderName
     * @return
     */
    private static TreeModel asTree() throws IOException {
        ResourceBundle globals = ResourceBundle.getBundle("application");
        final String FOLDER_NAME = globals.getString("globals.actionfolder.name");
        File rootFile = new File(FOLDER_NAME);
        if (!rootFile.exists()) {
            final String message = globals.getString("globals.folder.read.errNoFolder");
            final String title = globals.getString("globals.error.title");
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
            Utils.buildDefaultActionRunnerFolder(FOLDER_NAME);
            rootFile = new File(FOLDER_NAME);
        }
        final TreeModel model = new DefaultTreeModel(fileAsTree(rootFile));
        return model;
    }

    private static MutableTreeNode fileAsTree(File file) {
        File[] children = file.listFiles((pfile, ext)
                -> pfile.isDirectory() || ext.toLowerCase().trim().endsWith(".txt")
        );
        if (file.isDirectory() && children.length == 0) {
            return null;
        }
        if (children != null) {
            Arrays.sort(children, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        }
        final DefaultMutableTreeNode resp = new DefaultMutableTreeNode(file.getName());
        final String[] keyNamesArr = new String[]{"_end.txt", "_start.txt"};
        try {
            for (MutableTreeNode node : Arrays.asList(children)
                    .stream()
                    //Se aceptan directorios o archivos diferentes a _start.txt,_end.txt
                    .filter(f -> f.isDirectory()
                    || Arrays.binarySearch(keyNamesArr, f.getName()) < 0)
                    .map(f -> fileAsTree(f))
                    .filter(n -> n != null)
                    .collect(toList())) {
                resp.add(node);
            }
        } catch (NullPointerException npe) {
            ;
        }
        return resp;
    }

    private static List<Class<? extends ScriptActionRunner>> findRunnersCls() throws ClassNotFoundException, IOException {
        final Class<? extends ScriptActionRunner>[] classes = Utils.getClasses("oa.com.tests.scriptactionrunners", ScriptActionRunner.class);
        List<Class<? extends ScriptActionRunner>> resp
                = Arrays.asList(classes);
        return resp;
    }

    /**
     * Ejecuta las instrucciones en un archivo.
     *
     * @param file
     * @param log
     */
    public List<Exception> exec(File file, Logger log)
            throws InvalidVarNameException, FileNotFoundException, IOException {
//        final List<String> filteredLines = Files.lines(FileSystems.getDefault().getPath(file.getAbsolutePath()))
//                .map(String::trim)
//                .filter(l -> !l.isEmpty() && !l.startsWith("#"))
//                .collect(toList());
        List<Exception> resp = new LinkedList<>();
        final List<String> filteredLines = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            filteredLines.add(line);
        };
        reader.close();

        List<String> command = new LinkedList<>();
        int lineCounter = 0;
//        try {
        while (lineCounter < filteredLines.size()) {
            boolean correct = true;
            command.add(filteredLines.get(lineCounter++));
            String actionCommand = command.stream().collect(joining(" "));
            actionCommand = parse(actionCommand);
            correct = !command.isEmpty();
            if (correct) {
                AbstractDefaultScriptActionRunner runner;
                try {
                    runner = findRunner(actionCommand);
                } catch (BadSyntaxException ex) {
//                        ex.printStackTrace();
                    continue;
                }
                if (runner == null) {
                    String message = globals.getString("exec.err.noSuchRunnerException")
                            .replace("{0}", actionCommand)
                            .replace("{1}", file.getAbsolutePath());
                    resp.add(new NoActionSupportedException(message));
//                    JOptionPane.showMessageDialog(null, message,
//                            globals.getString("globals.error.title"), JOptionPane.ERROR_MESSAGE);
                    log.severe(message);
                    command.clear();
                    continue;
                }

                try {
                    runner.run(instance.getDriver(), log);
                    if (runner instanceof VariableProvider) {
                        VariableProvider varprovider = (VariableProvider) runner;
                        Variable variable = varprovider.getVariable();
                        if (variables.contains(variable)) {
                            variables.remove(variable);
                        }
                        variables.add(variable);
                    }
                } catch (Exception ex) {
                    BadSyntaxException badSyntaxException = new BadSyntaxException(prepareBadSystaxExMsg(actionCommand, file));
                    log.log(Level.SEVERE, actionCommand, ex);
                    resp.add(badSyntaxException);
                } finally {
                    command.clear();
                }
            }
        }
        if (!command.isEmpty()) {
            throwBadSynstaxEx(command.stream().collect(joining(" ")), file, log);
        }
//        } finally {
//            variables.clear();
//        }
        return resp;
    }

    private static void throwBadSynstaxEx(final String actionCommand, File file, Logger log) throws HeadlessException {
        String badSyntazMsg = prepareBadSystaxExMsg(actionCommand, file);
//        JOptionPane.showMessageDialog(null, badSyntazMsg,globals.getString("globals.error.title"), JOptionPane.ERROR_MESSAGE);
        log.severe(badSyntazMsg);
    }

    public static String prepareBadSystaxExMsg(final String actionCommand, File file) {
        String badSyntazMsg = globals.getString("exec.err.syntaxException")
                .replace("{0}", actionCommand)
                .replace("{1}", file.getAbsolutePath());
        return badSyntazMsg;
    }

    /**
     * @param actionCommand
     * @throws InvalidVarNameException
     * @return
     */
    public static String parse(String actionCommand) throws InvalidVarNameException {
        String resp = actionCommand;
        //Variables [:variable]
        resp = parseVariables(resp);
        resp = resp.replace("[::", "�{:");
        //Teclas especiales.[%keys]
        resp = parseKeys(resp);
        resp = resp.replace("[%%", "�{%");
        //Contrase�as [$PWD]
        resp = parsePWDs(resp);
        resp = resp.replace("[$$", "[$");
        return resp;
    }

    public static String parsePWDs(String resp) {
        String regexp = "([0-9|a-z|A-Z|\\s]*)"
                + "\\[\\$(.*)\\]"
                + "([0-9|a-z|A-Z|\\s]*)";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(resp);
        if (!matcher.find()) {
            return resp;
        }

        for (int i = 1; i <= matcher.groupCount(); i++) {
            String firstGroup = matcher.group(i++);
            String original = matcher.group(i++);
            String lastGroup = matcher.group(i);
            if (original.startsWith("$")) {
                continue;
            }
            String decrypted = firstGroup + Encryption.decrypt(original) + lastGroup;
            resp = resp.replace(firstGroup + "[$" + original + "]" + lastGroup, decrypted);
        }
        return resp;
    }

    /**
     * Helper for {@link #parse(java.lang.String)}
     *
     * @param resp
     * @return
     */
    public static String parseKeys(String resp) {
        String regexp = "([0-9|a-z|A-Z|\\s]*)("
                + sqOpen + "%[Keys\\.[0-9|a-z|A-Z]*\\" + (KEY_SEPARATOR) + "?]*" + sqClose
                + ")([0-9|a-z|A-Z|\\s]*)";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(resp);
        if (!matcher.find()) {
            return resp;
        }

        for (int i = 1; i <= matcher.groupCount(); i++) {
            String firstGroup = matcher.group(i++);
            String original = matcher.group(i++);
            String lastGroup = matcher.group(i);
            String varDef = original.substring(0, original.length() - 1)
                    .substring(2);
            String[] varDefs = varDef.contains(KEY_SEPARATOR)
                    ? varDef.split(KEY_SEPARATOR)
                    : new String[]{varDef};
            final CharSequence varAsKeys = firstGroup
                    + Arrays.asList(varDefs)
                            .stream()
                            .map(token -> Keys.valueOf(token.replace("Keys.", "")))
                            .collect(joining())
                    + lastGroup;
            String command = Keys.chord(varAsKeys);
            resp = resp.replace(firstGroup + original + lastGroup, command);
        }
        return resp;
    }

    /**
     * Determina si un texto dado contiene comidies que incluyen teclas
     * especiales como CONTROL, ESCAPE, F1,etc
     *
     * @param text
     * @return
     */
    public static Boolean hasKeys(String text) {
        String regexp = "([0-9|a-z|A-Z|\\s]*)("
                + sqOpen + "%[Keys\\.[0-9|a-z|A-Z]*\\" + (KEY_SEPARATOR) + "?]*" + sqClose
                + ")([0-9|a-z|A-Z|\\s]*)";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    /**
     * Helper for {@link #parse(java.lang.String) }
     *
     * @param actionCommand
     * @return
     * @throws InvalidVarNameException
     */
    private static String parseVariables(String actionCommand)
            throws InvalidVarNameException {
        String regexp = "(" + sqOpen + ":[0-9|a-z|A-Z]*" + sqClose + ")";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(actionCommand);

        if (!matcher.find()) {
            return actionCommand;
        }

        String resp = actionCommand;

        for (int i = 1; i <= matcher.groupCount(); i++) {
            String varDef = matcher.group(i);
            resp = resp.replace(varDef, resolveSelector4VarDef(varDef));
        }
        return resp;
    }

    /**
     * Retorna el selector para una variable que est� en un texto con su nombre.
     *
     * @param varDef El texto con el nombre de la variable en el formato
     * [:NOMBRE_VARIABLE]
     * @return
     */
    public static String resolveSelector4VarDef(String varDef) throws InvalidVarNameException {
        final String varName = varDef.substring(0, varDef.length() - 1)
                .substring(2);
        if (varName.isEmpty()) {
            throw new InvalidVarNameException("variable with name " + varName);
        }
        Optional<Variable> varMatch = instance.variables.stream()
                .filter(var -> var.getName().equals(varName))
                .findAny();
        if (varMatch.isEmpty()) {
            throw new InvalidVarNameException(varName);
        }
        Variable var = varMatch.get();
        if (var instanceof SelectorVariable) {
            return resolveSelectorHelper(((SelectorVariable) var).getFinder());
        }
        return var.getValue().toString();
    }

    /**
     * Retorna una representaci�n de los siguientes valores de un objeto
     * PathKeeper:
     * {@link oa.com.tests.actionrunners.interfaces.PathKeeper#getPath()  path}
     * +","type:"+
     * {@link oa.com.tests.actionrunners.interfaces.PathKeeper#getType() type.lowercase}
     *
     * @param finder
     * @return
     */
    private static String resolveSelectorHelper(PathKeeper finder) {
        ResourceBundle bundle = ResourceBundle.getBundle("application");
        String key = "CssSelectorActionRunner.attr.type";
        String typeKey = bundle.getString(key);
        return finder.getPath() + "\",\"" + typeKey + "\":\"" + finder.getType().name().toLowerCase();
    }

    /**
     * Para pruebas
     *
     * @param file
     * @param log
     */
    public static List<Exception> execInstance(File file, Logger log) throws InvalidVarNameException, IOException {
        return instance.exec(file, log);
    }

    /**
     * Needed to test full funcionality.
     *
     * @return
     */
    public static WebDriver getStDriver() {
        return instance.getDriver();
    }
}
