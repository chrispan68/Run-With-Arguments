package Actions;

import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import javax.naming.NoPermissionException;
import javax.swing.*;

/**
 * This action-plugin runs a program with program parameters.
 * Its main goal is to facilitate intro level CS classes which either don't utilize command-line or haven't learned command-line yet.
 * The intuitive nature of this plugin integrates nicely with both right clicking the file and clicking on the COS 226 toolbar
 * Updates Made 6/28:
 *      The Program Arguments pop-up initially contains the previous run argument, (this is also saved on the run manager)
 *      The plugin was made more general to apply to files embedded in packages
 *      User friendly error messages were shown if the file click isn't a java file
 * Updates Made 6/29
 *      Before you needed to manually configure the plugin to where you needed it, now the configuration process is automated. (It appears when you right click a button)
 *      The button to "Run with arguments" only appears when selecting a .java file, eliminating confusion and possible exceptions
 *      The text of the button changed from simply "Run with arguments" to "Run 'filename.java' with arguments
 *      When you use this run command the RunManager has your class selected after executing the action
 * Updates Made 6/30
 *      Changed the title text of the pop-up to correspond to the displayed name of the action
 *      Changed the default edit configuration options to match what they would be if you ran the program from intellij's built in run action
 *      If you choose cancel or x out the pop-up it doesn't add the program to the run manager or run the program
 *      Changed naming configurations to be cohesive
 */
public class RunWithArguments extends AnAction {

    /**
     * This method returns whether a given file contains a main method
     *
     * @param jfile the file being analyzed
     * @return whether the java file contains a main method or not
     */
    public static boolean hasMainMethod(PsiJavaFile jfile){
        for(PsiClass cls : jfile.getClasses()){
            for(PsiMethod m : cls.getMethods()){ //Iterates through every method in every class of the java file
                String totalmod = ""; //This will eventually store all the modifiers concatenated to one another
                for(JvmModifier mod : m.getModifiers())totalmod += mod.toString(); //Appends all the modifiers to totalmod
                if(totalmod.equals("PUBLICSTATIC") && m.getReturnType() != null && m.getReturnType().equalsToText("void") && m.getName().equals("main")) return true; //Checks if all the modifiers are public and static, as well as that the return type is void, and that the method name is main
                //If a main method is found, then we know the class is runnable and return true
            }
        }
        return false; //If no main method was found return false
    }
    /**
     * This method is called by intellij multiple times per second to keep the plugin appearance updated
     * It controls whether the Run With Arguments button is showing or not, as well as what the displayed name of the button is
     *
     * @param e contains useful information about the nature of the action performed
     */
    @Override
    public void update(AnActionEvent e){
        PsiFile file = e.getData(PlatformDataKeys.PSI_FILE); //Extracts the current file from the context
        boolean show;
        if(file == null){
            e.getPresentation().setVisible(false); //If its not a file being selected then it automatically doesn't show
            return;
        }
        else {
            try {
                show = file.getFileType().getName().equals("JAVA"); //The menu bar only opens if you are selecting a .java file
            } catch(Exception exc){
                e.getPresentation().setVisible(false);
                return;
            }
            PsiJavaFile jfile;
            if(show){
                jfile = (PsiJavaFile) file; //If it is a java file then cast it into a jfile
                show = hasMainMethod(jfile); //We only show the menu option if the program contains a main method
            }
        }
        e.getPresentation().setVisible(show); //Sets the visibility of the button
        if(show)e.getPresentation().setText("Run \'" + file.getName().substring(0 , file.getName().lastIndexOf('.')) + "\' with Arguments"); //Sets the display as the name of the file
    }
    /**
     * This method is called every time the button provided on the IDE is clicked
     *
     * @param e contains useful information about the nature of the action performed
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project proj = e.getProject(); //Gets the project stored before any UI actions
        if(proj == null)return;
        String filename; //The name of the class that you are running. If the class is named "HelloWorld.java" filename will store "HelloWorld"
        Module mod; //The module that the class is in
        PsiJavaFile file; //The psijavafile representation of the class file being ran
        RunnerAndConfigurationSettings config; //the configuration settings for the run manager
        ApplicationConfiguration apconfig; //stores the application specific run settings
        try{
            file = (PsiJavaFile) e.getData(PlatformDataKeys.PSI_FILE); //Gets the java file from the Action Event details
        } catch(Exception exc){
            return;
        }
        if(file == null)return; //Kicks out if there is no file selected
        try {
            mod = LangDataKeys.MODULE.getData(e.getDataContext()); //Gets the module from the data context of the Action Event information
        } catch(Exception exc){
            return;
        }
        if(mod == null)return;
        filename = file.getPackageName(); //Initially sets the filename as the package name
        filename += filename.length() == 0 ? "" : "."; //Adds a period to the filename if the program is in a package
        try {
            filename += (file.getName().substring(0, file.getName().lastIndexOf('.'))); //Adds the actual program file name to the filename string. After all these lines the filename is correctly set to "package.class" or "class"
        } catch(Exception exc) {
            return;
        }
        RunManager rm = RunManager.getInstance(proj); //Extracts the run manager from the project
        String orig = ""; //This variable stores the original value displayed in the program argument,  initially it is equal to the empty string
        RunnerAndConfigurationSettings prev = null;
        for(RunnerAndConfigurationSettings rcs : rm.getConfigurationSettingsList(ApplicationConfigurationType.getInstance())){
            ApplicationConfiguration ac;
            try {
                ac = (ApplicationConfiguration) rcs.getConfiguration(); //FIX THIS
            } catch(Exception exc){
                continue; //If it's not an application configuration then continue
            }
            if(ac.getMainClassName() != null && ac.getMainClassName().equals(filename) && ac.getModules()[0].getName().equals(mod.getName())){ //Checks to see if this file had a previous configuration with program parameters
                orig = ac.getProgramParameters(); //Sets the original text to the previous program parameters
                prev = rcs; //Gets the previous configuration
            }
        }
        String a = (String) JOptionPane.showInputDialog(null, "Enter Program Arguments: ", "Run \'" + filename + "\' with Arguments", 3, null, null, orig);
        if (a == null)
            return; //If the user x's out or cancels we don't add the program to the run manager or run it
        if(prev == null) {
            apconfig = new ApplicationConfiguration(filename, proj , ApplicationConfigurationType.getInstance()); //Instantiates a new Application Configuration instance using the filename as the name
            config = rm.createConfiguration(apconfig , apconfig.getFactory());
            config.setName(filename); //Sets the name of the configuration as the file name
            apconfig = (ApplicationConfiguration) config.getConfiguration();
            apconfig.setMainClassName(filename); //Sets the filename of the application configuration
            apconfig.setWorkingDirectory(proj.getBaseDir().getPath()); //Sets the working directory to the directory of the project
            apconfig.setProgramParameters(a); //Prompts the user to enter program parameters
            apconfig.setModuleName(mod.getName()); //Sets the module name in the application configuration
            prev = rm.getConfigurationTemplate(apconfig.getFactory()); //Next few lines set the settings to the default settings
            config.setTemporary(prev.isTemporary());
            config.setActivateToolWindowBeforeRun(prev.isActivateToolWindowBeforeRun());
            config.setShared(prev.isShared());
            config.setSingleton(prev.isSingleton());
            config.setEditBeforeRun(false);
            rm.addConfiguration(config);
            rm.setSelectedConfiguration(config); //sets the configuration as the current selected configuration
            ProgramRunnerUtil.executeConfiguration(config, DefaultRunExecutor.getRunExecutorInstance()); //Runs the program
        }
        else{
            config = prev; //Uses the previous configuration settings
            apconfig = (ApplicationConfiguration) config.getConfiguration(); //Extracts the application specific settings out of the settings
            apconfig.setProgramParameters(a); //Prompts the user to enter program parameters
            rm.setSelectedConfiguration(config);
            ProgramRunnerUtil.executeConfiguration(config, DefaultRunExecutor.getRunExecutorInstance()); //Runs the program
        }
    }
}

/**
 * To Do:
 * 4) Settings xml file
 **/
